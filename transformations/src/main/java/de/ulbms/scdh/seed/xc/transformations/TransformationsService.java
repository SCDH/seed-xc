package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.TransformationIDs;
import de.ulbms.scdh.seed.xc.api.TransformationsApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the web service,
 * <code>/transformations</code> path. It returns a list of available
 * transformations.
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
}
