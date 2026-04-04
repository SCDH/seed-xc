package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.TransformationIDs;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationsApi;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

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
		return Uni.createFrom().item(ids).toMulti();
	}

	@Override
	public Multi<TransformationInfo> transformationsTransformationInfoGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			TransformationInfo info = transformationsMap.get(transformationId).getTransformationInfo();
			return Uni.createFrom().item(info).toMulti();
		} else {
			return Multi.createFrom().failure(NotFoundException::new);
		}
	}

	@Override
	public Multi<XsltParameterDetails> transformationsTransformationParametersGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			XsltParameterDetails parameters =
					transformationsMap.get(transformationId).getTransformationParameters();
			return Uni.createFrom().item(parameters).toMulti();
		} else {
			return Multi.createFrom().failure(NotFoundException::new);
		}
	}
}
