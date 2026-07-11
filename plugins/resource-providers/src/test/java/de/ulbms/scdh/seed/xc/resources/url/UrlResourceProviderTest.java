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
import org.junit.jupiter.api.Test;

public class UrlResourceProviderTest {

	private static final URI RESOURCES = Path.of("src", "test", "resources").toUri();

	private static final String DOMAINS_WHITE_MS = "^.*\\.uni-muenster.de$";

	private static final String DOMAINS_WHITE_RUB = "^.*\\.ruhr-uni-bochum.de$";

	private static final String DOMAINS_WHITE_ALL = "";

	private static final String DOMAINS_BLACK_COM = ".*\\.com$";

	UrlResourceProviderBuilder builder;

	ResourceProvider provider;

	@BeforeEach
	public void createProvider()
			throws ResourceException, ResourceProviderConfigurationException, ResourceNotFoundException {
		builder = new UrlResourceProviderBuilder();
		builder.allowedProtocols = "file";
		builder.domainWhiteList = ".*";
		builder.domainBlackList = "asdf";
		builder.allowedFilePath = RESOURCES.getSchemeSpecificPart();
		provider = builder.withBase(RESOURCES);
		// provider = new UrlResourceProvider(RESOURCES);
	}

	@Test
	public void testResourcesFileUri() {
		assertEquals("file", RESOURCES.getScheme());
	}

	@Test
	public void testSplitString() {
		String[] splits = {"a", "b"};
		assertEquals(2, "a,b".split(",").length);
		assertEquals(1, "ab".split(",").length);
	}

	@Test
	public void testBadScheme() {
		UrlResourceProvider provider2 =
				new UrlResourceProvider(RESOURCES, "https", ".*", ".*", RESOURCES.getSchemeSpecificPart());
		assertThrows(ResourceException.class, () -> {
			provider2.openStream(new URI("samples/hello.xml"));
		});
	}

	@Test
	public void testFileScheme() {
		UrlResourceProvider provider2 =
				new UrlResourceProvider(RESOURCES, "file", ".*", ".*", RESOURCES.getSchemeSpecificPart());
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
}
