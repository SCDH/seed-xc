package de.ulbms.scdh.seed.xc.dts.navigation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import de.ulbms.scdh.seed.xc.dts.endpoints.NavigationApi;
import de.ulbms.scdh.seed.xc.dts.model.Navigation;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the DTS {@link NavigationAPI} endpoint uses
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
	private String TRANSFORMATION;

	@Inject
	TransformationMap transformations;

	@Inject
	ResourceProvider resourceProvider;

	@Override
	public Uni<Navigation> navigation(
			String resource,
			String ref,
			String start,
			String end,
			Integer down,
			String tree,
			Integer page,
			Map<String, String> cr,
			Map<String, String> cf) {

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, String> map = new HashMap<String, String>();
		if (resource != null) map.put("resource", resource);
		if (down != null) map.put("down", down.toString());
		if (tree != null) map.put("tree", tree);
		if (page != null) map.put("page", page.toString());
		if (ref != null) map.put("ref", ref);
		if (start != null) map.put("start", start);
		if (end != null) map.put("end", end);
		// all cf (= Context Follow ups) parameters are passed to the stylesheet
		if (cf != null) map.putAll(cf);
		params.globalParameters(map);

		// get the transformation or return failure
		Transformation transformation = transformations.get(TRANSFORMATION);
		if (transformation == null) {
			LOG.error("transformation not available: {}", TRANSFORMATION);
			return Uni.createFrom()
					.failure(new jakarta.ws.rs.BadRequestException("transformation not available: " + TRANSFORMATION));
		}

		// Create ResourceInContext from resource parameter and additional parameters
		ResourceInContext ric = new ResourceInContext("", resource);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug(resourceProvider::getResource)
				.onItem()
				.transform((s) -> {
					try {
						return transformation.transform(params, null, resource, s);
					} catch (TransformationPreparationException e) {
						LOG.error(e.getMessage());
						throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
					} catch (TransformationException e) {
						LOG.error(e.getMessage());
						throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
					}
				})
				.onItem()
				.transform((bs) -> {
					// TODO: Can we get rid of this serialization ○ deserialization
					// step? We could simple send the bytestream back to the
					// client, but that would break the signature of the interface
					// generated from OpenAPI specs. This extra step seems to be the
					// cost of using OpenAPI specs.
					try {
						ObjectMapper om = new ObjectMapper(new JsonFactory());
						return om.readValue(bs, Navigation.class);
					} catch (DatabindException e) {
						LOG.error(e.getMessage());
						throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
					} catch (StreamReadException e) {
						LOG.error(e.getMessage());
						throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
					} catch (IOException e) {
						LOG.error(e.getMessage());
						throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage());
					}
				});
	}
}
