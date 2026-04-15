package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.TransformationIDs;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationsApi;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
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
	public Uni<TransformationIDs> transformationsGet() {
		TransformationIDs ids = new TransformationIDs();
		ids.addAll(transformationsMap.keySet());
		return Uni.createFrom().item(ids);
	}

	@Override
	public Uni<TransformationInfo> transformationsTransformationInfoGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			TransformationInfo info = transformationsMap.get(transformationId).getTransformationInfo();
			return Uni.createFrom().item(info);
		} else {
			return Uni.createFrom().failure(NotFoundException::new);
		}
	}

	@Override
	public Uni<XsltParameterDetails> transformationsTransformationParametersGet(String transformationId) {
		if (transformationsMap.containsKey(transformationId)) {
			XsltParameterDetails parameters =
					transformationsMap.get(transformationId).getTransformationParameters();
			return Uni.createFrom().item(parameters);
		} else {
			return Uni.createFrom().failure(NotFoundException::new);
		}
	}
}
