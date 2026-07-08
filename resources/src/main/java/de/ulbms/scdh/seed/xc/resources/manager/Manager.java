package de.ulbms.scdh.seed.xc.resources.manager;

import de.ulbms.scdh.seed.xc.api.ResourceProviderBuilder;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import de.ulbms.scdh.seed.xc.api.ResourceProviderManager;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ResourceProviderManager} implementation that looks up providers registered as plugins with the SPI.
 */
@TransformTimeProvider
@ApplicationScoped
public class Manager implements ResourceProviderManager {

	private static final Logger LOG = LoggerFactory.getLogger(Manager.class);

	@Inject
	Instance<ResourceProviderBuilder> resourceProviderSelector;

	private final ServiceLoader<ResourceProviderBuilder> resourceProviderLoader =
			ServiceLoader.load(ResourceProviderBuilder.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResourceProviderBuilder get(String id) throws ResourceProviderConfigurationException {

		Iterator<ResourceProviderBuilder> services = resourceProviderLoader.iterator();
		while (services.hasNext()) {
			ResourceProviderBuilder service = services.next();
			if (service.getId().equals(id)) {
				// dynamically create a bean, this is idiomatic
				Instance<? extends ResourceProviderBuilder> resourceProviderInstance =
						resourceProviderSelector.select(service.getClass());
				ResourceProviderBuilder builder = resourceProviderInstance.get();
				// setup with base URI
				return builder;
			}
		}
		LOG.error("resource provider {} not found", id);
		throw new ResourceProviderConfigurationException("resource provider not found: " + id);
	}
}
