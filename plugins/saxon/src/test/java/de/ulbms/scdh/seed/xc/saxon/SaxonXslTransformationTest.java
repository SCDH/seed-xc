package de.ulbms.scdh.seed.xc.saxon;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider;
import de.ulbms.scdh.seed.xc.saxon.harden.RestrictiveResourceResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.RestrictiveUnparsedTextResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.ServiceConfiguration;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.WebApplicationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SaxonXslTransformationTest {

	private static final Processor SAXON_PROCESSOR = new Processor(false);

	private static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	private static final File CONFIG_FILE =
			Paths.get("src", "test", "resources", "xml-transformer-config.yaml").toFile();

	private static final String ID_XSL_REL = Paths.get("xsl", "id.xsl").toFile().toString();

	private static final String IMPORTING_XSL =
			Paths.get("xsl", "importing.xsl").toFile().toString();

	private static final String IMPORTING_ILLEGAL_XSL =
			Paths.get("xsl", "importing-illegal.xsl").toFile().toString();

	private static final String PARAM_INTEGER_XSL =
			Paths.get("xsl", "param-integer.xsl").toFile().toString();

	private static final String PARAM_INTEGER_STATIC_XSL =
			Paths.get("xsl", "param-integer-static.xsl").toFile().toString();

	private static final String UNPARSED_TEXT_XSL =
			Paths.get("xsl", "unparsed-text.xsl").toFile().toString();

	private static final String READ_DOC_XSL =
			Paths.get("xsl", "read-doc.xsl").toFile().toString();

	private static final String RESULT_DOC_XSL =
			Paths.get("xsl", "resultdoc-file.xsl").toFile().toString();

	private static final String TERMINATE_404_XSL =
			Paths.get("xsl", "terminate-404.xsl").toFile().toString();

	private static final String ASSERT_400_XSL =
			Paths.get("xsl", "assert-400.xsl").toFile().toString();

	private static final String ASSERT_404_XSL =
			Paths.get("xsl", "assert-404.xsl").toFile().toString();

	public static final String TIMES_3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<times>\n   <once "
			+ "n=\"3\"/>\n   <once n=\"2\"/>\n   <once n=\"1\"/>\n   <once "
			+ "n=\"0\"/>\n</times>\n";

	private final File helloXml =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private final File unparsedEntityXml = Paths.get("src", "test", "resources", "samples", "unparsed-entity.xml")
			.toFile();

	private final File UNPARSED_ENTITY_XML = Paths.get("src", "test", "resources", "samples", "unparsed-entity.xml")
			.toFile();

	private final File tagSoup1 =
			Paths.get("src", "test", "resources", "samples", "tagsoup1.html").toFile();

	private final File includeXml =
			Paths.get("src", "test", "resources", "samples", "include.xml").toFile();

	private byte[] output;

	private final ServiceConfiguration SERVICE_CONFIG = new TestingConfiguration();

	SaxonXslTransformation transformation;

	private String outputToString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	ResourceProvider resourceProvider;

	@BeforeEach
	public void setup() throws ConfigurationException {
		// set up a new transformation
		transformation = new SaxonXslTransformation();
		transformation.processor = SAXON_PROCESSOR;
		transformation.serviceConfig = SERVICE_CONFIG;
		resourceProvider = new FileSystemResourceProvider(XSL_DIR.getAbsolutePath());

		final RestrictiveResourceResolver FILE_RESOURCE_RESOLVER =
				new RestrictiveResourceResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
		final UnparsedTextURIResolver UNPARSED_TEXT_RESOLVER =
				new RestrictiveUnparsedTextResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
		final TransformationExceptionParser EXCEPTION_PARSER = new XslTransformationExceptionParser(true);

		transformation.compileTimeResourceResolver = FILE_RESOURCE_RESOLVER;
		// transformation.documentResourceResolver = DOCUMENT_RESOURCE_RESOLVER;
		transformation.staticAssetsUnparsedTextURIResolver = UNPARSED_TEXT_RESOLVER;
		transformation.transformationExceptionParser = EXCEPTION_PARSER;

		transformation.processor.setConfigurationProperty(Feature.XSLT_ENABLE_ASSERTIONS, true);
	}

	public static final TransformationInfo ID_CONFIG_ABS;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(new File(XSL_DIR, "id.xsl").getAbsolutePath());
		ID_CONFIG_ABS = info;
	}

	public static final TransformationInfo ID_CONFIG_REL;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(ID_XSL_REL);
		ID_CONFIG_REL = info;
	}

	public static final TransformationInfo IMPORTING_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(IMPORTING_XSL);
		IMPORTING_CONFIG = info;
	}

	public static final TransformationInfo IMPORTING_ILLEGAL_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(IMPORTING_ILLEGAL_XSL);
		IMPORTING_ILLEGAL_CONFIG = info;
	}

	public static final TransformationInfo UNPARSED_TEXT_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(UNPARSED_TEXT_XSL);
		UNPARSED_TEXT_CONFIG = info;
	}

	public static final TransformationInfo READ_DOC_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(READ_DOC_XSL);
		READ_DOC_CONFIG = info;
	}

	public static final TransformationInfo RESULT_DOC_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(RESULT_DOC_XSL);
		RESULT_DOC_CONFIG = info;
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

	public static final TransformationInfo PARAM_INTEGER_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(false);
		info.setLocation(PARAM_INTEGER_XSL);
		PARAM_INTEGER_CONFIG = info;
	}

	public static final RuntimeParameters PARAM_INTEGER_PARAMS;

	static {
		RuntimeParameters params = new RuntimeParameters();
		// Map<String, String> globalParams = new HashMap<String, String>();
		// globalParams.put("times", "3");
		params.putGlobalParametersItem("times", "3");
		PARAM_INTEGER_PARAMS = params;
	}

	public static final TransformationInfo PARAM_INTEGER_STATIC_CONFIG;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(false);
		info.setLocation(PARAM_INTEGER_STATIC_XSL);
		TypedParameter p1 = new TypedParameter("times", "3");
		p1.setType("integer");
		List<TypedParameter> ctimeParams = List.of(p1);
		info.setCompileTimeParameters(ctimeParams);
		PARAM_INTEGER_STATIC_CONFIG = info;
	}

	public static final RuntimeParameters PARAM_URI_PARAMS;

	static {
		RuntimeParameters params = new RuntimeParameters();
		params.putGlobalParametersItem("uri", "lego.txt");
		PARAM_URI_PARAMS = params;
	}

	public static final RuntimeParameters PARAM_RESULT_DOC_PARAMS;

	static {
		RuntimeParameters params = new RuntimeParameters();
		// Map<String, String> globalParams = new HashMap<String, String>();
		// globalParams.put("times", "3");
		params.putGlobalParametersItem("output", "hacked.xml");
		PARAM_RESULT_DOC_PARAMS = params;
	}

	@Test
	public void testTransformationInstance() {
		assertInstanceOf(Transformation.class, transformation);
	}

	@Test
	public void testResolveRelativeStylesheet() throws ConfigurationException {
		transformation.setup(ID_CONFIG_REL);
	}

	@Test
	public void testResolveAbsoluteStylesheet() throws ConfigurationException {
		transformation.setup(ID_CONFIG_ABS);
	}

	@Test
	public void testResolveImporting()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(IMPORTING_CONFIG);
		output = transformation.transform(
				null, null, helloXml.toString(), helloXml.toURI().toURL().openStream(), resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" " + "encoding=\"UTF-8\"?><hello><i><b>Hello</b></i></hello>",
				outputToString(output));
	}

	@Test
	public void testResolveImportingIllegal() {
		assertThrows(ConfigurationException.class, () -> transformation.setup(IMPORTING_ILLEGAL_CONFIG));
	}

	@Test
	public void testUnparsedTextSecret() throws IOException, ConfigurationException {
		transformation.setup(UNPARSED_TEXT_CONFIG);
		FileInputStream helloStream = new FileInputStream(helloXml);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(
						new RuntimeParameters(), null, helloXml.toString(), helloStream, resourceProvider));
	}

	@Test
	public void testUnparsedTextLego()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(UNPARSED_TEXT_CONFIG);
		FileInputStream helloStream = new FileInputStream(helloXml);
		output = transformation.transform(PARAM_URI_PARAMS, null, helloXml.toString(), helloStream, resourceProvider);
		assertEquals("Tolle, lege! Tolle, lege!\n", outputToString(output));
	}

	@Disabled("nothing thrown anymore")
	@Test
	public void testReadDocForbidden() throws IOException, ConfigurationException {
		transformation.setup(READ_DOC_CONFIG);
		FileInputStream inputStream = new FileInputStream(helloXml);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(
						new RuntimeParameters(), null, helloXml.toString(), inputStream, resourceProvider));
	}

	@Test
	public void testReadDocAllowed()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(READ_DOC_CONFIG);
		RuntimeParameters params = new RuntimeParameters();
		params.putGlobalParametersItem("uri", "allowed.xml");
		FileInputStream inputStream = new FileInputStream(helloXml);
		output = transformation.transform(params, null, helloXml.toString(), inputStream, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" " + "encoding=\"UTF-8\"?><result><allowed>*<star/>*</" + "allowed></result>",
				outputToString(output));
	}

	@Test
	public void testParamIntegerGetInfo() throws ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		assertInstanceOf(TransformationInfo.class, transformation.getTransformationInfo());
		assertEquals(PARAM_INTEGER_CONFIG, transformation.getTransformationInfo());
	}

	@Test
	public void testParamIntegerGetParameters() throws ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		assertInstanceOf(XsltParameterDetails.class, transformation.getTransformationParameters());
		XsltParameterDetails parameters = transformation.getTransformationParameters();
		assertEquals(1, parameters.size());
		assertEquals("", parameters.get("times").getOccurrenceIndicator());
		assertEquals("xs:integer", parameters.get("times").getItemType());
		assertEquals("xs:integer", parameters.get("times").getUnderlyingDeclaredType());
		assertEquals(true, parameters.get("times").getIsRequired());
	}

	@Test
	public void testTransformParamIntegerTransform3ParamsHelloOutfile()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		output = transformation.transform(
				PARAM_INTEGER_PARAMS,
				null,
				helloXml.toString(),
				helloXml.toURI().toURL().openStream(),
				resourceProvider);
		assertEquals(TIMES_3, outputToString(output));
	}

	@Test
	public void testTransformParamIntegerTransform3ParamsNull() throws ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(
						null,
						null,
						helloXml.toString(),
						helloXml.toURI().toURL().openStream(),
						resourceProvider));
	}

	// TODO: should these NullPointerExceptions be something else?

	@Disabled("old")
	@Test
	public void testTransformParamIntegerTransform3SystemIdNull() throws ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		// assertThrows(TransformationException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null,
		// null));
	}

	// @Test
	// public void testTransformParamIntegerTransform3OutfileNull()
	// 	throws IOException, ConfigurationException,
	// TransformationPreparationException {
	// 	transformation.setup(PARAM_INTEGER_CONFIG);
	// 	assertThrows(NullPointerException.class, () ->
	// transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(),
	// null));
	// }

	@Test
	public void testTransformParamIntegerTransform4ParamsHelloOutfile()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		FileInputStream helloStream = new FileInputStream(helloXml);
		output = transformation.transform(
				PARAM_INTEGER_PARAMS, null, helloXml.toString(), helloStream, resourceProvider);
		assertEquals(TIMES_3, outputToString(output));
	}

	@Test
	public void testTransformParamIntegerTransform4ParamsNull() throws IOException, ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		FileInputStream helloStream = new FileInputStream(helloXml);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(null, null, helloXml.toString(), helloStream, resourceProvider));
	}

	@Test
	public void testTransformParamIntegerTransform4SystemIdNull()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		FileInputStream helloStream = new FileInputStream(helloXml);
		output = transformation.transform(PARAM_INTEGER_PARAMS, null, null, helloStream, resourceProvider);
		assertEquals(TIMES_3, outputToString(output));
	}

	@Test
	public void testTransformParamIntegerTransform4InputStreamNull()
			throws ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		output = transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(), null, resourceProvider);
		assertEquals(TIMES_3, outputToString(output));
	}

	// @Test
	// public void testTransformParamIntegerTransform4OutfileNull()
	// 	throws IOException, ConfigurationException,
	// TransformationPreparationException, TransformationException {
	// 	transformation.setup(PARAM_INTEGER_CONFIG);
	// 	FileInputStream helloStream = new FileInputStream(helloXml);
	// 	assertThrows(NullPointerException.class, () ->
	// transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(),
	// helloStream));
	// }

	@Disabled("old")
	@Test
	public void testTransformParamIntegerTransform4SystemIdNullInputStreamNull() throws ConfigurationException {
		transformation.setup(PARAM_INTEGER_CONFIG);
		// assertThrows(TransformationException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null, null,
		// null, resourceProvider));
	}

	@Test
	public void testTransformParamIntegerStaticTransform()
			throws ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(PARAM_INTEGER_STATIC_CONFIG);
		output = transformation.transform(null, null, helloXml.toString(), null, resourceProvider);
		assertEquals(TIMES_3, outputToString(output));
	}

	// With this test we make sure, that unparsed entities are not
	// expanded automatically by the XSLT processor or the underlying
	// parser. For security reasons, unparsed entities must not be
	// expanded automatically and must not bypass the setup of allowed
	// protocolls in the XSLT processor's configuration.
	@Test
	public void testUnparsedEntityNotExpanded4()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(unparsedEntityXml);
		output = transformation.transform(null, null, unparsedEntityXml.toString(), input, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test " + "source=\"passwd\"/>", outputToString(output));
	}

	@Test
	public void testUnparsedEntityNotExpanded3()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(unparsedEntityXml);
		output = transformation.transform(null, null, unparsedEntityXml.toString(), input, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test " + "source=\"passwd\"/>", outputToString(output));
	}

	@Test
	public void testResultDoc() throws IOException, ConfigurationException {
		transformation.setup(RESULT_DOC_CONFIG);
		FileInputStream input = new FileInputStream(helloXml);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(null, null, helloXml.toString(), input, resourceProvider));
	}

	@Test
	public void testResultDocWithOutput() throws IOException, ConfigurationException {
		transformation.setup(RESULT_DOC_CONFIG);
		FileInputStream input = new FileInputStream(helloXml);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(
						PARAM_RESULT_DOC_PARAMS, null, helloXml.toString(), input, resourceProvider));
	}

	/* exception parser */

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
		Uni<FileInputStream> input = Uni.createFrom().item(new FileInputStream(helloXml));
		UniAssertSubscriber<byte[]> subscriber = input.plug((is) -> {
					return transformation.transformAsync(null, null, helloXml.toString(), input, resourceProvider);
				})
				.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		subscriber.awaitFailure().assertFailedWith(WebApplicationException.class, "not found 405 minus 1");
	}

	@Disabled("Saxon throws an error on compilation!")
	@Test
	public void testErrorAssert400() throws IOException, ConfigurationException {
		transformation.setup(ASSERT_400_CONFIG);
		Uni<FileInputStream> input = Uni.createFrom().item(new FileInputStream(helloXml));
		var e = assertThrows(
				WebApplicationException.class,
				() -> transformation
						.transformAsync(null, null, helloXml.toString(), input, resourceProvider)
						.subscribe());
		assertInstanceOf(WebApplicationException.class, e);
		assertEquals(401, ((WebApplicationException) e).getResponse().getStatus());
		assertEquals("bad request 401 minus 1", ((WebApplicationException) e).getMessage());
	}

	@Test
	public void testErrorAssert404() throws IOException, ConfigurationException {
		transformation.setup(ASSERT_404_CONFIG);
		Uni<FileInputStream> input = Uni.createFrom().item(new FileInputStream(helloXml));
		UniAssertSubscriber<byte[]> subscriber = input.plug((is) -> {
					return transformation.transformAsync(null, null, helloXml.toString(), input, resourceProvider);
				})
				.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		subscriber.awaitFailure().assertFailedWith(WebApplicationException.class, "not found 405 minus 1");
	}

	/* source parsers */

	public static final Config NU_VALIDATOR_CONFIG;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("nu.validator.htmlparser.sax.HtmlParser");
		cfg.setParser(parser);
		NU_VALIDATOR_CONFIG = cfg;
	}

	public static final Config COWAN_TAGSOUP_CONFIG;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("org.ccil.cowan.tagsoup.Parser");
		cfg.setParser(parser);
		COWAN_TAGSOUP_CONFIG = cfg;
	}

	public static final Config UNKNOWN_PARSER_CONFIG;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("un.known.tagsoup.Reader");
		cfg.setParser(parser);
		UNKNOWN_PARSER_CONFIG = cfg;
	}

	public static final Config BAD_PARSER_CONFIG;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("java.lang.String");
		cfg.setParser(parser);
		BAD_PARSER_CONFIG = cfg;
	}

	@Test
	public void testTagSoup1WithDefaultParser() throws IOException, ConfigurationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(null, null, unparsedEntityXml.toString(), input, resourceProvider));
	}

	@Test
	public void testTagSoup1WithNuValidator()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		output = transformation.transform(
				null, NU_VALIDATOR_CONFIG, unparsedEntityXml.toString(), input, resourceProvider);
		assertTrue(outputToString(output)
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html "
						+ "xmlns=\"http://www.w3.org/1999/xhtml\">"));
	}

	@Test
	public void testTagSoup1WithCowanTagSoup()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		output = transformation.transform(
				null, COWAN_TAGSOUP_CONFIG, unparsedEntityXml.toString(), input, resourceProvider);
		// assertEquals("", outputToString(output));
		assertTrue(outputToString(output)
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html "
						+ "xmlns=\"http://www.w3.org/1999/xhtml\" "
						+ "xmlns:html=\"http://www.w3.org/1999/xhtml\">"));
	}

	@Test
	public void testTagSoup1WithUnknownParser() throws IOException, ConfigurationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		assertThrows(
				TransformationPreparationException.class,
				() -> transformation.transform(
						null, UNKNOWN_PARSER_CONFIG, unparsedEntityXml.toString(), input, resourceProvider));
	}

	@Test
	public void testTagSoup1WithBadParser() throws IOException, ConfigurationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		assertThrows(
				TransformationPreparationException.class,
				() -> transformation.transform(
						null, BAD_PARSER_CONFIG, unparsedEntityXml.toString(), input, resourceProvider));
	}

	public static final TransformationInfo ID_CONFIG_ABS_TAGSOUP_PARSER;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
		info.setRequiresSource(true);
		info.setLocation(new File(XSL_DIR, "id.xsl").getAbsolutePath());
		Parser parser = new Parser();
		parser.setPropertyClass("org.ccil.cowan.tagsoup.Parser");
		info.setParser(parser);
		ID_CONFIG_ABS_TAGSOUP_PARSER = info;
	}

	@Test
	public void testTagSoup1WithTransformationParser()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS_TAGSOUP_PARSER);
		FileInputStream input = new FileInputStream(tagSoup1);
		output = transformation.transform(null, null, unparsedEntityXml.toString(), input, resourceProvider);
		// assertEquals("", outputToString(output));
		assertTrue(outputToString(output)
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html "
						+ "xmlns=\"http://www.w3.org/1999/xhtml\" "
						+ "xmlns:html=\"http://www.w3.org/1999/xhtml\">"));
	}

	public static final Config CONFIG_WITH_XINCLUDE;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setXinclude(true);
		cfg.setParser(parser);
		CONFIG_WITH_XINCLUDE = cfg;
	}

	public static final Config CONFIG_WITH_XINCLUDE_FALSE;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setXinclude(false);
		cfg.setParser(parser);
		CONFIG_WITH_XINCLUDE_FALSE = cfg;
	}

	public static final Config NU_VALIDATOR_CONFIG_WITH_XINCLUDE;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("nu.validator.htmlparser.sax.HtmlParser");
		parser.setXinclude(true);
		cfg.setParser(parser);
		NU_VALIDATOR_CONFIG_WITH_XINCLUDE = cfg;
	}

	@Test
	public void testXInclude()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(includeXml);
		output = transformation.transform(null, CONFIG_WITH_XINCLUDE, includeXml.toString(), input, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper "
						+ "xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    <hello "
						+ "xml:base=\"hello.xml\"><greating>Hello</greating></"
						+ "hello>\n    "
						+ "<hello "
						+ "xml:base=\"hello.xml\"><greating>Hello</greating></"
						+ "hello>\n</wrapper>",
				outputToString(output));
	}

	@Test
	public void testXIncludeFalse()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(includeXml);
		output = transformation.transform(
				null, CONFIG_WITH_XINCLUDE_FALSE, includeXml.toString(), input, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper "
						+ "xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    "
						+ "<xi:include href=\"hello.xml\"/>\n    <xi:include "
						+ "href=\"hello.xml\"/>\n</wrapper>",
				outputToString(output));
	}

	@Test
	public void testXIncludeNotSet()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(includeXml);
		output = transformation.transform(null, null, includeXml.toString(), input, resourceProvider);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper "
						+ "xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    "
						+ "<xi:include href=\"hello.xml\"/>\n    <xi:include "
						+ "href=\"hello.xml\"/>\n</wrapper>",
				outputToString(output));
	}

	@Test
	public void testTagSoup1WithNuValidatorWithXInclude()
			throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
		transformation.setup(ID_CONFIG_ABS);
		FileInputStream input = new FileInputStream(tagSoup1);
		output = transformation.transform(
				null, NU_VALIDATOR_CONFIG_WITH_XINCLUDE, tagSoup1.toString(), input, resourceProvider);
		assertTrue(outputToString(output)
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html "
						+ "xmlns=\"http://www.w3.org/1999/xhtml\">"));
	}

	@Test
	public void testExportId() throws ConfigurationException {
		transformation.setup(ID_CONFIG_REL);
		assertThrows(UnsupportedOperationException.class, () -> transformation.export());
	}

	@Test
	public void testExportIdForJS() throws ConfigurationException {
		transformation.setup(ID_CONFIG_REL);
		assertThrows(UnsupportedOperationException.class, () -> transformation.export("JS"));
	}
}
