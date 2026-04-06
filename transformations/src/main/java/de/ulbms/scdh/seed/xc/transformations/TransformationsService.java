package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.TransformationIDs;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationsApi;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.resteasy.reactive.RestMulti;

/**
 * The implementation of the web service,
 * <code>/transformations</code> path. It provides access to available
 * transformations and information about them.
 */
@ApplicationScoped
public class TransformationsService implements TransformationsApi {

	@Inject
	TransformationMap transformationsMap;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Multi<TransformationIDs> transformationsGet() {
		TransformationIDs ids = new TransformationIDs();
		ids.addAll(transformationsMap.keySet());
		return RestMulti.fromMultiData(Multi.createFrom().item(ids)).encodeAsJsonArray(false).build();
	}

	@Override
	public Multi<TransformationInfo> transformationsTransformationInfoGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			TransformationInfo info = transformationsMap.get(transformationId).getTransformationInfo();
			Multi<TransformationInfo> data = Multi.createFrom().item(info);
			return RestMulti.fromMultiData(data).encodeAsJsonArray(false).build();
		} else {
			throw new NotFoundException();
		}
	}

	@Override
	public Multi<XsltParameterDetails> transformationsTransformationParametersGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			XsltParameterDetails parameters =
					transformationsMap.get(transformationId).getTransformationParameters();
			return RestMulti.fromMultiData(Multi.createFrom().item(parameters)).encodeAsJsonArray(false).build();
		} else {
			throw new NotFoundException();
		}
	}
}
