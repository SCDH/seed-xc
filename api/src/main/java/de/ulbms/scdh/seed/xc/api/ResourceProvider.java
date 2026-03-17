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
	 * Returns a {@link Source} for a given resource identifier as used in the
	 * DTS service.
	 */
	InputStream getSource(String resource)
		throws ResourceNotFoundException, ResourceException,
			   ResourceProviderConfigurationException, ConfigurationException;
}
