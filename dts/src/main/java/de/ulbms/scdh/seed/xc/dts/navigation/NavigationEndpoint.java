package de.ulbms.scdh.seed.xc.dts.navigation;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
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
import java.net.URI;
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

	private static final Logger LOG =
		LoggerFactory.getLogger(NavigationEndpoint.class);

	/**
	 * The ID of the transformation using for transforming a resource.
	 */
	@ConfigProperty(
		name = "de.ulbms.scdh.seed.xc.dts.NavigationEndpoint.TRANSFORMATION",
		defaultValue = "navigation")
	private String TRANSFORMATION;

	@Inject TransformationMap transformations;

	@Inject ResourceProvider resourceProvider;

	@Override
	public Uni<Navigation> navigation(URI collection, String resource,
									  String ref, String start, String end,
									  Integer down, String tree, Integer page) {
		return Uni.createFrom()
			.item(new ResourceInContext("", resource))
			.onItem()
			.transform((ric) -> {
				try {
					return resourceProvider.getSource(ric);
				} catch (ResourceNotFoundException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.NotFoundException(e.getMessage());
				} catch (ConfigurationException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				} catch (ResourceProviderConfigurationException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				} catch (ResourceException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				}
			})
			.onItem()
			.transform((s) -> {
				Transformation transformation =
					transformations.get(TRANSFORMATION);
				if (transformation == null) {
					LOG.error("transformation not available: {}",
							  TRANSFORMATION);
					throw new jakarta.ws.rs.BadRequestException(
						"transformation not available: " + TRANSFORMATION);
				}
				try {
					return transformation.transform(null, null, resource, s);
				} catch (TransformationPreparationException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				} catch (TransformationException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
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
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				} catch (StreamReadException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				} catch (IOException e) {
					LOG.error(e.getMessage());
					throw new jakarta.ws.rs.InternalServerErrorException(
						e.getMessage());
				}
			});
	}
}
