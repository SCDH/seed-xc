package de.ulbms.scdh.seed.xc.saxon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider;
import de.ulbms.scdh.seed.xc.saxon.harden.RestrictiveResourceResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.RestrictiveUnparsedTextResolver;
import io.vertx.core.http.HttpServerRequest;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SaxonXQueryTransformationTest {

	private static final File BASE_DIR = Paths.get("").toFile();

	private static final File XQL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File CONFIG_FILE =
			Paths.get("src", "test", "resources", "xml-transformer-config.yaml").toFile();

	private final File helloXml =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	ResourceProvider resourceProvider;

	HttpServerRequest request = null;

	SaxonXQueryTransformation transformation;

	private byte[] output;

	private String outputToString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@BeforeEach
	void setup() throws ConfigurationException {
		transformation = new SaxonXQueryTransformation();
		transformation.processor = new Processor(false);
		resourceProvider = new FileSystemResourceProvider(XQL_DIR.getAbsolutePath());

		final RestrictiveResourceResolver FILE_RESOURCE_RESOLVER =
				new RestrictiveResourceResolver(XQL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
		final UnparsedTextURIResolver UNPARSED_TEXT_RESOLVER =
				new RestrictiveUnparsedTextResolver(XQL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
		final TransformationExceptionParser EXCEPTION_PARSER = new XslTransformationExceptionParser(true);

		transformation.compileTimeResourceResolver = FILE_RESOURCE_RESOLVER;
		// transformation.documentResourceResolver = DOCUMENT_RESOURCE_RESOLVER;
		transformation.staticAssetsUnparsedTextURIResolver = UNPARSED_TEXT_RESOLVER;
		transformation.transformationExceptionParser = EXCEPTION_PARSER;
	}

	public static final TransformationInfo TITLE_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXQueryTransformation.TRANSFORMATION_TYPE);
		info.setLocation(new File(XQL_DIR, "title.xql").getAbsolutePath());
		info.setRequiresSource(false);
		TITLE_CONFIG = info;
	}

	public static final TransformationInfo TITLE_WITH_SOURCE_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXQueryTransformation.TRANSFORMATION_TYPE);
		info.setLocation(new File(XQL_DIR, "title.xql").getAbsolutePath());
		info.setRequiresSource(true);
		TITLE_WITH_SOURCE_CONFIG = info;
	}

	public static final RuntimeParameters HELLO_RESOURCES;

	static {
		RuntimeParameters params = new RuntimeParameters();
		params.putGlobalParametersItem("resources", "../samples/?select=hell*");
		HELLO_RESOURCES = params;
	}

	@Test
	public void testTitlesWithoutGlobalContext()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					MalformedURLException, IOException {
		transformation.setup(TITLE_CONFIG, BASE_DIR);
		output = transformation.transform(
				null,
				null,
				helloXml.getAbsolutePath(),
				helloXml.toURI().toURL().openStream(),
				resourceProvider,
				request);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><titles xmlns=\"http://www.tei-c.org/ns/1.0\"><title>Books</title><title>Didascalicon</title><title>Etymologiae</title></titles>",
				outputToString(output));
	}

	@Test
	public void testTitlesWithGlobalContext()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					MalformedURLException, IOException {
		transformation.setup(TITLE_WITH_SOURCE_CONFIG, BASE_DIR);
		output = transformation.transform(
				null,
				null,
				helloXml.getAbsolutePath(),
				helloXml.toURI().toURL().openStream(),
				resourceProvider,
				request);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><titles xmlns=\"http://www.tei-c.org/ns/1.0\"><title>Books</title><title>Didascalicon</title><title>Etymologiae</title></titles>",
				outputToString(output));
	}

	@Test
	public void testTitlesFromHelloWithoutGlobalContext()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					MalformedURLException, IOException {
		transformation.setup(TITLE_CONFIG, BASE_DIR);
		output = transformation.transform(
				HELLO_RESOURCES,
				null,
				helloXml.getAbsolutePath(),
				helloXml.toURI().toURL().openStream(),
				resourceProvider,
				request);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><titles xmlns=\"http://www.tei-c.org/ns/1.0\"/>",
				outputToString(output));
	}

	@Test
	public void testTitlesFromHelloWithGlobalContext()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					MalformedURLException, IOException {
		transformation.setup(TITLE_WITH_SOURCE_CONFIG, BASE_DIR);
		output = transformation.transform(
				HELLO_RESOURCES,
				null,
				helloXml.getAbsolutePath(),
				helloXml.toURI().toURL().openStream(),
				resourceProvider,
				request);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><titles xmlns=\"http://www.tei-c.org/ns/1.0\"/>",
				outputToString(output));
	}
}
