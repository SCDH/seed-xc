package de.ulbms.scdh.seed.xc.api;

import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import java.io.InputStream;

/**
 * A {@link MappingTransformation} is {@link Transformation} with the additional ability to create
 *  a {@link MappedDOMResource}.
 */
public interface MappingTransformation extends Transformation {

	/**
	 * Returns <code>true</code> if and only if the transformation can create a mapped resource.<P/>
	 *
	 * This indicates, e.g., that the compilation of the mapping transformation was successful.
	 */
	boolean canMapResource();

	/**
	 * Similar to {@link Transformation#transformAsync(RuntimeParameters, Config, String, Uni, ResourceProvider, HttpServerRequest)},
	 * but returns a {@link MappedDOMResource}.
	 *
	 * @param parameters - {@link RuntimeParameters} to apply on the transformation
	 * @param config - per-request {@link Config} for the transformation
	 * @param systemId - {@link String} pointing to the source documents location and set as XML base property
	 * @param source - {@link InputStream} with the XML document to be transformed
	 * @param resourceProvider - {@link ResourceProvider} used for getting secondary documents
	 * @param request - provides access to the incoming HTTP request
	 * @return a mapped resource
	 */
	Uni<MappedDOMResource> mapResourceAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider,
			HttpServerRequest request);
}
