package de.ulbms.scdh.seed.xc.resources.manager;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderBuilder;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import de.ulbms.scdh.seed.xc.api.ResourceProviderManager;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ManagerTest {

	@TransformTimeProvider
	@Inject
	protected ResourceProviderManager manager;

	@Test
	public void testManagerBeanAvailable() {
		assertNotNull(manager);
		assertInstanceOf(ResourceProviderManager.class, manager);
	}

	@Test
	public void testGetNone() throws ResourceProviderConfigurationException {
		AtomicReference<ResourceProviderBuilder> providerRef = null;
		assertThrows(ResourceProviderConfigurationException.class, () -> {
			providerRef.set(manager.get("diamonds"));
		});
	}

	// file system resource provider build must be registered for tests
	@Test
	public void testGetFSB() throws ResourceProviderConfigurationException, URISyntaxException {
		AtomicReference<ResourceProviderBuilder> builderRef = new AtomicReference();
		assertDoesNotThrow(() -> {
			builderRef.set(manager.get("file"));
		});
		assertInstanceOf(ResourceProviderBuilder.class, builderRef.get());
		AtomicReference<ResourceProvider> providerRef = new AtomicReference();
		assertDoesNotThrow(() -> {
			providerRef.set(builderRef.get().withBase(new URI("bla")));
		});
		assertInstanceOf(ResourceProvider.class, providerRef.get());
	}
}
