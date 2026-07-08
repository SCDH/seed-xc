package de.ulbms.scdh.seed.xc.api;

import java.net.URI;

/**
 * A {@link ResourceProviderManager} is a bean that provides access to {@link ResourceProvider}s.
 */
public interface ResourceProviderManager {

	/**
	 * Returns a {@link ResourceProvider} for an identifier, e.g.
	 * <code>urn</code>, and a base {@link URI}.
	 * @param id - the identifier or type
	 * @return a resource provider
	 */
	ResourceProviderBuilder get(String id) throws ResourceProviderConfigurationException;
}
