package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.JsonObject;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StandoffEndpointTest {

	private static final String BASE = "https://dts.example.com";

	private static final File ANNOT_DIR =
			Paths.get("src", "test", "resources", "annotations").toFile();

	private static final File ANNOT_JOHN_FW = new File(ANNOT_DIR, "John.fw.json");
	private static final File ANNOT_JOHN_BW = new File(ANNOT_DIR, "John.bw.json");
	private static final File ANNOT_P11 = new File(ANNOT_DIR, "p1.1.json");

	@Test
	public void testDocumentJohnXmlStatus200() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(200);
	}

	@Test
	public void testForwardNotAllowedWithTransformation() {
		given().multiPart("annotations", ANNOT_JOHN_FW, "application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml?mediaType=text/plain")
				.then()
				.statusCode(405)
				.header("Allow", "");
	}

	@Test
	public void testConNegJsonLD() {
		byte[] graph = given().multiPart("annotations", ANNOT_JOHN_FW, "application/ld+json")
				.accept("application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(200)
				.extract()
				.asByteArray();
		String body = new String(graph, Charset.defaultCharset());
		assertTrue(body.startsWith("{\""), "is a JSON-LD graph");
		assertFalse(body.contains("\"@graph\":"), "@graph is omitted for single annotations");
		assertTrue(body.startsWith("{\"id\":"), "@graph is omitted for single annotations, single id instead");
	}

	@Test
	public void testConNegTurtle() {
		byte[] graph = given().multiPart("annotations", ANNOT_JOHN_FW, "application/ld+json")
				.accept("text/turtle")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(200)
				.extract()
				.asByteArray();
		String body = new String(graph, Charset.defaultCharset());
		assertTrue(body.startsWith("<https://annotations.example.com/samples/p1.1>"), "is a turtle graph");
	}

	private static JsonObject getSelector(JsonObject body) {
		return body // .get("@graph").asJsonArray().get(0).asJsonObject()
				.get("target")
				.asJsonObject()
				.get("selector")
				.asJsonObject();
	}

	private static String getSource(JsonObject body) {
		return body // .get("@graph").asJsonArray().get(0).asJsonObject()
				.get("target")
				.asJsonObject()
				.getString("source");
	}

	private static JsonObject getStartSelector(JsonObject body) {
		return body // .get("@graph").asJsonArray().get(0).asJsonObject()
				.get("target")
				.asJsonObject()
				.get("selector")
				.asJsonObject()
				.get("startSelector")
				.asJsonObject();
	}

	private static JsonObject getEndSelector(JsonObject body) {
		return body // .get("@graph").asJsonArray().get(0).asJsonObject()
				.get("target")
				.asJsonObject()
				.get("selector")
				.asJsonObject()
				.get("endSelector")
				.asJsonObject();
	}

	private static String getType(JsonObject resource) {
		return resource.getString("type");
	}

	private static String getXPathComponent(JsonObject selector) {
		return selector.getString("value");
	}

	private static String getRFC5147Component(JsonObject selector) {
		return selector.get("refinedBy").asJsonObject().getString("value");
	}

	@Test
	public void testForwardToBaseRepresentationWithJohnWhole() {
		JsonObject body = given().multiPart("annotations", ANNOT_JOHN_FW, "application/ld+json")
				.accept("application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(JsonObject.class);
		assertEquals("RangeSelector", getType(getSelector(body)));
		// rewritten start selector has namespaces
		assertEquals("XPathSelector", getType(getStartSelector(body)));
		assertTrue(
				getXPathComponent(getStartSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[3]/"));
		assertTrue(getXPathComponent(getStartSelector(body)).endsWith("text()[1]"));
		assertEquals("char=2", getRFC5147Component(getStartSelector(body)));
		// rewritten end selector was rebased
		assertEquals("XPathSelector", getType(getEndSelector(body)));
		assertTrue(
				getXPathComponent(getEndSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[4]/"));
		assertTrue(getXPathComponent(getEndSelector(body)).endsWith("text()[2]"), "second text node!");
		assertEquals("char=4", getRFC5147Component(getEndSelector(body)), "re-calculated!");
		assertEquals(BASE + "/file/sample/document/john.xml", getSource(body));
	}

	@Disabled
	@Test
	public void testJohn13to15() {
		byte[] body = given().when()
				.get("/file/sample/document/john.xml?start=John:1:3&end=John:1:5")
				.asByteArray();
		assertEquals("", new String(body, Charset.defaultCharset()));
	}

	@Test
	public void testForwardToBaseRepresentationWithJohn13to15() {
		JsonObject body = given().multiPart("annotations", ANNOT_JOHN_FW, "application/ld+json")
				.accept("application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml?start=John:1:3&end=John:1:5")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(JsonObject.class);
		assertEquals("RangeSelector", getType(getSelector(body)));
		// rewritten start selector was rebased
		assertEquals("XPathSelector", getType(getStartSelector(body)));
		// assertEquals("", getXPathComponent(getStartSelector(body)));
		assertTrue(
				getXPathComponent(getStartSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{https://w3id.org/api/dts#}wrapper[1]/Q{http://www.tei-c.org/ns/1.0}l[1]/"),
				"in dts:wrapper and rewritten from [3] to [1]!");
		assertTrue(getXPathComponent(getStartSelector(body)).endsWith("text()[1]"));
		assertEquals("char=2", getRFC5147Component(getStartSelector(body)));
		// rewritten end selector was rebased
		assertEquals("XPathSelector", getType(getEndSelector(body)));
		assertTrue(
				getXPathComponent(getEndSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{https://w3id.org/api/dts#}wrapper[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/"),
				"in dts:wrapper and rewritten from [5] to [2]");
		assertTrue(getXPathComponent(getEndSelector(body)).endsWith("text()[2]"), "second text node!");
		assertEquals("char=4", getRFC5147Component(getEndSelector(body)), "re-calculated!");
		assertEquals(BASE + "/file/sample/document/john.xml?start=John:1:3&end=John:1:5", getSource(body));
	}

	@Test
	public void testBackwardToBaseRepresentationWithJohn13to15() {
		JsonObject body = given().multiPart("annotations", ANNOT_JOHN_BW, "application/ld+json")
				.accept("application/ld+json")
				.when()
				.post("/file/sample/oa/backward/john.xml?start=John:1:3&end=John:1:5")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(JsonObject.class);
		assertEquals("RangeSelector", getType(getSelector(body)));
		// rewritten start selector has namespaces
		assertEquals("XPathSelector", getType(getStartSelector(body)));
		assertTrue(
				getXPathComponent(getStartSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[3]/"));
		assertTrue(getXPathComponent(getStartSelector(body)).endsWith("text()[1]"));
		assertEquals("char=2", getRFC5147Component(getStartSelector(body)));
		// rewritten end selector was rebased
		assertEquals("XPathSelector", getType(getEndSelector(body)));
		assertTrue(
				getXPathComponent(getEndSelector(body))
						.startsWith(
								"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[4]/"));
		assertTrue(getXPathComponent(getEndSelector(body)).endsWith("text()[2]"), "second text node!");
		assertEquals("char=4", getRFC5147Component(getEndSelector(body)), "re-calculated!");
		assertEquals(BASE + "/file/sample/document/john.xml", getSource(body));
	}

	// just for failing!
	@Disabled
	@Test
	public void testDocumentJohnXmlStatus201() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(201);
	}
}
