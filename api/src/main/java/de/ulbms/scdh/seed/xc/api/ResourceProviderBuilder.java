package de.ulbms.scdh.seed.xc.api;

import java.net.URI;

/**
 * A {@link ResourceProviderBuilder} configures and makes a {@link ResourceProvider}.
 */
public interface ResourceProviderBuilder {

	/**
	 * Returns the identifier of the resource provider, e.g. <code>urn</code> or <code>file</code>.
	 * It may be used as a segment of an URL path.
	 * @return the type.
	 */
	String getId();

	/**
	 * Sets up the provider with a base URI.
	 * @param base - the base URI against which all requests for resources will be resolved against.
	 */
	ResourceProvider withBase(URI base) throws ResourceException;
}
