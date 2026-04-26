package de.ulbms.scdh.seed.xc.resources.filesystem;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class FileSystemResourceProviderTest {

	private static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File ID_XSL = new File(XSL_DIR, "id.xsl");

	private static final File UNKNOWN_XSL = new File(XSL_DIR, "unknown.xsl");

	private static final File HELLO_XML =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private static final File HELLO_XML_VIA_DOTS = Paths.get(
					"src", "test", "resources", "xsl", "..", "samples", "hello.xml")
			.toFile();

	private static final String encoding = "UTF-8";

	FileSystemResourceProvider provider;

	Reader resource;

	InputStream resourceStream;

	@BeforeEach
	public void setupProvider() throws ConfigurationException {
		provider = new FileSystemResourceProvider(XSL_DIR.getAbsoluteFile().toString());
		if (provider.getError() != null) {
			throw new ConfigurationException(provider.getError().getMessage());
		}
	}

	@AfterEach
	public void tearDown() throws IOException {
		if (resource != null) {
			resource.close();
		}
		if (resourceStream != null) {
			resourceStream.close();
		}
	}

	@Test
	public void nullPath() {
		FileSystemResourceProvider provider = new FileSystemResourceProvider(null);
		assertNotNull(provider.getError());
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	public void rootPath()
			throws URISyntaxException, IOException, ResourceProviderConfigurationException, ResourceNotFoundException,
					ResourceException {
		FileSystemResourceProvider resolver = new FileSystemResourceProvider("/");
		URI request = new URI("/etc/passwd");
		// resolving may fail on non-*nix
		resource = new InputStreamReader(resolver.openStream(request), encoding);
		assertTrue(resource.read() > 0); // not EOF
	}

	@Test
	public void validPathIdentityXSL()
			throws URISyntaxException, IOException, ResourceProviderConfigurationException, ResourceNotFoundException,
					ResourceException {
		URI request = new URI(ID_XSL.getAbsolutePath());
		resource = new InputStreamReader(provider.openStream(request), encoding);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Test
	public void validPathFileIdentityXSL()
			throws URISyntaxException, IOException, ResourceProviderConfigurationException, ResourceNotFoundException,
					ResourceException {
		URI request = new URI("file:" + ID_XSL.getAbsolutePath());
		resource = new InputStreamReader(provider.openStream(request), encoding);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Disabled("currently not resolving relative paths")
	@Test
	public void relativePathIdentityXSL()
			throws URISyntaxException, IOException, ResourceProviderConfigurationException, ResourceNotFoundException,
					ResourceException {
		URI request = new URI("id.xsl");
		resource = new InputStreamReader(provider.openStream(request), encoding);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Test
	public void validPathUnknownXSL() throws URISyntaxException {
		URI request = new URI(UNKNOWN_XSL.getAbsolutePath());
		// the resource does not exist, so trying to make a FileReader from it
		// fails
		assertThrows(ResourceNotFoundException.class, () -> resourceStream = provider.openStream(request));
	}

	@Test
	public void illegalPathFileEtcPasswd() throws URISyntaxException {
		URI request = new URI("file:/etc/passwd");
		assertThrows(ResourceException.class, () -> resourceStream = provider.openStream(request));
	}

	@Test
	public void illegalPathEtcPasswd() throws URISyntaxException {
		URI request = new URI("/etc/passwd");
		assertThrows(ResourceException.class, () -> resourceStream = provider.openStream(request));
	}

	// @Test
	// @Disabled
	// public void delegateHttpRequest() throws XPathException,
	// URISyntaxException { 	URI request = new
	// URI("http://example.com/some/transform.xsl");
	// 	assertNull(resolver.resolve(request, encoding, config));
	// }

	// @Test
	// @Disabled
	// public void delegateHttpsRequest() throws XPathException,
	// URISyntaxException { 	URI request = new
	// URI("https://example.com/some/transform.xsl");
	// 	assertNull(resolver.resolve(request, encoding, config));
	// }

	@Test
	public void illegalPathHelloXML() throws URISyntaxException {
		URI request = new URI(HELLO_XML.getAbsolutePath());
		assertThrows(ResourceException.class, () -> resourceStream = provider.openStream(request));
	}

	@Test
	public void normalizationWorksForHelloXMLViaDots() throws URISyntaxException {
		URI request = new URI(HELLO_XML_VIA_DOTS.getAbsolutePath());
		assertThrows(ResourceException.class, () -> resourceStream = provider.openStream(request));
	}
}
