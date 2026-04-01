package de.ulbms.scdh.seed.xc.api;

/**
 * An exception that occurred in one of the following phases:
 *
 * 1) during the configuration of the service, e.g. when setting a a
 * {@link ResourceProvider}.
 *
 * 2) during the access to the component which the
 *
 * {@link ResourceProvider} offers access to, e.g. because of downtime
 * or because of configuration errors.
 */
public class ResourceProviderConfigurationException extends Exception {
	public ResourceProviderConfigurationException(String msg) {
		super(msg);
	}

	public ResourceProviderConfigurationException(Throwable cause) {
		super(cause);
	}

	public ResourceProviderConfigurationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
