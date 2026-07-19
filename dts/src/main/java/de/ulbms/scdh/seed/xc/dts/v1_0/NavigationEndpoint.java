package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.CollectionConfiguration;
import de.ulbms.scdh.seed.xc.dts.CollectionMetadataProcessor;
import de.ulbms.scdh.seed.xc.dts.URITemplateBuilder;
import de.ulbms.scdh.seed.xc.dts.endpoints.NavigationApi;
import de.ulbms.scdh.seed.xc.dts.model.Navigation;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the DTS {@link NavigationApi} endpoint uses
 * a {@link ResourceProvider} plugin to get the resource from a
 * resource storage. Then, it transforms it to a {@link Navigation}
 * object using a configured and pre-compiled transformation. These
 * steps are performed in non-blocking IO using the Mutiny async
 * framework.
 */
@RequestScoped
public class NavigationEndpoint implements NavigationApi {

	private static final Logger LOG = LoggerFactory.getLogger(NavigationEndpoint.class);

	/**
	 * The ID of the transformation using for transforming a resource.
	 */
	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.dts.NavigationEndpoint.TRANSFORMATION",
			defaultValue = "dts-transformations-xsl-navigation")
	protected String TRANSFORMATION;

	/**
	 * Location of the collection metadata, same as for Collection endpoint.
	 */
	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.GRAPH", defaultValue = "collection.json")
	protected String GRAPH;

	@Inject
	protected CollectionMetadataProcessor collectionMetadataProc;

	@Inject
	protected CollectionConfiguration collectionConfiguration;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	protected ResourceProviderManager resourceProviderManager;

	@Inject
	protected HttpServerRequest request;

	@Inject
	protected URITemplateBuilder uriTemplateBuilder;

	/**
	 * <P>Implementation of the DTS Navigation endpoint.</P>
	 * <P>This first gets the resource using the resource provider and then transformes it.</P>
	 *
	 * @param provider - the type of resource provider
	 * @param location - the base location accessed by the resource provider
	 * @param resource - Resource identifier. Passed as runtime parameter to the transformation and also to the resource provider.
	 * @param ref - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param start - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param end - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param down - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param tree - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param page - See DTS specs. Passed as runtime parameter to the transformation.
	 * @return The document or parts of it in the requested media type.
	 */
	@Override
	public Uni<byte[]> navigation(
			URI resource,
			URI provider,
			URI location,
			String ref,
			String start,
			String end,
			Integer down,
			String tree,
			Integer page) {

		if (resource == null || resource.toString().isEmpty())
			throw new BadRequestException("resource parameter is required");

		Config transformationConfig = new Config();
		transformationConfig.base(request.absoluteURI());

		URI thisIri;
		try {
			URI rqUrl = new URI(request.absoluteURI());
			// the IRI of the resource is the current request, but query part and fragment cut off
			thisIri = new URI(
					rqUrl.getScheme(),
					rqUrl.getRawUserInfo(),
					rqUrl.getHost(),
					rqUrl.getPort(),
					rqUrl.getPath(),
					null,
					null);
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make Base URI");
		}
		LOG.debug("getting metadata for {}", thisIri);

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		map.put("resource", pvOf(request.absoluteURI())); // @id has URL with any parameters
		if (down != null) map.put("down", pvOf(down.toString()));
		if (tree != null) map.put("tree", pvOf(tree));
		if (page != null) map.put("page", pvOf(page.toString()));
		if (ref != null) map.put("ref", pvOf(ref));
		if (start != null) map.put("start", pvOf(start));
		if (end != null) map.put("end", pvOf(end));
		// parameters for URI templates
		try {
			URI requestUri = new URI(request.absoluteURI());
			map.put("collection-uri-template", pvOf(uriTemplateBuilder.resourceTemplate(requestUri, "collection")));
			map.put("navigation-uri-template", pvOf(uriTemplateBuilder.resourceTemplate(requestUri, "navigation")));
			map.put("document-uri-template", pvOf(uriTemplateBuilder.resourceTemplate(requestUri, "document")));
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException(e.getMessage());
		}
		// make global parameters from the map
		params.globalParameters(map);

		// get the transformation or return failure
		Transformation transformation = transformations.get(TRANSFORMATION);
		if (transformation == null) {
			LOG.error("transformation not available: {}", TRANSFORMATION);
			return Uni.createFrom()
					.failure(new jakarta.ws.rs.BadRequestException("transformation not available: " + TRANSFORMATION));
		}

		ResourceProvider resourceProvider;
		try {
			ResourceProviderBuilder resourceProviderBuilder = resourceProviderManager.get(provider.toString());
			resourceProvider = resourceProviderBuilder.withBase(location);
		} catch (ResourceProviderConfigurationException e) {
			LOG.error("cannot find resource provider builder type {}", provider);
			throw new BadRequestException("unknown resource provider: " + provider);
		} catch (ResourceNotFoundException e) {
			LOG.error("not found: {}", location);
			throw new NotFoundException("not found");
		} catch (ResourceException e) {
			LOG.error("cannot open base location {} with {} resource provider: {}", location, provider, e.getMessage());
			throw new BadRequestException("cannot open base location: " + e.getMessage());
		}

		// async processing of
		// 1. get collection.json, 2. lookup the resource's location, 3. get the resource, 4. transform it
		Map<String, String> crContext = Map.of();
		ResourceInContext ric = new ResourceInContext(crContext, GRAPH);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug((r) -> resourceProvider.asyncOpenStream(r, request))
				.onItem()
				.transform(s -> {
					try {
						return s.readAllBytes();
					} catch (IOException e) {
						throw new InternalServerErrorException(e.getMessage());
					}
				})
				.onItem()
				.transform(bytes -> {
					Config config = collectionConfiguration.merge(bytes, transformationConfig, "navigation");
					return Tuple2.of(bytes, config);
				})
				.onItem()
				.transform(t -> t.mapItem1(ByteArrayInputStream::new))
				.onItem()
				.transformToUni(t -> Uni.createFrom()
						.item(t.getItem1())
						.plug(s -> collectionMetadataProc.getResource(
								resourceProvider, s, GRAPH, t.getItem2(), crContext, thisIri))
						.onItem()
						.transform(s -> Tuple2.of(s, t.getItem2())))
				.onItem()
				.transformToUni((t) -> transformation.transformAsync(
						collectionConfiguration.appendToParameters(params, t.getItem2()),
						t.getItem2(),
						resource.toString(),
						Uni.createFrom().item(t.getItem1()),
						resourceProvider,
						request));
	}
}
