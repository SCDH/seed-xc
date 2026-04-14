package de.ulbms.scdh.seed.xc.dts.document;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.endpoints.DocumentApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Collections;
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

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProvider resourceProvider;

	/**
	 * Implementation of the DTS Document endpoint. This first gets the resource using the resource provider and then transformes it.
	 *
	 * @param resource - Resource identifer. Passed as runtime parameter to the transformation and also to the resource provider.
	 * @param ref - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param start - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param end - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param tree - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param mediaType - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param cr - Context information for getting the resource as {@link Map<String,String>}. This hash map is passed to the resource provider.
	 * @param cf - Context information for follow-up links as {@link Map<String,String>}. These are passed as runtime parameters to the transformation.
	 * @return The document or parts of it in the requested media type.
	 */
	@Override
	public Uni<byte[]> document(
			String resource,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			Map<String, String> cr,
			Map<String, String> cf) {

		// get the transformation or return failure
		Transformation transformation = transformations.get(TRANSFORMATION);
		if (transformation == null) {
			LOG.error("transformation not available: {}", TRANSFORMATION);
			return Uni.createFrom()
					.failure(new jakarta.ws.rs.BadRequestException("transformation not available: " + TRANSFORMATION));
		}

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, String> map = new HashMap<String, String>();
		if (resource != null) map.put("resource", resource);
		if (ref != null) map.put("ref", ref);
		if (start != null) map.put("start", start);
		if (end != null) map.put("end", end);
		if (tree != null) map.put("tree", tree);
		/* TODO: media type */
		// all cf (= Context Follow ups) parameters are passed to the stylesheet
		if (cf != null) map.putAll(cf);
		params.globalParameters(map);

		// Create ResourceInContext from resource parameter and additional parameters
		if (cr == null) cr = Map.of();
		ResourceInContext ric = new ResourceInContext(Collections.unmodifiableMap(cr), resource);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug(resourceProvider::getResource).onItem().transform((s) -> {
			return transformation.transformF(params, null, resource, s, resourceProvider);
		});
	}
}
