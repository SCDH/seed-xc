package de.ulbms.scdh.seed.xc.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.harden.*;
import de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider;
import jakarta.ws.rs.WebApplicationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class XslTransformationExceptionParserTest {

	private final TransformationExceptionParser EXCEPTION_PARSER = new XslTransformationExceptionParser(true);

	/* isolated tests */

	@Test
	public void testCodeTransformationException() {
		assertEquals(500, EXCEPTION_PARSER.parseCode(new TransformationException("here")));
	}

	@Test
	public void testCodeSaxonApiException() {
		assertEquals(500, EXCEPTION_PARSER.parseCode(new TransformationException(new SaxonApiException("here"))));
	}

	@Test
	public void testCodeXPathException() {
		assertEquals(500, EXCEPTION_PARSER.parseCode(new TransformationException(new XPathException("here"))));
	}

	@Test
	public void testCodeSaxonApiExceptionXPathException() {
		assertEquals(
				500,
				EXCEPTION_PARSER.parseCode(
						new TransformationException(new SaxonApiException(new XPathException("here")))));
	}

	@Test
	public void testCodeSaxonApiExceptionException() {
		assertEquals(500, EXCEPTION_PARSER.parseCode(new TransformationException(new Exception("here"))));
	}

	@Test
	public void testCodeNull() {
		assertEquals(500, EXCEPTION_PARSER.parseCode(null));
	}

	@Test
	public void testMsgTransformationException() {
		assertEquals("here", EXCEPTION_PARSER.message(new TransformationException("here")));
	}

	@Test
	public void testMsgSaxonApiException() {
		assertEquals("here", EXCEPTION_PARSER.message(new TransformationException(new SaxonApiException("here"))));
	}

	@Test
	public void testMsgXPathException() {
		assertEquals("here", EXCEPTION_PARSER.message(new TransformationException(new XPathException("here"))));
	}

	@Test
	public void testMsgSaxonApiExceptionXPathException() {
		assertEquals(
				"here",
				EXCEPTION_PARSER.message(
						new TransformationException(new SaxonApiException(new XPathException("here")))));
	}

	@Test
	public void testMsgNull() {
		assertEquals("null", EXCEPTION_PARSER.message(null));
	}

	/* Tests with instance injected */

	private static final Processor SAXON_PROCESSOR = new Processor(false);

	private static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File CONFIG_FILE =
			Paths.get("src", "test", "resources", "xml-transformer-config.yaml").toFile();

	private static final String TERMINATE_404_XSL =
			Paths.get("xsl", "terminate-404.xsl").toFile().toString();

	private static final String ASSERT_400_XSL =
			Paths.get("xsl", "assert-400.xsl").toFile().toString();

	private static final String ASSERT_404_XSL =
			Paths.get("xsl", "assert-404.xsl").toFile().toString();

	private final File helloXml =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private byte[] output;
	private final ServiceConfiguration SERVICE_CONFIG = new TestingConfiguration();

	SaxonXslTransformation transformation;

	private String outputToString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	ResourceProvider resourceProvider;

	@BeforeEach
	public void setup() throws IOException, ConfigurationException {
		// set up a new transformation
		transformation = new SaxonXslTransformation();
		transformation.processor = SAXON_PROCESSOR;
		transformation.serviceConfig = SERVICE_CONFIG;
		resourceProvider = new FileSystemResourceProvider(XSL_DIR.getAbsolutePath());

		final FileURIResolver FILE_RESOURCE_RESOLVER =
				new FileURIResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
		final RestrictiveFileOnlyResolver XSLT_RESOURCE_RESOLVER =
				new RestrictiveFileOnlyResolver(FILE_RESOURCE_RESOLVER);
		final RestrictiveResourceResolver DOCUMENT_RESOURCE_RESOLVER =
				new RestrictiveResourceResolver(FILE_RESOURCE_RESOLVER, SAXON_PROCESSOR);
		final UnparsedTextURIResolver UNPARSED_TEXT_RESOLVER =
				new RestrictiveUnparsedTextResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());

		transformation.compileTimeResourceResolver = XSLT_RESOURCE_RESOLVER;
		transformation.staticAssetsUnparsedTextURIResolver = UNPARSED_TEXT_RESOLVER;
		transformation.transformationExceptionParser = EXCEPTION_PARSER;

		transformation.processor.setConfigurationProperty(Feature.XSLT_ENABLE_ASSERTIONS, true);
	}

	public static final TransformationInfo TERMINATE_404_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(TERMINATE_404_XSL);
		TERMINATE_404_CONFIG = info;
	}

	public static final TransformationInfo ASSERT_400_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(ASSERT_400_XSL);
		ASSERT_400_CONFIG = info;
	}

	public static final TransformationInfo ASSERT_404_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(ASSERT_404_XSL);
		ASSERT_404_CONFIG = info;
	}

	@Test
	public void testExceptionTerminate404() throws IOException, ConfigurationException {
		transformation.setup(TERMINATE_404_CONFIG);
		FileInputStream input = new FileInputStream(helloXml);
		var e = assertThrows(
				TransformationException.class,
				() -> transformation.transform(null, null, helloXml.toString(), input, resourceProvider));
		assertInstanceOf(SaxonApiException.class, e.getCause());
		assertEquals(
				"XTMM9000", ((SaxonApiException) e.getCause()).getErrorCode().toString());
	}

	@Test
	public void testErrorTerminate404HasCode() throws IOException, ConfigurationException {
		transformation.setup(TERMINATE_404_CONFIG);
		FileInputStream input = new FileInputStream(helloXml);
		var e = assertThrows(
				WebApplicationException.class,
				() -> transformation.transformF(null, null, helloXml.toString(), input, resourceProvider));
		assertInstanceOf(WebApplicationException.class, e);
		assertEquals(405, ((WebApplicationException) e).getResponse().getStatus());
		assertEquals("not found 405 minus 1", ((WebApplicationException) e).getMessage());
	}

	@Disabled("Saxon throws an error on compilation!")
	@Test
	public void testErrorAssert400() throws IOException, ConfigurationException {
		transformation.setup(ASSERT_400_CONFIG);
		FileInputStream input = new FileInputStream(helloXml);
		var e = assertThrows(
				WebApplicationException.class,
				() -> transformation.transformF(null, null, helloXml.toString(), input, resourceProvider));
		assertInstanceOf(WebApplicationException.class, e);
		assertEquals(401, ((WebApplicationException) e).getResponse().getStatus());
		assertEquals("bad request 401 minus 1", ((WebApplicationException) e).getMessage());
	}
}
