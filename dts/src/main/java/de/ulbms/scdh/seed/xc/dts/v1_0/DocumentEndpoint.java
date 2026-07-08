package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.CollectionMetadataProcessor;
import de.ulbms.scdh.seed.xc.dts.endpoints.DocumentApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the DTS Document endpoint uses a configurable
 * {@link ResourceProvider} bean for getting the resource from a
 * persistence service. It uses the compiled transformations to process
 * the resource according to the DTS specification.
 */
@RequestScoped
public class DocumentEndpoint implements DocumentApi {

	private static final Logger LOG = LoggerFactory.getLogger(DocumentEndpoint.class);

	/**
	 * The ID of the transformation using for transforming a resource.
	 */
	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TRANSFORMATION",
			defaultValue = "dts-transformations-xsl-document")
	protected String TRANSFORMATION;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TYPE", defaultValue = "DtsDocumentProcessor")
	protected String TYPE;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.SETS_SERIALIZER", defaultValue = "true")
	protected boolean SETS_SERIALIZER;

	/**
	 * Location of the collection metadata, same as for Collection endpoint.
	 */
	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.GRAPH", defaultValue = "collection.json")
	protected String GRAPH;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.NavigationEndpoint.RESOURCE_ID_PATH", defaultValue = "false")
	protected boolean RESOURCE_IS_PATH;

	@Inject
	CollectionMetadataProcessor collectionMetadataProc;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProviderManager resourceProviderManager;

	@Inject
	HttpServerRequest request;

	/**
	 * Implementation of the DTS Document endpoint. This first gets the resource using the resource provider and then transforms it.
	 *
	 * @param provider - the type of resource provider
	 * @param location - the base location accessed by the resource provider
	 * @param resource - Resource identifier. Passed as runtime parameter to the transformation and also to the resource provider.
	 * @param ref - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param start - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param end - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param tree - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param mediaType - See DTS specs. Passed as runtime parameter to the transformation.
	 * @return The document or parts of it in the requested media type.
	 */
	@Override
	public Uni<byte[]> document(
			URI resource,
			URI provider,
			URI location,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType) {

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

		Transformation transformation = null;
		if (mediaType == null) {
			// get the default transformation or return failure
			transformation = transformations.get(TRANSFORMATION);
			if (transformation == null) {
				LOG.error("transformation not available: {}", TRANSFORMATION);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"transformation not available: " + TRANSFORMATION));
			}
		} else {
			// try to get a transformation for the requested media type
			LOG.info("searching for document transformation to media type {}", mediaType);
			boolean found = false;
			for (String transformationId : transformations.keySet()) {
				transformation = transformations.get(transformationId);
				LOG.info(
						"testing transformation {}, with type {}: {}",
						transformationId,
						transformation.getType(),
						transformation.getOutputMediaType());
				if (transformation.getOutputMediaType() != null
						&& transformation.getOutputMediaType().equals(mediaType)
						&& transformation.getType() != null
						&& Arrays.asList(transformation.getType()).contains(TYPE)) {
					found = true;
					if (SETS_SERIALIZER) {
						// we have to set the serializer because the called stylesheet is always document.xsl which has
						// output method XML.
						Serializer serializer = new Serializer();
						serializer.setMethod(mediaType);
						transformationConfig.setSerializer(serializer);
					}
					break;
				}
			}
			if (!found) {
				LOG.error("DTS document transformation to media type not available: {}", mediaType);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"DTS document transformation to requested media type not available: " + mediaType));
			}
		}
		final Transformation finalTransformation = transformation; // final required for the lambda expression below
		final Config finalConfig = transformationConfig;

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		if (mediaType != null) map.put("mediaType", pvOf(mediaType));
		map.put("resource", pvOf(resource));
		if (ref != null) map.put("ref", pvOf(ref));
		if (start != null) map.put("start", pvOf(start));
		if (end != null) map.put("end", pvOf(end));
		if (tree != null) map.put("tree", pvOf(tree));
		params.globalParameters(map);
		LOG.info("parameters: {}", map);

		ResourceProvider resourceProvider;
		try {
			ResourceProviderBuilder resourceProviderBuilder = resourceProviderManager.get(provider.toString());
			resourceProvider = resourceProviderBuilder.withBase(location);
		} catch (ResourceProviderConfigurationException e) {
			LOG.error("cannot find resource provider builder type {}", provider);
			throw new BadRequestException("unknown resource provider: " + provider);
		} catch (ResourceException e) {
			LOG.error("cannot open base location {} with {} resource provider: {}", location, provider, e.getMessage());
			throw new BadRequestException("cannot open base location: " + e.getMessage());
		}

		// Create ResourceInContext from resource parameter and additional parameters
		Map<String, String> crContext = Map.of();
		Uni<ResourceInContext> uniRic;
		if (RESOURCE_IS_PATH) {
			ResourceInContext ric = new ResourceInContext(crContext, resource.toString());
			uniRic = Uni.createFrom().item(ric);
		} else {
			// get the resource location from the collection metadata
			ResourceInContext collectionIc = new ResourceInContext(crContext, GRAPH);
			uniRic = Uni.createFrom()
					.item(collectionIc)
					.plug((cic) -> {
						return resourceProvider.asyncOpenStream(cic, request);
					})
					.plug((s) -> {
						return collectionMetadataProc.getResourceLocation(
								s, GRAPH, transformationConfig, crContext, thisIri.toString());
					})
					.onItem()
					.transform(resourceLocation -> {
						return new ResourceInContext(crContext, resourceLocation);
					});
		}

		return uniRic.plug((r) -> resourceProvider.asyncOpenStream(r, request))
				.plug((s) -> finalTransformation.transformAsync(
						// TODO: systemId from collectionMetadataProc
						params, finalConfig, resource.toString(), s, resourceProvider, request));
	}
}
