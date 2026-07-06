package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.dts.URITemplateBuilder;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionEndpointTest {

	private static final String[] GENERAL_MEMBERS = {
		"/collection/agrapha", "ollection/apocryphs", "collection/john.xml", "collection/matt.xml",
	};

	@Test
	public void testStatusGeneral() {
		given().when().get("/collection/general").then().statusCode(200);
	}

	@Test
	public void testStatusUnknown() {
		given().when().get("/collection/unknown?nav=parents").then().statusCode(404);
	}

	@Disabled
	@Test
	public void testStatusDefault() {
		given().when().get("/collection/").then().statusCode(200);
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/general")
	URL urlGeneral;

	@Test
	public void testGeneral() throws IOException {
		try (InputStream in = urlGeneral.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").endsWith("/collection/general"));
			assertFalse(bodyObj.containsKey("mediaTypes"), "collection does not have mediaTypes");
			assertTrue(bodyObj.containsKey("collection"), "collection must have collection URI template");
			assertFalse(bodyObj.containsKey("navigation"), "collection must not have navigation URI template");
			assertFalse(bodyObj.containsKey("document"), "collection must not have document URI template");
			String collection = bodyObj.getString("collection");
			assertTrue(bodyObj.containsKey("member"));
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(4, members.size());
			members.forEach((m) -> {
				assertEquals(JsonValue.ValueType.OBJECT, m.getValueType());
				JsonObject member = (JsonObject) m;
				String memberId = member.getString("@id");
				assertTrue(
						Arrays.asList(GENERAL_MEMBERS).contains(memberId.substring(memberId.length() - 19)),
						memberId + " in members");
				if (memberId.endsWith(".xml")) {
					assertTrue(member.containsKey("mediaTypes"), "has mediaTypes property");
					assertEquals(
							JsonValue.ValueType.ARRAY, member.get("mediaTypes").getValueType());
					JsonArray mediaTypes = (JsonArray) member.get("mediaTypes");
					assertFalse(mediaTypes.isEmpty(), "at least one transformation available");
				}
				assertTrue(member.containsKey("collection"), "collection must have collection URI template");
				assertNotEquals(
						collection,
						member.getString("collection"),
						"member's template URI differs from the collection");
				if (member.getString("@type").equals("Resource")) {
					assertTrue(member.containsKey("navigation"), "collection not have navigation URI template");
					assertTrue(member.containsKey("document"), "collection not have document URI template");
				} else {
					assertFalse(member.containsKey("navigation"), "collection not have navigation URI template");
					assertFalse(member.containsKey("document"), "collection not have document URI template");
				}
			});
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/general") // default explicit
	URL urlGeneralChildren;

	@Test
	public void testGeneralChildren() throws IOException {
		try (InputStream in = urlGeneralChildren.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").endsWith("/collection/general"));
			assertFalse(bodyObj.containsKey("mediaTypes"), "collection does not have mediaTypes");
			assertTrue(bodyObj.containsKey("member"));
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(4, members.size());
			members.forEach((m) -> {
				assertEquals(JsonValue.ValueType.OBJECT, m.getValueType());
				JsonObject member = (JsonObject) m;
				String memberId = member.getString("@id");
				assertTrue(
						Arrays.asList(GENERAL_MEMBERS).contains(memberId.substring(memberId.length() - 19)),
						memberId + " in members");
			});
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("collection/general?nav=parents") // default explicit
	URL urlGeneralParents;

	@Test
	public void testGeneralParents() throws IOException {
		try (InputStream in = urlGeneralParents.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			// assertEquals(10, bodyObj.size());
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").contains("/collection/general"));
			assertFalse(bodyObj.containsKey("mediaTypes"), "collection does not have mediaTypes");
			assertTrue(bodyObj.containsKey("collection"), "collection must have collection URI template");
			assertFalse(bodyObj.containsKey("navigation"), "collection must not have navigation URI template");
			assertFalse(bodyObj.containsKey("document"), "collection must not have document URI template");
			assertFalse(bodyObj.containsKey("member"), "leave has no member");
			//			if (bodyObj.containsKey("member")) {
			//				assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			//				JsonArray members = (JsonArray) bodyObj.get("member");
			//				assertEquals(0, members.size());
			//			}
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/apocryphs")
	URL urlCollection;

	@Test
	public void testCollection() throws IOException {
		try (InputStream in = urlCollection.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			// assertEquals(10, bodyObj.size());
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").endsWith("/collection/apocryphs"));
			assertFalse(bodyObj.containsKey("mediaTypes"), "collection does not have mediaTypes");
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(2, members.size());
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/apocryphs?nav=parents")
	URL urlCollectionParents;

	@Test
	public void testCollectionParents() throws IOException {
		try (InputStream in = urlCollectionParents.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			// assertEquals(10, bodyObj.size());
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").contains("/collection/apocryphs"));
			assertFalse(bodyObj.containsKey("mediaTypes"), "collection does not have mediaTypes");
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(1, members.size());
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/matt.xml")
	URL urlResource;

	@Test
	public void testResource() throws IOException {
		try (InputStream in = urlResource.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertTrue(bodyObj.getString("@id").endsWith("/collection/matt.xml"));
			assertFalse(bodyObj.containsKey("member"), "has no member");
			assertTrue(bodyObj.containsKey("mediaTypes"), "has mediaTypes property");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("mediaTypes").getValueType());
			JsonArray mediaTypes = (JsonArray) bodyObj.get("mediaTypes");
			assertTrue(bodyObj.containsKey("collection"), "collection must have collection URI template");
			assertTrue(bodyObj.containsKey("navigation"), "collection not have navigation URI template");
			assertTrue(bodyObj.containsKey("document"), "collection not have document URI template");
			assertFalse(mediaTypes.isEmpty(), "at least one transformation available");
			//			if (bodyObj.containsKey("member")) {
			//				assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			//				JsonArray members = (JsonArray) bodyObj.get("member");
			//				assertEquals(0, members.size());
			//			}
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/matt.xml?nav=parents")
	URL urlResourceParents;

	@Test
	public void testResourceParents() throws IOException {
		try (InputStream in = urlResourceParents.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			assertTrue(bodyObj.containsKey("@id"), "has @id");
			assertEquals(
					JsonValue.ValueType.STRING, ((JsonObject) body).get("@id").getValueType());
			assertEquals(urlResource.toString(), bodyObj.getString("@id"));
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(1, members.size());
			assertTrue(bodyObj.containsKey("mediaTypes"), "has mediaTypes property");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("mediaTypes").getValueType());
			JsonArray mediaTypes = (JsonArray) bodyObj.get("mediaTypes");
			assertTrue(bodyObj.containsKey("collection"), "collection must have collection URI template");
			assertTrue(bodyObj.containsKey("navigation"), "collection not have navigation URI template");
			assertTrue(bodyObj.containsKey("document"), "collection not have document URI template");
		}
	}

	// @TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("/collection/unknown?nav=parents")
	URL urlUnknownParents;

	@Disabled // see similar test on top. 404 is better than empty graph
	@Test
	public void testUnknownParentsContent() throws IOException {
		try (InputStream in = urlUnknownParents.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "has no @graph");
			assertTrue(bodyObj.containsKey("@context"), "has @context");
			assertFalse(bodyObj.containsKey("@id"), "has no @id");
		}
	}

	@Test
	public void testMediaTypes() throws IOException {
		// The dts:requested extension property is semantically meaningless but key to get JSON-LD framing right!
		try (InputStream in = urlResource.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertTrue(bodyObj.containsKey("mediaTypes"), "Resource has mediaTypes property");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("mediaTypes").getValueType());
			JsonArray mediaTypes = (JsonArray) bodyObj.get("mediaTypes");
			assertTrue(mediaTypes.size() > 1, "at least 2 transformations configured for document endpoint");
			assertEquals(2, mediaTypes.size());
			// assertTrue(mediaTypes.stream().map(String::valueOf).toList().contains("text/plain"), "text/plain is
			// available");
			boolean plainPresent = false;
			boolean teiPresent = false;
			for (JsonValue mediaType : mediaTypes.stream().toList()) {
				assertEquals(JsonValue.ValueType.STRING, mediaType.getValueType());
				JsonString js = (JsonString) mediaType;
				if (js.getString().equals("text/plain")) plainPresent = true;
				if (js.getString().equals("application/tei+xml")) teiPresent = true;
			}
			assertTrue(plainPresent, "text/plain is available from dts-transformations-testing");
			assertTrue(teiPresent, "application/tei+xml is available from plain dts-transformations");
		}
	}

	@Test
	public void testRequestedExtension() throws IOException {
		// The dts:requested extension property is semantically meaningless but key to get JSON-LD framing right!
		try (InputStream in = urlGeneral.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("dts:requested"), "dts:requested not directly in Collection object");
			assertFalse(bodyObj.containsKey("requested"), "requested not directly in Collection object");
			assertTrue(bodyObj.containsKey("extensions"), "Collection has extensions property");
			assertEquals(JsonValue.ValueType.OBJECT, bodyObj.get("extensions").getValueType());
			JsonObject extensions = (JsonObject) bodyObj.get("extensions");
			assertTrue(extensions.containsKey("requested"), "requested is nested in extensions");
		}
	}

	@Disabled
	@Test
	public void testIdFromUrl() throws IOException {
		try (InputStream in = urlCollectionParents.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertEquals(urlCollectionParents.toString(), bodyObj.getString("@id"));
		}
	}

	@Test
	public void testUriTemplates() throws IOException {
		String resource = "matt.xml";
		try (InputStream in = urlResource.openStream()) {
			String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertTrue(bodyObj.getString("collection").contains("/collection/"));
			assertTrue(
					bodyObj.getString("collection")
							.contains("/collection/" + resource + URITemplateBuilder.THIS_COLLECTION_TEMPLATE),
					"collection URI template");
			assertTrue(bodyObj.getString("collection").endsWith("}"));
			assertTrue(bodyObj.getString("navigation").contains("/navigation/"));
			assertTrue(
					bodyObj.getString("navigation")
							.contains("/navigation/" + resource + URITemplateBuilder.THIS_NAVIGATION_TEMPLATE),
					"navigation URI template");
			assertTrue(bodyObj.getString("document").endsWith("}"));
			assertTrue(bodyObj.getString("document").contains("/document/"));
			assertTrue(
					bodyObj.getString("document")
							.endsWith("/document/" + resource + URITemplateBuilder.THIS_DOCUMENT_TEMPLATE),
					"document URI template");
		}
	}
}
