package de.ulbms.scdh.seed.xc.dts.document;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DocumentEndpointTest {

	@Test
	public void testNoParams() {
		given().when().get("/document").then().statusCode(400);
	}

	@Test
	public void testNonExistingPath() {
		given().when().get("/document/asdf").then().statusCode(404);
	}

	@Test
	public void testJohnXml200() {
		given().when().get("/document?resource=john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnTei404() {
		given().when().get("/document?resource=john.tei").then().statusCode(404);
	}

	@Test
	public void testJohnXmlStartEndMembersNotFound() {
		given().when()
				.get("/document?resource=john.xml&start=eins&end=zwei")
				.then()
				.statusCode(404);
	}

	@Test
	public void testJohnXmlRefMembersNotFound() {
		given().when().get("/document?resource=john.xml&ref=eins").then().statusCode(404);
	}

	@Test
	public void testStartWithoutEnd() {
		given().when().get("/document?resource=john.xml&start=John:1:1").then().statusCode(400);
	}

	@Test
	public void testEndWithoutStart() {
		given().when().get("/document?resource=john.xml&end=John:1:1").then().statusCode(400);
	}

	@Test
	public void testStartEndWithRef() {
		given().when()
				.get("/document?resource=john.xml&start=John:1:1&end=John:1:2&ref=John:1")
				.then()
				.statusCode(400);
	}

	@Test
	public void testStartWithoutEndButRef() {
		given().when()
				.get("/document?resource=john.xml&start=John:1:1&ref=John:1")
				.then()
				.statusCode(400);
	}

	// Testing returned contents: For robustness against changes in the XSLT,
	// just assert the presence or absence of significant parts!

	@TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("?resource=john.xml")
	URL johnPlain;

	@Test
	public void testJohnPlain() throws IOException {
		InputStream in = johnPlain.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}

	@TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("?resource=john.xml&ref=John:1:1")
	URL john11;

	@Test
	public void testJohn11() throws IOException {
		InputStream in = john11.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.contains(
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\"><dts:wrapper xmlns:dts=\"https://w3id.org/api/dts#\">"));
		assertTrue(contents.contains("In the beginning was the Word"));
		assertFalse(contents.contains("He was with God in the beginning."));
	}

	@TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("?resource=john.xml&tree=page-hateoas&start=p.1.start&end=p.1.end")
	URL johnP1;

	@Test
	public void testjohnP1() throws IOException {
		InputStream in = johnP1.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.contains(
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\"><dts:wrapper xmlns:dts=\"https://w3id.org/api/dts#\">"));
		assertTrue(contents.contains("<pb n=\"1\"/>"));
		assertTrue(contents.contains("In the beginning was the Word"));
		assertTrue(contents.contains("He was with God in the beginning."));
		assertFalse(contents.contains("<pb n=\"2\"/>"));
		assertFalse(contents.contains("of all mankind."));
	}

	@TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("?resource=john.xml&tree=page-hateoas&ref=p.1")
	URL johnP1ref;

	@Test
	public void testJohnP1milestone() throws IOException {
		InputStream in = johnP1ref.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.contains(
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\"><dts:wrapper xmlns:dts=\"https://w3id.org/api/dts#\">"));
		assertTrue(contents.contains("<pb n=\"1\"/>"));
		assertFalse(contents.contains("In the beginning was the Word"));
		assertFalse(contents.contains("He was with God in the beginning."));
		assertFalse(contents.contains("<pb n=\"2\"/>"));
		assertFalse(contents.contains("of all mankind."));
	}
}
