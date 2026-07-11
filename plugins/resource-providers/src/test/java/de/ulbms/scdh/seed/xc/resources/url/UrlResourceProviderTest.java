package de.ulbms.scdh.seed.xc.resources.url;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class UrlResourceProviderTest {

	private static final URI RESOURCES = Path.of("src", "test", "resources").toUri();

	UrlResourceProviderBuilder builder;

	ResourceProvider provider;

	UrlConfig config;

	@BeforeEach
	public void createProvider()
			throws ResourceException, ResourceProviderConfigurationException, ResourceNotFoundException {
		config = new UrlConfig();
		config.allowedProtocols = "file";
		config.domainWhiteList = ".*";
		config.domainBlackList = "asdf";
		config.allowedFilePath = RESOURCES.getSchemeSpecificPart();
		builder = new UrlResourceProviderBuilder();
		builder.config = config;
		provider = builder.withBase(RESOURCES);
		// provider = new UrlResourceProvider(RESOURCES);
	}

	@Test
	public void testResourcesFileUri() {
		assertEquals("file", RESOURCES.getScheme());
	}

	@Test
	public void testSplitString() {
		assertEquals(2, "a,b".split(",").length);
		assertEquals("a", "a,b".split(",")[0]);
		assertEquals(1, "ab".split(",").length);
	}

	@Test
	public void testBadScheme() throws ResourceProviderConfigurationException {
		config.allowedProtocols = "https";
		UrlResourceProvider provider2 = new UrlResourceProvider(RESOURCES, config);
		assertThrows(ResourceException.class, () -> {
			provider2.openStream(new URI("samples/hello.xml"));
		});
	}

	@Test
	public void testFileScheme() throws ResourceProviderConfigurationException {
		UrlResourceProvider provider2 = new UrlResourceProvider(RESOURCES, config);
		assertDoesNotThrow(() -> {
			InputStream input = provider2.openStream(new URI("samples/hello.xml"));
		});
	}

	@Test
	public void testFileForbiddenEtcPasswd() {
		assertThrows(
				ResourceNotFoundException.class,
				() -> {
					InputStream input = provider.openStream(new URI("/etc/passwd"));
				},
				"access to forbidden file paths cause NotFound for not leaking internal information");
	}

	@Test
	public void testFileForbiddenRelative() {
		assertThrows(ResourceNotFoundException.class, () -> {
			InputStream input = provider.openStream(new URI("../../../pom.xml"));
		});
	}

	@Disabled
	@Test
	public void testConnectionTimeout() throws ResourceProviderConfigurationException {
		config.connectTimeout = 0;
		UrlResourceProvider provider2 = new UrlResourceProvider(RESOURCES, config);
		assertThrows(ResourceNotFoundException.class, () -> {
			InputStream input = provider2.openStream(new URI("samples/hello.xml"));
		});
	}

}
