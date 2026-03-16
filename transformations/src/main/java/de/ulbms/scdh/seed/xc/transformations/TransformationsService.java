package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationIDs;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationsApi;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the web service,
 * <code>/transformations</code> path. It provides access to available
 * transformations and informations about them.
 */
@ApplicationScoped
public class TransformationsService implements TransformationsApi {

	private final Logger LOGGER =
		LoggerFactory.getLogger(TransformationsService.class);

	@Inject TransformationMap transformationsMap;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response transformationsGet() {
		TransformationIDs ids = new TransformationIDs();
		for (String id : transformationsMap.keySet()) {
			ids.add(id);
		}
		RestResponse<TransformationIDs> response = RestResponse.ok(ids);
		return response.toResponse();
	}

	@Override
	public Response
	transformationsTransformationInfoGet(String transformationId) {
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
	transformationsTransformationParametersGet(String transformationId) {
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
}
