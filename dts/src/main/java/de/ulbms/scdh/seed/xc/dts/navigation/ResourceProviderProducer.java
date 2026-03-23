package de.ulbms.scdh.seed.xc.dts.navigation;

import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ResourceProviderProducer {

	private static final Logger LOG =
		LoggerFactory.getLogger(ResourceProviderProducer.class);

	private ServiceLoader<ResourceProvider> resourceProviderLoader =
		ServiceLoader.load(ResourceProvider.class);

	private ResourceProvider resourceProvider = null;

	@Inject Instance<ResourceProvider> resourceProviderSelector;

	public ResourceProviderProducer(@ConfigProperty(
		name = "de.ulbms.scdh.seed.xc.dts.ResourceProviderProducer.class",
		defaultValue = "de.ulbms.scdh.seed.xc.resources.filesystem."
					   + "FileSystemResourceProvider") String clazz) {
		LOG.info("trying to instantiate resource provider {}", clazz);
		Iterator<ResourceProvider> services = resourceProviderLoader.iterator();
		while (services.hasNext() && resourceProvider == null) {
			ResourceProvider rp = services.next();
			if (clazz.equals(rp.getName())) {
				// we create a bean and therefore use
				// Instance#select(Class) to dynamically create an
				// transformation instance of a dynamically
				// determined class
				Instance<? extends ResourceProvider> resourceProviderInstance =
					resourceProviderSelector.select(rp.getClass());
				this.resourceProvider = resourceProviderInstance.get();
				this.resourceProvider.setUp();
				LOG.info("resource provider found and configured: {}", clazz);
				return;
			}
		}
		LOG.error("resource provider not found: {}", clazz);
	}

	@Produces
	public ResourceProvider create() {
		return this.resourceProvider;
	}
}
