package de.ulbms.scdh.seed.xc.harden;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import java.io.File;
import java.nio.file.Paths;
import javax.xml.transform.Source;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileURIResolverTest {

	private static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File ID_XSL = new File(XSL_DIR, "id.xsl");

	private static final File UNKNOWN_XSL = new File(XSL_DIR, "unknown.xsl");

	private static final File HELLO_XML =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private static final File HELLO_XML_VIA_DOTS = Paths.get(
					"src", "test", "resources", "xsl", "..", "samples", "hello.xml")
			.toFile();

	FileURIResolver resolver;

	@BeforeEach
	public void setupResolver() throws ConfigurationException {
		resolver = new FileURIResolver(
				XSL_DIR.getAbsoluteFile().toString(), ID_XSL.getAbsoluteFile().toString());
	}

	@Test
	public void nullPath() {
		assertThrows(ConfigurationException.class, () -> new FileURIResolver(null, null));
	}

	@Test
	public void nullFilePath() {
		assertThrows(ConfigurationException.class, () -> new FileURIResolver("file:/etc", "/etc/passwd"));
	}

	@Test
	public void emptyPath() throws ConfigurationException, XPathException {
		assertThrows(ConfigurationException.class, () -> new FileURIResolver("", "/etc/passwd"));
	}

	@Test
	public void rootPath() throws ConfigurationException, XPathException {
		ResourceResolver resolver = new FileURIResolver("/", "/gnu-herd-2.1");
		ResourceRequest request = new ResourceRequest();
		request.uri = "/etc/passwd";
		Source resource = resolver.resolve(request);
		assertTrue(resource.getSystemId().startsWith("file:"));
		assertTrue(resource.getSystemId().endsWith("/etc/passwd"));
	}

	@Test
	public void validPathIdentityXSL() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = ID_XSL.getAbsolutePath().toString();
		System.out.println(request.uri);
		Source resource = resolver.resolve(request);
		System.out.println(resource.getSystemId());
		assertTrue(resource.getSystemId().startsWith("file:"));
		assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
	}

	@Test
	public void validPathFileIdentityXSL() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = "file:" + ID_XSL.getAbsolutePath().toString();
		Source resource = resolver.resolve(request);
		assertTrue(resource.getSystemId().startsWith("file:"));
		assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
	}

	@Test
	public void relativePathIdentityXSL() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = "id.xsl";
		Source resource = resolver.resolve(request);
		assertTrue(resource.getSystemId().startsWith("file:"));
		assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
	}

	@Test
	public void validPathUnknownXSL() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = UNKNOWN_XSL.getAbsolutePath().toString();
		// neither the resolver nor the source asserts that the resource really
		// exists
		Source resource = resolver.resolve(request);
		assertTrue(resource.getSystemId().startsWith("file:"));
		assertTrue(resource.getSystemId().endsWith("xsl/unknown.xsl"));
		// assertThrows(XPathException.class, () -> resource.get(request));
	}

	@Test
	public void illegalPathFileEtcPasswd() {
		ResourceRequest request = new ResourceRequest();
		request.uri = "file:/etc/passwd";
		assertThrows(XPathException.class, () -> resolver.resolve(request));
	}

	@Test
	public void illegalPathEtcPasswd() {
		ResourceRequest request = new ResourceRequest();
		request.uri = "/etc/passwd";
		assertThrows(XPathException.class, () -> resolver.resolve(request));
	}

	@Test
	public void delegateHttpRequest() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = "http://example.com/some/transform.xsl";
		assertNull(resolver.resolve(request));
	}

	@Test
	public void delegateHttpsRequest() throws XPathException {
		ResourceRequest request = new ResourceRequest();
		request.uri = "https://example.com/some/transform.xsl";
		assertNull(resolver.resolve(request));
	}

	@Test
	public void illegalPathHelloXML() {
		ResourceRequest request = new ResourceRequest();
		request.uri = HELLO_XML.getAbsolutePath().toString();
		assertThrows(XPathException.class, () -> resolver.resolve(request));
	}

	@Test
	public void normalizationWorksForHelloXMLViaDots() {
		ResourceRequest request = new ResourceRequest();
		request.uri = HELLO_XML_VIA_DOTS.getAbsolutePath().toString();
		assertThrows(XPathException.class, () -> resolver.resolve(request));
	}
}
