package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;

/**
 * A {@link UrlResourceProviderBuilder} is a {@link ResourceProviderBuilder} that produces {@link UrlResourceProvider}
 * for getting resources from locations on the web.
 */
@ApplicationScoped
public class UrlResourceProviderBuilder extends UrlValidator implements ResourceProviderBuilder {

	@Inject
	protected UrlConfig config;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return "url";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResourceProvider withBase(URI base)
			throws ResourceException, ResourceProviderConfigurationException, ResourceNotFoundException {
		configure(config);
		check(base);
		return new UrlResourceProvider(base, config);
	}
}
