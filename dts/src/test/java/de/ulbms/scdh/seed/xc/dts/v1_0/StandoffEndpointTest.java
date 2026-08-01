package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
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

	private static final File ANNOT_JOHN = new File(ANNOT_DIR, "John.fw.json");
	private static final File ANNOT_P11 = new File(ANNOT_DIR, "p1.1.json");

	@Test
	public void testDocumentJohnXmlStatus200() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnForwardTxtRepresentation() {
		given().multiPart("annotations", ANNOT_JOHN, "application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml?mediaType=text/plain")
				.then()
				.statusCode(405)
				.header("Allow", "");
	}

	@Test
	public void testJohnForwardBaseRepresentationAsJsonLD() {
		byte[] graph = given().multiPart("annotations", ANNOT_JOHN, "application/ld+json")
				.accept("application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(200)
				.extract().asByteArray();
		String body = new String(graph, Charset.defaultCharset());
		assertTrue(body.startsWith("{\"@graph\":"), "is a JSON-LD graph");
	}

	@Test
	public void testJohnForwardBaseRepresentationAsTurtle() {
		byte[] graph = given().multiPart("annotations", ANNOT_JOHN, "application/ld+json")
				.accept("text/turtle")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(200)
				.extract().asByteArray();
		String body = new String(graph, Charset.defaultCharset());
		assertEquals("", body);
		assertTrue(body.startsWith("<https://annotations.example.com/samples/p1.1>"), "is a turtle graph");
	}

	@Disabled
	@Test
	public void testDocumentJohnXmlStatus205() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(205);
	}

	@Test
	public void testDocumentJohnXmlStatus201() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(201);
	}

}
