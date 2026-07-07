package de.ulbms.scdh.seed.xc.resources.manager;

import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import de.ulbms.scdh.seed.xc.api.ResourceProviderManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ResourceProviderManager} implementation that looks up providers registered as plugins with the SPI.
 */
@ApplicationScoped
public class Manager implements ResourceProviderManager {

	private static final Logger LOG = LoggerFactory.getLogger(Manager.class);

	@Inject
	Instance<ResourceProvider> resourceProviderSelector;

	private final ServiceLoader<ResourceProvider> resourceProviderLoader = ServiceLoader.load(ResourceProvider.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResourceProvider get(String id, URI base) throws ResourceProviderConfigurationException {

		Iterator<ResourceProvider> services = resourceProviderLoader.iterator();
		while (services.hasNext()) {
			ResourceProvider service = services.next();
			if (service.getId().equals(id)) {
				// dynamically create a bean, this is idiomatic
				Instance<? extends ResourceProvider> resourceProviderInstance =
						resourceProviderSelector.select(service.getClass());
				ResourceProvider resourceProvider = resourceProviderInstance.get();
				// setup with base URI
				resourceProvider.setup(base);
				return resourceProvider;
			}
		}
		LOG.error("resource provider {} not found", id);
		throw new ResourceProviderConfigurationException("resource provider not found: " + id);
	}
}
