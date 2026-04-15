package de.ulbms.scdh.seed.xc.transformer;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.saxon.SaxonXslTransformation;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransformServiceTest {

	private final File helloXml =
			Paths.get("src", "test", "resources", "samples", "hello.xml").toFile();

	private final File tagsoupHtml =
			Paths.get("src", "test", "resources", "samples", "tagsoup.html").toFile();

	private final String times3 =
			"<times>\n   <once n=\"3\"/>\n   <once n=\"2\"/>\n   <once n=\"1\"/>\n   <once n=\"0\"/>\n</times>\n";

	public static final Config COWAN_TAGSOUP_CONFIG;

	static {
		Config cfg = new Config();
		Parser parser = new Parser();
		parser.setPropertyClass("org.ccil.cowan.tagsoup.Parser");
		cfg.setParser(parser);
		COWAN_TAGSOUP_CONFIG = cfg;
	}

	@Test
	public void testTransformIdentityInfoGet() {
		given().when()
				.get("/transformations/identity/info")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("ident", is("identity"))
				.body("class", is(SaxonXslTransformation.TRANSFORMATION_TYPE))
				.body("location", endsWith("id.xsl"));
	}

	@Test
	public void testTransformIdentityParametersGet() {
		given().when()
				.get("/transformations/identity/parameters")
				.then()
				.statusCode(200)
				.body("size()", is(0));
	}

	@Test
	public void testTransformIdentityPost() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.when()
				.post("/transform/identity")
				.then()
				.statusCode(200)
				// .contentType(ContentType.XML)
				// the transformation adds the xml declaration
				.body(endsWith("<hello><greating>Hello</greating></hello>"));
	}

	@Test
	public void testTransformParamIntegerInfoGet() {
		given().when()
				.get("/transformations/param-integer/info")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("ident", is("param-integer"))
				.body("class", is(SaxonXslTransformation.TRANSFORMATION_TYPE))
				.body("location", endsWith("param-integer.xsl"));
	}

	@Test
	public void testTransformParamIntegerParametersGet() {
		given().when()
				.get("/transformations/param-integer/parameters")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("size()", is(1))
				.body("times.occurrenceIndicator", is(""))
				.body("times.itemType", is("xs:integer"))
				.body("times.underlyingDeclaredType", is("xs:integer"))
				.body("times.isRequired", is(true));
	}

	@Test
	public void testTransformParamIntegerPostNoParam() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(500);
	}

	@Disabled("TODO: find a way to send error message again")
	@Test
	public void testTransformParamIntegerPostNoParamHasErrorMessage() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(500)
				// we have an error message:
				// .contentType(ContentType.ANY)
				.body(is("transformation failed: No value supplied for required " + "parameter times"));
	}

	@Test
	public void testTransformParamIntegerPost() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.multiPart("runtimeParameters", "{\"globalParameters\":{\"times\":\"3\"}}", "application/json")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(200)
				.body(endsWith(times3));
	}

	@Test
	public void testTransformParamIntegerPostAsNumber() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.multiPart("runtimeParameters", "{\"globalParameters\":{\"times\":3}}", "application/json")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(200)
				// .contentType(ContentType.XML)
				.body(endsWith(times3));
	}

	@Test
	public void testTransformParamIntegerPostHexNumber() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.multiPart("runtimeParameters", "{\"globalParameters\":{\"times\":\"0x3\"}}", "application/json")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(400);
		// .body(endsWith(times3));
	}

	@Test
	public void testTransformParamIntegerPostInvalidNumber() {
		given().contentType("multipart/form-data")
				.multiPart("source", helloXml, "application/xml")
				.multiPart("runtimeParameters", "{\"globalParameters\":{\"times\":\"3z\"}}", "application/json")
				.when()
				.post("/transform/param-integer")
				.then()
				.statusCode(400);
	}

	@Test
	public void testTransformIdentityPostTagsoup() {
		given().contentType("multipart/form-data")
				.multiPart("source", tagsoupHtml, "text/html")
				.when()
				.post("/transform/tagsoup")
				.then()
				.statusCode(200)
				// .contentType(ContentType.HTML)
				// the transformation adds the xml declaration
				.body(endsWith("</html>"));
	}

	@Test
	public void testTransformIdentityPostTagsoupWithIdentity() {
		given().contentType("multipart/form-data")
				.multiPart("source", tagsoupHtml, "application/xml")
				.when()
				.post("/transform/identity")
				.then()
				.statusCode(500);
	}

	@Test
	public void testTransformIdentityPostTagsoupWithIdentityAndTagsoupParser() {
		given().contentType("multipart/form-data")
				.multiPart("source", tagsoupHtml, "text/html")
				.multiPart("config", COWAN_TAGSOUP_CONFIG, "application/json")
				.when()
				.post("/transform/identity")
				.then()
				.statusCode(200)
				// .contentType(ContentType.XML)
				// the transformation adds the xml declaration
				.body(endsWith("</html>"));
	}
}
