package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link UrlResourceProviderBuilder} is a {@link ResourceProviderBuilder} that produces {@link UrlResourceProvider}
 * for getting resources from locations on the web.
 */
@ApplicationScoped
public class UrlResourceProviderBuilder extends UrlValidator implements ResourceProviderBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(UrlResourceProviderBuilder.class);

	@Inject
	protected UrlConfig config;

	public UrlResourceProviderBuilder() {}

	@Inject
	public UrlResourceProviderBuilder(UrlConfig config) {
		LOG.info("used simplified injection to configure URL provider builder. {}", config.getAllowedProtocols());
		this.config = config;
	}

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
		LOG.info(
				"going to configure URL provider with base {} and configuration {}",
				base,
				config.getAllowedProtocols());
		configure(config);
		check(base);
		LOG.info("creating URL provider with base {} and configuration {}", base, config);
		configure(config);
		return new UrlResourceProvider(base, config);
	}
}
