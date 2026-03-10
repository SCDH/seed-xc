package de.ulbms.scdh.seed.xc.harden;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.transform.Source;
import java.util.zip.ZipFile;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Iterator;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;

import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.trans.XPathException;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;


public class ZipFileURIResolverTest {

    private static final File XSL_ZIP = Paths.get("src", "test", "resources", "xsl.zip").toFile();

    private ZipFile zipFile;

    ZipFileURIResolver resolver;

    @BeforeEach
	public void setupResolver() throws ConfigurationException, IOException {
	resolver = new ZipFileURIResolver();
	zipFile = new ZipFile(XSL_ZIP);

	// // diagnostics: What's in the zip?
	// System.out.println(zipFile.getName());
	// Iterator<? extends ZipEntry> entries = zipFile.entries().asIterator();
	// while (entries.hasNext()) {
	//     ZipEntry entry = entries.next();
	//     System.out.println(entry.getName());
	// }
    }

    @Test
    public void nullZip() {
	resolver = new ZipFileURIResolver();
	assertThrows(ConfigurationException.class, () -> resolver.setup(null, null));
    }

    @Test
    public void rootBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	// We cannot resolve against / base URI
	resolver.setNonDelegating();
	resolver.setup(zipFile, new URI("/"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void rootBaseUriDelegating() throws ConfigurationException, XPathException, URISyntaxException {
	// We cannot resolve against / base URI
	resolver.setup(zipFile, new URI("/"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	assertNull(resolver.resolve(request));
    }

    @Test
    public void nullBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	// Resolving against null base URI works!
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	Source resource = resolver.resolve(request);
	assertTrue(resource.getSystemId().startsWith("file:"));
	assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
    }

    @Test
    public void nullBaseUriWithFile() throws ConfigurationException, XPathException, URISyntaxException {
	// Resolving against null base URI works!
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "file:xsl/id.xsl";
	Source resource = resolver.resolve(request);
	assertTrue(resource.getSystemId().startsWith("file:"));
	assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
	assertEquals("file:xsl/id.xsl", resource.getSystemId());
    }

    @Test
    public void fullSystemId() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "file:xsl/id.xsl";
	Source resource = resolver.resolve(request);
	assertEquals("file:xsl/id.xsl", resource.getSystemId()); // only asserting this once!
    }

    @Test
    public void singleSegmentBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("xsl"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	Source resource = resolver.resolve(request);
	assertTrue(resource.getSystemId().startsWith("file:"));
	assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
    }

    @Test
    public void singleSegmentWithTrailingSlashBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("xsl/"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "id.xsl";
	Source resource = resolver.resolve(request);
	assertTrue(resource.getSystemId().startsWith("file:"));
	assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
    }

    @Test
    public void singleSegmentWithLeadingSlashBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("/xsl"));
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
	request.uri = "/xsl/id.xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
	request.uri = "id.xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void singleSegmentWithLeadingSlashBaseUriDelegating() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("/xsl"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/id.xsl";
	assertNull(resolver.resolve(request));
	request.uri = "/xsl/id.xsl";
	assertNull(resolver.resolve(request));
	request.uri = "id.xsl";
	assertNull(resolver.resolve(request));
    }

    @Test
    public void deepPathBaseUri() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("xsl/id.xsl"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "id.xsl";
	Source resource = resolver.resolve(request);
	assertTrue(resource.getSystemId().startsWith("file:"));
	assertTrue(resource.getSystemId().endsWith("xsl/id.xsl"));
    }

    @Test
    public void resolveDots() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, new URI("xsl/id.xsl"));
	ResourceRequest request = new ResourceRequest();
	request.uri = "../samples/hello.xml";
	Source resource = resolver.resolve(request);
	assertEquals("file:samples/hello.xml", resource.getSystemId());
    }

    @Test
    public void unknownFile() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/unknown.xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void unknownFileDelegating() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl/unknown.xsl";
	assertNull(resolver.resolve(request));
    }

    @Test
    public void directoryPath() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void directoryPathDelegating() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "xsl";
	assertNull(resolver.resolve(request));
    }

    @Test
    @Disabled
    public void nonXmlFile() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "samples/secret.txt";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    @Disabled
    public void nonXmlFileDelegating() throws ConfigurationException, XPathException, URISyntaxException {
	resolver.setup(zipFile, null);
	ResourceRequest request = new ResourceRequest();
	request.uri = "samples/secret.txt";
	assertNull(resolver.resolve(request));
    }

    @Test
    public void illegalPathFileEtcPasswd() {
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "file:/etc/passwd";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void illegalPathFileEtcPasswdDelegating() throws XPathException {
	ResourceRequest request = new ResourceRequest();
	request.uri = "file:/etc/passwd";
	assertNull(resolver.resolve(request));
    }

    @Test
    public void illegalPathEtcPasswd() {
	resolver.setNonDelegating();
	ResourceRequest request = new ResourceRequest();
	request.uri = "/etc/passwd";
	assertThrows(XPathException.class, () -> resolver.resolve(request));
    }

    @Test
    public void illegalPathEtcPasswdDelegating() throws XPathException {
	ResourceRequest request = new ResourceRequest();
	request.uri = "/etc/passwd";
	assertNull(resolver.resolve(request));
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

}
