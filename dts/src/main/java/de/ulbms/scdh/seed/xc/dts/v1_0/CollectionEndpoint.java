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
import java.util.Collections;
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

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.GRAPH", defaultValue = "collection.n3")
	protected String GRAPH;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.CR_GRAPH_KEY", defaultValue = "graph")
	protected String CR_GRAPH_KEY;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TYPE", defaultValue = "DtsDocumentProcessor")
	protected String MEDIA_TYPES_TRANSFORMATIONS;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProvider resourceProvider;

	@Inject
	HttpServerRequest request;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<byte[]> collection(String id, String nav, Integer page, Map<String, String> cr, Map<String, String> cf) {

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		if (id != null) map.put("id", pvOf(id));
		if (nav != null) map.put("nav", pvOf(nav));
		if (page != null) map.put("page", pvOf(page.toString()));
		if (cf != null) for (String k : cf.keySet()) map.put(k, pvOf(cf));
		// set mediaTypes from available transformations
		List<String> mediaTypes = transformations.getByType(MEDIA_TYPES_TRANSFORMATIONS).stream()
				.map(Transformation::getOutputMediaType)
				.toList();
		LOG.debug("setting mediaTypes to {}", mediaTypes);
		// map.put("mediaTypes", pvOf(mediaTypes));
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

		// determine collection metadata graph file and make resource in context from it
		if (cr == null) cr = Map.of();
		String graph;
		if (cr.containsKey(CR_GRAPH_KEY)) {
			graph = cr.get(CR_GRAPH_KEY);
		} else {
			graph = GRAPH;
		}
		ResourceInContext ric = new ResourceInContext(Collections.unmodifiableMap(cr), graph);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug((r) -> resourceProvider.asyncOpenStream(r, request))
				.plug((s) -> transformation.transformAsync(params, null, graph, s, resourceProvider, request));
	}
}
