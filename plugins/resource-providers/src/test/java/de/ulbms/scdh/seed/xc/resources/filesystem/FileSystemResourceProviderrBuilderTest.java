package de.ulbms.scdh.seed.xc.resources.filesystem;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class FileSystemResourceProviderrBuilderTest {

	Path RESOURCES_DIR = Paths.get("src", "test", "resources");

	@Test
	public void testFileSystemProtectionEtc() {
		FileSystemResourceProviderBuilder builder = new FileSystemResourceProviderBuilder();
		builder.path = RESOURCES_DIR;
		assertThrows(ResourceException.class, () -> {
			builder.withBase(new URI("/etc"));
		});
	}

	@Test
	public void testFileSystemProtectionDotDot() {
		FileSystemResourceProviderBuilder builder = new FileSystemResourceProviderBuilder();
		builder.path = RESOURCES_DIR;
		assertThrows(ResourceException.class, () -> {
			builder.withBase(new URI(".."));
		});
	}

	@Test
	public void testFileSystemProtectionDotDotDotDot() {
		FileSystemResourceProviderBuilder builder = new FileSystemResourceProviderBuilder();
		builder.path = RESOURCES_DIR;
		assertThrows(ResourceException.class, () -> {
			builder.withBase(new URI("../.."));
		});
	}

	@Test
	public void testFileResolvesDot()
			throws URISyntaxException, ResourceProviderConfigurationException, ResourceException,
					ResourceNotFoundException, IOException {
		FileSystemResourceProviderBuilder builder = new FileSystemResourceProviderBuilder();
		builder.path = RESOURCES_DIR;
		AtomicReference<ResourceProvider> provider = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			provider.set(builder.withBase(new URI(".")));
		});
		assertNotNull(provider.get());
		try (InputStream input = provider.get().openStream(new URI("xsl/id.xsl"))) {
			String content = new String(input.readAllBytes(), Charset.defaultCharset());
			assertTrue(content.startsWith("<?xml"));
		}
	}

	@Test
	public void testFileResolvesDir()
			throws URISyntaxException, ResourceProviderConfigurationException, ResourceException,
					ResourceNotFoundException, IOException {
		FileSystemResourceProviderBuilder builder = new FileSystemResourceProviderBuilder();
		builder.path = RESOURCES_DIR;
		AtomicReference<ResourceProvider> provider = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			provider.set(builder.withBase(new URI("xsl")));
		});
		assertNotNull(provider.get());
		try (InputStream input = provider.get().openStream(new URI("id.xsl"))) {
			String content = new String(input.readAllBytes(), Charset.defaultCharset());
			assertTrue(content.startsWith("<?xml"));
		}
	}
}
