package de.ulbms.scdh.seed.xc.api;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import java.io.InputStream;
import java.net.URI;

/**
 * A {@link ResourceProvider} is a plugin, that provides access
 * resources in sense in some persistence layer. Resource here is
 * taken in the sense DTS concepts: It is a document. This interface
 * makes no assumption about the format.<P/>
 *
 * Implementations should have the
 * {@link de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider}
 * qualifier assigned in order to avoid ambiguities with Saxon
 * resolver beans.
 */
public interface ResourceProvider {

	/**
	 * Tries to open an input stream for a given {@link URI}
	 * @param uri - The resource as {@link URI}
	 * @return the resource as {@link InputStream}
	 * @throws ResourceProviderConfigurationException - on bad configuration
	 * @throws ResourceNotFoundException - when resource not found
	 * @throws ResourceException - when resource cannot be read
	 */
	InputStream openStream(URI uri)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException;

	/**
	 * Returns a {@link InputStream} of a resource wrapped in a {@link Uni}.
	 *
	 * @param resourceInContextUni - Information for identifying the resource, wrapped in a {@link Uni}
	 * @param request - access to the incoming HTTP request
	 */
	Uni<InputStream> getResource(Uni<ResourceInContext> resourceInContextUni, HttpServerRequest request);
}
