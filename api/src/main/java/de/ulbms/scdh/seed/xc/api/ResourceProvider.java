package de.ulbms.scdh.seed.xc.api;

import java.io.InputStream;

/**
 * A {@link SourceProvider} is a plugin, that provides access
 * resources in sense in some persistence layer. Resource here is
 * taken in the sense DTS concepts: It is a document. This interface
 * makes no assumption about the format.
 */
public interface ResourceProvider {

	/**
	 * Returns the name of the class. This may be required in a bean context.
	 */
	String getName();

	/**
	 * Setup method to be called after the initialization.
	 */
	void setUp();

	/**
	 * Returns a {@link Source} for a given resource identifier as used in the
	 * DTS service.
	 *
	 * @param context - The key to the context the resource is from
	 * @param resource - An identifier of the requested resource
	 */
	InputStream getSource(ResourceInContext resourceInContext)
		throws ResourceNotFoundException, ResourceException,
			   ResourceProviderConfigurationException, ConfigurationException;
}
