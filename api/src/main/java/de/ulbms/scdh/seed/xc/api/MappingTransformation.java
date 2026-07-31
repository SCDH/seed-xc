package de.ulbms.scdh.seed.xc.api;

import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import java.io.InputStream;
import java.net.URI;

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
	 * but returns a {@link MappedDOMResource}.<P/>
	 *
	 * There's a slight but decisive difference between the <code>systemId</code> and the <code>preimageIri</code>
	 * parameters. The systemId is the physical location of the resource and may be important of processing XInclude or
	 * other things that may require resolving physical locations. The <code>preimageIri</code> is the IRI of the
	 * resource as delivered by an endpoint and that may be used in a Linked Open Data context.
	 *
	 * @param parameters - {@link RuntimeParameters} to apply on the transformation
	 * @param config - per-request {@link Config} for the transformation
	 * @param systemId - {@link String} pointing to the source documents location and set as XML base property
	 * @param preimageIri - the IRI af the resource being transformed
	 * @param imageIri - the IRI af the derived representation
	 * @param source - {@link InputStream} with the XML document to be transformed
	 * @param resourceProvider - {@link ResourceProvider} used for getting secondary documents
	 * @param request - provides access to the incoming HTTP request
	 * @return a mapped resource
	 */
	Uni<MappedDOMResource> mapResourceAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			URI preimageIri,
			URI imageIri,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider,
			HttpServerRequest request);
}
