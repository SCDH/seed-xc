package de.ulbms.scdh.seed.xc.dts.document;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.dts.endpoints.DocumentApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Inject
	ResourceProvider resourceProvider;

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
		if (tree != null) map.put("tree", tree);
		if (ref != null) {
			map.put("ref", ref);
		} else if (start != null) map.put("start", start);
		if (end != null) map.put("end", end);
		params.globalParameters(map);

		// Create ResourceInContext from resource parameter and additional parameters
		ResourceInContext ric = new ResourceInContext("", resource);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug(resourceProvider::getResource).onItem().transform((s) -> {
			try {
				return transformation.transform(params, null, resource, s);
			} catch (TransformationPreparationException | TransformationException e) {
				LOG.error(e.getMessage());
				throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
			}
		});
	}
}
