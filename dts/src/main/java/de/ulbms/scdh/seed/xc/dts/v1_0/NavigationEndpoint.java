package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.CollectionMetadataProcessor;
import de.ulbms.scdh.seed.xc.dts.endpoints.NavigationApi;
import de.ulbms.scdh.seed.xc.dts.model.Navigation;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collections;
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
	private String TRANSFORMATION;

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
	TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProvider resourceProvider;

	@Inject
	HttpServerRequest request;

	/**
	 * <P>Implementation of the DTS Navigation endpoint.</P>
	 * <P>This first gets the resource using the resource provider and then transformes it.</P>
	 *
	 * @param resource - Resource identifier. Passed as runtime parameter to the transformation and also to the resource provider.
	 * @param ref - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param start - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param end - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param down - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param tree - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param page - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param cr - Context information for getting the resource as {@link Map<String,String>}. This hash map is passed to the resource provider.
	 * @param cf - Context information for follow-up links as {@link Map<String,String>}. These are passed as runtime parameters to the transformation.
	 * @param direct - Whether to interpret the resource parameter directly as a link to the resource
	 * @return The document or parts of it in the requested media type.
	 */
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
			Map<String, String> cf,
			Boolean direct) {

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		if (resource != null) map.put("resource", pvOf(resource));
		if (down != null) map.put("down", pvOf(down.toString()));
		if (tree != null) map.put("tree", pvOf(tree));
		if (page != null) map.put("page", pvOf(page.toString()));
		if (ref != null) map.put("ref", pvOf(ref));
		if (start != null) map.put("start", pvOf(start));
		if (end != null) map.put("end", pvOf(end));
		// all cf (= Context Follow-ups) parameters are passed to the stylesheet
		if (cf != null) for (String k : cf.keySet()) map.put(k, pvOf(cf));
		params.globalParameters(map);

		// get the transformation or return failure
		Transformation transformation = transformations.get(TRANSFORMATION);
		if (transformation == null) {
			LOG.error("transformation not available: {}", TRANSFORMATION);
			return Uni.createFrom()
					.failure(new jakarta.ws.rs.BadRequestException("transformation not available: " + TRANSFORMATION));
		}

		// Create ResourceInContext from resource parameter and additional parameters
		if (cr == null) cr = Map.of();
		LOG.info("additional parameters cr {}", cr);
		Map<String, String> crContext = Collections.unmodifiableMap(cr);
		Uni<ResourceInContext> uniRic;
		if (RESOURCE_IS_PATH || (direct != null && direct)) {
			ResourceInContext ric = new ResourceInContext(crContext, resource);
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
						return collectionMetadataProc.getResourceLocation(s, GRAPH, crContext, resource);
					})
					.onItem()
					.transform((location -> {
						return new ResourceInContext(crContext, location);
					}));
		}

		return uniRic.plug((r) -> {
					return resourceProvider.asyncOpenStream(r, request);
				})
				.plug((s) -> {
					return transformation.transformAsync(params, null, resource, s, resourceProvider, request);
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
