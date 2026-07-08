package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.endpoints.CollectionApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the collection endpoints returns metadata by per
 * default running a SPARQL query on a graph given in a single JSON-LD file.
 * This file shauld simply contain all collection and resource datasets with
 * members given as IDs. The SPARQL query will then look up the members by ID
 * and add de-reference them as required by the endpoint specs.
 */
@RequestScoped
public class CollectionEndpoint implements CollectionApi {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionEndpoint.class);

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.CHILDREN_TRANSFORMATION",
			defaultValue = "dts-transformations-rq-children")
	protected String CHILDREN_TRANSFORMATION;

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.PARENTS_TRANSFORMATION",
			defaultValue = "dts-transformations-rq-parents")
	protected String PARENTS_TRANSFORMATION;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.GRAPH", defaultValue = "collection.json")
	protected String GRAPH;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TYPE", defaultValue = "DtsDocumentProcessor")
	protected String MEDIA_TYPES_TRANSFORMATIONS;

	@ConfigProperty(name = "dts-default-collection", defaultValue = "general")
	protected URI defaultCollection;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProviderManager resourceProviderManager;

	@Inject
	HttpServerRequest request;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<byte[]> collectionDefault(URI provider, URI location) {
		URI thisIri;
		try {
			URI rqUrl = new URI(request.absoluteURI());
			// the IRI of the collection/resource is the current request, but query part and fragment cut off
			thisIri = new URI(
					rqUrl.getScheme(),
					rqUrl.getRawUserInfo(),
					rqUrl.getHost(),
					rqUrl.getPort(),
					rqUrl.getPath() + "/" + defaultCollection,
					null,
					null);
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make Base URI");
		}
		LOG.info("getting metadata for default collection {}", thisIri);
		return collection(provider, location, thisIri.toString(), thisIri, defaultCollection, null, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<byte[]> collection(URI provider, URI location, URI id, String nav, Integer page) {

		URI thisIri;
		try {
			URI rqUrl = new URI(request.absoluteURI());
			// the IRI of the collection/resource is the current request, but query part and fragment cut off
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
		LOG.info("getting metadata for {}", thisIri);

		return collection(provider, location, request.absoluteURI(), thisIri, id, nav, page);
	}

	protected Uni<byte[]> collection(
			URI provider, URI location, String baseIri, URI iri, URI id, String nav, Integer page) {
		Config transformationsConfig = new Config();
		transformationsConfig.empty404(true);

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		map.put("requested", pvOf(iri.toString())); // most important parameter!
		if (id != null) map.put("idP", pvOf(id.toString()));
		if (nav != null) map.put("navP", pvOf(nav));
		if (page != null) map.put("pageP", pvOf(page.toString()));
		// set mediaTypes from available transformations
		List<String> mediaTypes = transformations.getByType(MEDIA_TYPES_TRANSFORMATIONS).stream()
				.map(Transformation::getOutputMediaType)
				.toList();
		LOG.debug("setting mediaTypes to {}", mediaTypes);
		map.put("mediaTypesP", pvOf(mediaTypes));
		// set endpoint specific parameters
		map.put("base", pvOf(iri));
		// transformationsConfig.base(base.toString());
		transformationsConfig.base(baseIri);
		params.setGlobalParameters(map);

		Transformation transformation;
		if (nav == null || nav.equals("children")) {
			if (transformations.containsKey(CHILDREN_TRANSFORMATION)) {
				transformation = transformations.get(CHILDREN_TRANSFORMATION);
			} else {
				LOG.error("transformation for nav=children not available: {}", CHILDREN_TRANSFORMATION);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"transformation not available: " + CHILDREN_TRANSFORMATION));
			}
		} else {
			if (transformations.containsKey(PARENTS_TRANSFORMATION)) {
				transformation = transformations.get(PARENTS_TRANSFORMATION);
			} else {
				LOG.error("transformation for nav=parents not available: {}", PARENTS_TRANSFORMATION);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"transformation not available: " + PARENTS_TRANSFORMATION));
			}
		}

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

		ResourceInContext ric = new ResourceInContext(Map.of(), GRAPH);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug((r) -> resourceProvider.asyncOpenStream(r, request))
				.plug((s) -> transformation.transformAsync(
						params, transformationsConfig, GRAPH, s, resourceProvider, request));
	}
}
