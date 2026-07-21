package de.ulbms.scdh.seed.xc.dts.v1_0;

import de.ulbms.scdh.seed.xc.dts.endpoints.StandoffApi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.EntityPart;
import java.net.URI;

@RequestScoped
public class StandoffEndpoint implements StandoffApi {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<String> standoffBackward(
			URI resource,
			URI provider,
			URI location,
			EntityPart annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			EntityPart frame) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<String> standoffForward(
			URI resource,
			URI provider,
			URI location,
			EntityPart annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			EntityPart frame) {
		return null;
	}
}
