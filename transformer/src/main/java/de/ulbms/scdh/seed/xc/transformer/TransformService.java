package de.ulbms.scdh.seed.xc.transformer;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the web service, <code>/transform</code> path.
 *
 */
@RequestScoped
public class TransformService implements TransformApi {

	private static final Logger LOG = LoggerFactory.getLogger(TransformService.class);

	@Inject
	TransformationMap transformationsMap;

	@TransformTimeProvider
	@Inject
	ResourceProvider resourceProvider;

	@Override
	public Uni<byte[]> transformTransformationUrlPost(
			String transformationId, String url, RuntimeParameters parameters, Config config) {

		Transformation transformation = transformationsMap.get(transformationId);
		if (transformation == null) {
			return Uni.createFrom().failure(new NotFoundException("unknown transformation " + transformationId));
		}

		ResourceInContext ric = new ResourceInContext(Map.of(), url);
		Uni<ResourceInContext> uniRic = Uni.createFrom().item(ric);

		return uniRic.plug(resourceProvider::getResource).onItem().transform((s) -> {
			try {
				return transformation.transform(parameters, config, url, s, resourceProvider);
			} catch (TransformationPreparationException | TransformationException e) {
				LOG.error(e.getMessage());
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}

	@Override
	public Uni<byte[]> transformTransformationUrlGet(String transformationId, String url) {
		return transformTransformationUrlPost(transformationId, url, null, null);
	}

	@Override
	public Uni<byte[]> transformTransformationPost(
			String transformationId,
			InputStream source,
			String url,
			@FormParam(value = "runtimeParameters") RuntimeParameters parameters,
			@FormParam(value = "config") Config config) {

		Transformation transformation = transformationsMap.get(transformationId);
		if (transformation == null) {
			return Uni.createFrom().failure(new NotFoundException("unknown transformation " + transformationId));
		}

		ResourceInContext ric = new ResourceInContext(Map.of(), url);
		return Uni.createFrom().item(source).onItem().transform((s) -> {
			try {
				return transformation.transform(parameters, config, url, s, resourceProvider);
			} catch (TransformationPreparationException | TransformationException e) {
				LOG.error(e.getMessage());
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}
}
