package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderBuilder;
import jakarta.enterprise.context.ApplicationScoped;
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
	public ResourceProvider withBase(URI base) throws ResourceException {
		configure();
		check(base);
		return new UrlResourceProvider(base, allowedProtocols, domainWhiteList, domainBlackList);
	}
}
