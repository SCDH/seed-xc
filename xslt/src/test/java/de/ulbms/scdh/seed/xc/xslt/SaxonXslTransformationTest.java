package de.ulbms.scdh.seed.xc.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.Parser;
import de.ulbms.scdh.seed.xc.api.TypedParameter;
import de.ulbms.scdh.seed.xc.harden.DenyingResourceResolver;
import de.ulbms.scdh.seed.xc.harden.FileURIResolver;
import de.ulbms.scdh.seed.xc.harden.RestrictiveFileOnlyResolver;
import de.ulbms.scdh.seed.xc.harden.RestrictiveResourceResolver;
import de.ulbms.scdh.seed.xc.harden.RestrictiveUnparsedTextResolver;

public class SaxonXslTransformationTest {

    private static final Processor SAXON_PROCESSOR = new Processor(false);

    private static final File XSL_DIR = Paths.get("src", "test", "resources", "xsl").toFile();

    private static final File CONFIG_FILE = Paths.get("src", "test", "resources", "xml-transformer-config.yaml").toFile();

    private static final String ID_XSL_REL = Paths.get("xsl", "id.xsl").toFile().toString();

    private static final String IMPORTING_XSL = Paths.get("xsl", "importing.xsl").toFile().toString();

    private static final String IMPORTING_ILLEGAL_XSL = Paths.get("xsl", "importing-illegal.xsl").toFile().toString();

    private static final String PARAM_INTEGER_XSL = Paths.get("xsl", "param-integer.xsl").toFile().toString();

    private static final String PARAM_INTEGER_STATIC_XSL = Paths.get("xsl", "param-integer-static.xsl").toFile().toString();

    private static final String UNPARSED_TEXT_XSL = Paths.get("xsl", "unparsed-text.xsl").toFile().toString();

    private static final String READ_DOC_XSL = Paths.get("xsl", "read-doc.xsl").toFile().toString();

    public static final String TIMES_3 =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<times>\n   <once n=\"3\"/>\n   <once n=\"2\"/>\n   <once n=\"1\"/>\n   <once n=\"0\"/>\n</times>\n";

    private final File helloXml = Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

    private final File unparsedEntityXml = Paths.get("src", "test", "resources", "samples", "unparsed-entity.xml").toFile();

    private final File UNPARSED_ENTITY_XML = Paths.get("src", "test", "resources", "samples", "unparsed-entity.xml").toFile();

    private final File tagSoup1 = Paths.get("src", "test", "resources", "samples", "tagsoup1.html").toFile();

    private final File includeXml = Paths.get("src", "test", "resources", "samples", "include.xml").toFile();

    private byte[] output;

    private final ServiceConfiguration SERVICE_CONFIG = new TestingConfiguration();

    SaxonXslTransformation transformation;

    private String outputToString(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
    }

    @BeforeEach
    public void setup() throws IOException, ConfigurationException {
    // setup a new transformation
    transformation = new SaxonXslTransformation();
    transformation.processor = SAXON_PROCESSOR;
    transformation.serviceConfig = SERVICE_CONFIG;

    final FileURIResolver FILE_RESOURCE_RESOLVER = new FileURIResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());
    final RestrictiveFileOnlyResolver XSLT_RESOURCE_RESOLVER =new RestrictiveFileOnlyResolver(FILE_RESOURCE_RESOLVER);
    final RestrictiveResourceResolver DOCUMENT_RESOURCE_RESOLVER = new RestrictiveResourceResolver(FILE_RESOURCE_RESOLVER, SAXON_PROCESSOR);
    final UnparsedTextURIResolver UNPARSED_TEXT_RESOLVER = new RestrictiveUnparsedTextResolver(XSL_DIR.getAbsolutePath(), CONFIG_FILE.getAbsolutePath());

