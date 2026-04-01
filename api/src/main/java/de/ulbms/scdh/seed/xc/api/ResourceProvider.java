package de.ulbms.scdh.seed.xc.api;

import io.smallrye.mutiny.Uni;
import java.io.InputStream;

/**
 * A {@link ResourceProvider} is a plugin, that provides access
 * resources in sense in some persistence layer. Resource here is
 * taken in the sense DTS concepts: It is a document. This interface
 * makes no assumption about the format.
 */
public interface ResourceProvider {

	/**
	 * Returns a {@link InputStream} for a given resource identifier as used in the
	 * DTS service.
	 *
	 * @param resourceInContext - An identifier of the requested resource
	 */
	InputStream getSource(ResourceInContext resourceInContext)
			throws ResourceNotFoundException, ResourceException, ResourceProviderConfigurationException,
					ConfigurationException;

	/**
	 * Returns a {@link InputStream} of a resource wrapped in a {@link Uni}.
	 *
	 * @param resourceInContextUni - Information for identifying the resource, wrapped in a {@link Uni}
	 */
	Uni<InputStream> getResource(Uni<ResourceInContext> resourceInContextUni);
}
