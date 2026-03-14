package de.ulbms.scdh.seed.xc.harden;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class RestrictiveUnparsedTextResolverTest {

	private static final File XSL_DIR =
		Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File ID_XSL = new File(XSL_DIR, "id.xsl");

	private static final File UNKNOWN_XSL = new File(XSL_DIR, "unknown.xsl");

	private static final File HELLO_XML =
		Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private static final File HELLO_XML_VIA_DOTS =
		Paths
			.get("src", "test", "resources", "xsl", "..", "samples",
				 "hello.xml")
			.toFile();

	private static final Configuration config = new Configuration();

	private static final String encoding = "UTF-8";

	RestrictiveUnparsedTextResolver resolver;

	@BeforeEach
	public void setupResolver() throws ConfigurationException {
		resolver = new RestrictiveUnparsedTextResolver(
			XSL_DIR.getAbsoluteFile().toString(),
			ID_XSL.getAbsoluteFile().toString());
	}

	@Test
	public void nullPath() {
		assertThrows(ConfigurationException.class,
					 () -> new RestrictiveUnparsedTextResolver(null, null));
	}

	@Test
	public void nullFilePath() {
		assertThrows(ConfigurationException.class,
					 ()
						 -> new RestrictiveUnparsedTextResolver("file:/etc",
																"/etc/passwd"));
	}

	@Test
	public void emptyPath() throws ConfigurationException, XPathException {
		assertThrows(
			ConfigurationException.class,
			() -> new RestrictiveUnparsedTextResolver("", "/etc/passwd"));
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	public void rootPath() throws ConfigurationException, XPathException,
								  URISyntaxException, IOException {
		RestrictiveUnparsedTextResolver resolver =
			new RestrictiveUnparsedTextResolver("/", "/gnu-herd-2.1");
		URI request = new URI("/etc/passwd");
		// resolving may fail on non-*nix
		Reader resource = resolver.resolve(request, encoding, config);
		assertTrue(resource.read() > 0); // not EOF
	}

	@Test
	public void validPathIdentityXSL()
		throws XPathException, URISyntaxException, IOException {
		URI request = new URI(ID_XSL.getAbsolutePath().toString());
		Reader resource = resolver.resolve(request, encoding, config);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Test
	public void validPathFileIdentityXSL()
		throws XPathException, URISyntaxException, IOException {
		URI request = new URI("file:" + ID_XSL.getAbsolutePath().toString());
		Reader resource = resolver.resolve(request, encoding, config);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Test
	public void relativePathIdentityXSL()
		throws XPathException, URISyntaxException, IOException {
		URI request = new URI("id.xsl");
		Reader resource = resolver.resolve(request, encoding, config);
		assertEquals(0x3c, resource.read()); // 0x3c == '<'
	}

	@Test
	public void validPathUnknownXSL()
		throws XPathException, URISyntaxException {
		URI request = new URI(UNKNOWN_XSL.getAbsolutePath().toString());
		// the resource does not exist, so trying to make a FileReader from it
		// fails
		assertThrows(XPathException.class,
					 () -> resolver.resolve(request, encoding, config));
	}

	@Test
	public void illegalPathFileEtcPasswd() throws URISyntaxException {
		URI request = new URI("file:/etc/passwd");
		assertThrows(XPathException.class,
					 () -> resolver.resolve(request, encoding, config));
	}

	@Test
	public void illegalPathEtcPasswd() throws URISyntaxException {
		URI request = new URI("/etc/passwd");
		assertThrows(XPathException.class,
					 () -> resolver.resolve(request, encoding, config));
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
		URI request = new URI(HELLO_XML.getAbsolutePath().toString());
		assertThrows(XPathException.class,
					 () -> resolver.resolve(request, encoding, config));
	}

	@Test
	public void normalizationWorksForHelloXMLViaDots()
		throws URISyntaxException {
		URI request = new URI(HELLO_XML_VIA_DOTS.getAbsolutePath().toString());
		assertThrows(XPathException.class,
					 () -> resolver.resolve(request, encoding, config));
	}
}