    transformation.xsltResourceResolver = XSLT_RESOURCE_RESOLVER;
    transformation.documentResourceResolver = DOCUMENT_RESOURCE_RESOLVER;
    transformation.unparsedTextURIResolver = UNPARSED_TEXT_RESOLVER;
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
    IMPORTING_CONFIG= info;
    }

    public static final TransformationInfo IMPORTING_ILLEGAL_CONFIG;
    static {
    TransformationInfo info = new TransformationInfo();
    info.setPropertyClass(SaxonXslTransformation.TRANSFORMATION_TYPE);
    info.setRequiresSource(true);
    info.setLocation(IMPORTING_ILLEGAL_XSL);
    IMPORTING_ILLEGAL_CONFIG= info;
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
    List<TypedParameter> ctimeParams = List.of(p1);;
    info.setCompileTimeParameters(ctimeParams);
    PARAM_INTEGER_STATIC_CONFIG = info;
    }

    public static final RuntimeParameters PARAM_URI_PARAMS;
    static {
    RuntimeParameters params = new RuntimeParameters();
    params.putGlobalParametersItem("uri", "lego.txt");
    PARAM_URI_PARAMS = params;
    }


    @Test
    public void testTransformationInstance() throws ConfigurationException {
    assertTrue(transformation instanceof Transformation);
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
    public void testResolveImporting() throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(IMPORTING_CONFIG);
    output = transformation.transform(null, null, helloXml.toString());
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello><i><b>Hello</b></i></hello>", outputToString(output));
    }

    @Test
    public void testResolveImportingIllegal() {
    assertThrows(ConfigurationException.class, () -> transformation.setup(IMPORTING_ILLEGAL_CONFIG));
    }

    @Test
    public void testUnparsedTextSecret()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(UNPARSED_TEXT_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    assertThrows(TransformationException.class,
             () -> transformation.transform(new RuntimeParameters(), null, helloXml.toString(), helloStream));
    }

    @Test
    public void testUnparsedTextLego()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(UNPARSED_TEXT_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    output = transformation.transform(PARAM_URI_PARAMS, null, helloXml.toString(), helloStream);
    assertEquals("Tolle, lege! Tolle, lege!\n", outputToString(output));
    }

    @Test
    public void testReadDocForbidden()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(READ_DOC_CONFIG);
    FileInputStream inputStream = new FileInputStream(helloXml);
    assertThrows(TransformationException.class,
             () -> transformation.transform(new RuntimeParameters(), null, helloXml.toString(), inputStream));
    }

    @Test
    public void testReadDocAllowed()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(READ_DOC_CONFIG);
    RuntimeParameters params = new RuntimeParameters();
    params.putGlobalParametersItem("uri", "allowed.xml");
    FileInputStream inputStream = new FileInputStream(helloXml);
    output = transformation.transform(params, null, helloXml.toString(), inputStream);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><result><allowed>*<star/>*</allowed></result>", outputToString(output));
    }


    @Test
    public void testParamIntegerGetInfo() throws ConfigurationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    assertTrue(transformation.getTransformationInfo() instanceof TransformationInfo);
    assertTrue(transformation.getTransformationInfo().equals(PARAM_INTEGER_CONFIG));
    }

    @Test
    public void testParamIntegerGetParameters() throws ConfigurationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    assertTrue(transformation.getTransformationParameters() instanceof XsltParameterDetails);
    XsltParameterDetails parameters = transformation.getTransformationParameters();
    assertEquals(parameters.size(), 1);
    assertEquals(parameters.get("times").getOccurrenceIndicator(), "");
    assertEquals(parameters.get("times").getItemType(), "xs:integer");
    assertEquals(parameters.get("times").getUnderlyingDeclaredType(), "xs:integer");
    assertEquals(parameters.get("times").getIsRequired(), true);
    }

    @Test
    public void testTransformParamIntegerTransform3ParamsHelloOutfile()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    output = transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString());
    assertEquals(TIMES_3, outputToString(output));
    }

    @Test
    public void testTransformParamIntegerTransform3ParamsNull()
    throws IOException, ConfigurationException, TransformationPreparationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    assertThrows(TransformationException.class, () -> transformation.transform(null, null, helloXml.toString()));
    }

    // TODO: should these NullPointerExceptions be something else?

    @Test
    public void testTransformParamIntegerTransform3SystemIdNull()
    throws IOException, ConfigurationException, TransformationPreparationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    assertThrows(TransformationException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null, null));
    }

    // @Test
    // public void testTransformParamIntegerTransform3OutfileNull()
    //     throws IOException, ConfigurationException, TransformationPreparationException {
    //     transformation.setup(PARAM_INTEGER_CONFIG);
    //     assertThrows(NullPointerException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(), null));
    // }


    @Test
    public void testTransformParamIntegerTransform4ParamsHelloOutfile()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    output = transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(), helloStream);
    assertEquals(TIMES_3, outputToString(output));
    }

    @Test
    public void testTransformParamIntegerTransform4ParamsNull()
    throws IOException, ConfigurationException, TransformationPreparationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    assertThrows(TransformationException.class, () -> transformation.transform(null, null, helloXml.toString(), helloStream));
    }

    @Test
    public void testTransformParamIntegerTransform4SystemIdNull()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    output = transformation.transform(PARAM_INTEGER_PARAMS, null, null, helloStream);
    assertEquals(TIMES_3, outputToString(output));
    }

    @Test
    public void testTransformParamIntegerTransform4InputStreamNull()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    output = transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(), null);
    assertEquals(TIMES_3, outputToString(output));
    }

    // @Test
    // public void testTransformParamIntegerTransform4OutfileNull()
    //     throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    //     transformation.setup(PARAM_INTEGER_CONFIG);
    //     FileInputStream helloStream = new FileInputStream(helloXml);
    //     assertThrows(NullPointerException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null, helloXml.toString(), helloStream));
    // }

    @Test
    public void testTransformParamIntegerTransform4SystemIdNullInputStreamNull()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_CONFIG);
    assertThrows(TransformationException.class, () -> transformation.transform(PARAM_INTEGER_PARAMS, null, null));
    }

    @Test
    public void testTransformParamIntegerStaticTransform()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(PARAM_INTEGER_STATIC_CONFIG);
    FileInputStream helloStream = new FileInputStream(helloXml);
    output = transformation.transform(null, null, helloXml.toString(), null);
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
    output = transformation.transform(null, null, unparsedEntityXml.toString(), input);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test source=\"passwd\"/>", outputToString(output));
    }

    @Test
    public void testUnparsedEntityNotExpanded3()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(unparsedEntityXml);
    output = transformation.transform(null, null, unparsedEntityXml.toString());
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test source=\"passwd\"/>", outputToString(output));
    }


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
    public void testTagSoup1WithDefaultParser()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    assertThrows(TransformationException.class, () -> transformation.transform(null, null, unparsedEntityXml.toString(), input));
    }

    @Test
    public void testTagSoup1WithNuValidator()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    output = transformation.transform(null, NU_VALIDATOR_CONFIG, unparsedEntityXml.toString(), input);
    assertTrue(outputToString(output).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\">"));
    }

    @Test
    public void testTagSoup1WithCowanTagSoup()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    output = transformation.transform(null, COWAN_TAGSOUP_CONFIG, unparsedEntityXml.toString(), input);
    //assertEquals("", outputToString(output));
    assertTrue(outputToString(output).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:html=\"http://www.w3.org/1999/xhtml\">"));
    }

    @Test
    public void testTagSoup1WithUnknownParser()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    assertThrows(TransformationPreparationException.class, () -> transformation.transform(null, UNKNOWN_PARSER_CONFIG, unparsedEntityXml.toString(), input));
    }

    @Test
    public void testTagSoup1WithBadParser()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    assertThrows(TransformationPreparationException.class, () -> transformation.transform(null, BAD_PARSER_CONFIG, unparsedEntityXml.toString(), input));
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
    output = transformation.transform(null, null, unparsedEntityXml.toString(), input);
    //assertEquals("", outputToString(output));
    assertTrue(outputToString(output).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:html=\"http://www.w3.org/1999/xhtml\">"));
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
    output = transformation.transform(null, CONFIG_WITH_XINCLUDE, includeXml.toString(), input);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    <hello xml:base=\"hello.xml\"><greating>Hello</greating></hello>\n    <hello xml:base=\"hello.xml\"><greating>Hello</greating></hello>\n</wrapper>", outputToString(output));
    }

    @Test
    public void testXIncludeFalse()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(includeXml);
    output = transformation.transform(null, CONFIG_WITH_XINCLUDE_FALSE, includeXml.toString(), input);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    <xi:include href=\"hello.xml\"/>\n    <xi:include href=\"hello.xml\"/>\n</wrapper>", outputToString(output));
    }

    @Test
    public void testXIncludeNotSet()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(includeXml);
    output = transformation.transform(null, null, includeXml.toString(), input);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><wrapper xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n    <xi:include href=\"hello.xml\"/>\n    <xi:include href=\"hello.xml\"/>\n</wrapper>", outputToString(output));
    }

    @Test
    public void testTagSoup1WithNuValidatorWithXInclude()
    throws IOException, ConfigurationException, TransformationPreparationException, TransformationException {
    transformation.setup(ID_CONFIG_ABS);
    FileInputStream input = new FileInputStream(tagSoup1);
    output = transformation.transform(null, NU_VALIDATOR_CONFIG_WITH_XINCLUDE, tagSoup1.toString(), input);
    assertTrue(outputToString(output).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\">"));
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
