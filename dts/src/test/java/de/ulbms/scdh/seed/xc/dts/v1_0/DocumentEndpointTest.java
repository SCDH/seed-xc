package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DocumentEndpointTest {

	// legacy ?direct parameters are simple ignored

	private static final String BASE = "http://example.com/"; // "http%3A%2F%2Fexample.com%2F";

	@Disabled
	@Test
	public void testNoParams() {
		given().when().get("/file/sample/document").then().statusCode(400);
	}

	@Test
	public void testNonExistingPath() {
		given().when().get("/file/sample/document/asdf?direct=true").then().statusCode(404);
	}

	@Test
	public void testJohnXml200() {
		given().when().get("/file/sample/document/john.xml?direct=true").then().statusCode(200);
	}

	@Test
	public void testJohnXmlIndirect200() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnTei404() {
		given().when().get("/file/sample/document/john.tei?direct=true").then().statusCode(404);
	}

	@Test
	public void testJohnXmlStartEndMembersNotFound() {
		given().when()
				.get("/file/sample/document/john.xml?start=eins&end=zwei&direct=true")
				.then()
				.statusCode(404);
	}

	@Test
	public void testJohnXmlRefMembersNotFound() {
		given().when()
				.get("/file/sample/document/john.xml?ref=eins&direct=true")
				.then()
				.statusCode(404);
	}

	@Test
	public void testStartWithoutEnd() {
		given().when()
				.get("/file/sample/document/john.xml?start=John:1:1&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testEndWithoutStart() {
		given().when()
				.get("/file/sample/document/john.xml?end=John:1:1&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testStartEndWithRef() {
		given().when()
				.get("/file/sample/document/john.xml?start=John:1:1&end=John:1:2&ref=John:1&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testStartWithoutEndButRef() {
		given().when()
				.get("/file/sample/document/john.xml?start=John:1:1&ref=John:1&direct=true")
				.then()
				.statusCode(400);
	}

	// Testing returned contents: For robustness against changes in the XSLT,
	// just assert the presence or absence of significant parts!

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml?direct=true")
	URL johnPlain;

	@Test
	public void testJohnPlain() throws IOException {
		InputStream in = johnPlain.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml")
	URL johnPlainIndirect;

	@Test
	public void testJohnPlainIndirect() throws IOException {
		InputStream in = johnPlainIndirect.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		assertTrue(contents.contains("<TEI"));
	}

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml?ref=John:1:1&direct=true")
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

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml?tree=page-hateoas&start=p.1.start&end=p.1.end&direct=true")
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

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml?tree=page-hateoas&ref=p.1&direct=true")
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

	@Test
	public void testJohnXmlMediaTypeXml200() {
		given().when()
				.get("/file/sample/document/john.xml?mediaType=application/tei+xml&direct=true")
				.then()
				.statusCode(200);
	}

	@Test
	public void testJohnXmlMediaTypeHtml200() {
		given().when()
				.get("/file/sample/document/john.xml?mediaType=text/html&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testJohnXmlMediaTypePlaintext200() {
		given().when()
				.get("/file/sample/document/john.xml?mediaType=text/plain&direct=true")
				.then()
				.statusCode(200);
	}

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("/file/sample/document/john.xml?mediaType=text/plain&direct=true")
	URL johnMediaTypePlaintext;

	@Test
	public void testJohnMediaTypePlaintextHasNoTags() throws IOException {
		InputStream in = johnMediaTypePlaintext.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertFalse(contents.contains("<TEI"), "no TEI element");
		assertFalse(contents.contains("<l"), "no verse elements");
		assertFalse(contents.contains("<?xml"), "no XML declaration");
		assertFalse(contents.contains("<"), "no triangle bracket");
	}

	@Test
	public void testJohnMediaTypePlaintext() throws IOException {
		InputStream in = johnMediaTypePlaintext.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(
				contents.contains(
						"In the beginning was the Word, and the Word was with God, and the Word was  God.\nHe was with God in the beginning."));
		assertTrue(contents.endsWith("There was a man sent from God whose name was John.\nbla"));
	}

	// @TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource(
			"/file/sample/document/john.xml?tree=page-hateoas&start=p.1.start&end=p.1.end&mediaType=text/plain&direct=true")
	URL johnP1MediaTypePlaintext;

	@Test
	public void testJohnP1MediaTypePlaintext() throws IOException {
		InputStream in = johnP1MediaTypePlaintext.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.contains("In the beginning was the Word"));
		// assertTrue(contents.endsWith("In him was life, and that life was the light")); // not contained because of
		// tei2txt stylesheet
		assertFalse(contents.contains("of all mankind."));
	}
}
