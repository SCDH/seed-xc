package de.ulbms.scdh.seed.xc.transformer;

import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import de.ulbms.scdh.seed.xc.api.TransformApi;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the web service, <code>/transform</code> path.
 *
 */
@RequestScoped
public class TransformService implements TransformApi {

	private final Logger LOGGER =
		LoggerFactory.getLogger(TransformService.class);

	@Inject TransformationMap transformationsMap;

	@Override
	public Response transformTransformationInfoGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			TransformationInfo info = transformationsMap.get(transformationId)
										  .getTransformationInfo();
			RestResponse<TransformationInfo> response = RestResponse.ok(info);
			return response.toResponse();
		} else {
			return RestResponse.status(Status.NOT_FOUND).toResponse();
		}
	}

	@Override
	public Response
	transformTransformationParametersGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			XsltParameterDetails parameters =
				transformationsMap.get(transformationId)
					.getTransformationParameters();
			RestResponse<XsltParameterDetails> response =
				RestResponse.ok(parameters);
			return response.toResponse();
		} else {
			return RestResponse.status(Status.NOT_FOUND).toResponse();
		}
	}

	@Override
	public Response transformTransformationUrlPost(String transformationId,
												   String url,
												   RuntimeParameters parameters,
												   Config config) {
		if (transformationsMap.containsKey(transformationId)) {
			try {
				Transformation transformation =
					transformationsMap.get(transformationId);
				byte[] output =
					transformation.transform(parameters, config, url);
				RestResponse<byte[]> response;
				if (transformation.getOutputMediaType() != null) {
					response = RestResponse.ok(
						output, transformation.getOutputMediaType());
				} else {
					response = RestResponse.ok(output);
				}
				return response.toResponse();
			} catch (TransformationPreparationException e) {
				// RestResponse<String> where the string holds an error message,
				// see RestResponse#status(Status, T)
				return RestResponse.status(Status.BAD_REQUEST, e.getMessage())
					.toResponse();
			} catch (TransformationException e) {
				// the log must reflect the fact that
				// transformTransformationUrlGet is also handled here
				LOGGER.error("failed GET /transform/" + transformationId +
								 "/{url} (or POST with runtime parameters)",
							 e);
				return RestResponse
					.status(Status.INTERNAL_SERVER_ERROR, e.getMessage())
					.toResponse();
			}
		} else {
			LOGGER.info("Unknown transformation '{}'", transformationId);
			return RestResponse.status(Status.NOT_FOUND).toResponse();
		}
	}

	@Override
	public Response transformTransformationUrlGet(String transformationId,
												  String url) {
		return transformTransformationUrlPost(transformationId, url, null,
											  null);
	}

	@Override
	public Response transformTransformationPost(
		String transformationId, InputStream source, String url,
		@FormParam(value = "runtimeParameters") RuntimeParameters parameters,
		@FormParam(value = "config") Config config) {
		if (transformationsMap.containsKey(transformationId)) {
			try {
				Transformation transformation =
					transformationsMap.get(transformationId);
				// RuntimeParameters parameters = null; // delete when
				// parameters are passed into this method
				byte[] output =
					transformation.transform(parameters, config, url, source);
				RestResponse<byte[]> response;
				if (transformation.getOutputMediaType() != null) {
					response = RestResponse.ok(
						output, transformation.getOutputMediaType());
				} else {
					response = RestResponse.ok(output);
				}
				return response.toResponse();
			} catch (TransformationPreparationException e) {
				return RestResponse.status(Status.BAD_REQUEST, e.getMessage())
					.toResponse();
			} catch (TransformationException e) {
				LOGGER.error("failed POST /transform/" + transformationId, e);
				return RestResponse
					.status(Status.INTERNAL_SERVER_ERROR, e.getMessage())
					.toResponse();
			}
		} else {
			LOGGER.info("Unknown transformation '{}'", transformationId);
			return RestResponse.status(Status.NOT_FOUND).toResponse();
		}
	}
}
