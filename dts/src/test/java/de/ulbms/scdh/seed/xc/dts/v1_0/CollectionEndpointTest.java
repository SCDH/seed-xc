package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionEndpointTest {

	private static final String[] GENERAL_MEMBERS = {
		"\"http://example.com/agrapha\"",
		"\"http://example.com/apocryphs\"",
		"\"http://example.com/john.xml\"",
		"\"http://example.com/matt.xml\"",
	};

	@Test
	public void testStatusGeneral() {
		given().when().get("/collection?id=http://example.com/general").then().statusCode(200);
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?id=http://example.com/general")
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
			assertEquals("\"http://example.com/general\"", bodyObj.get("@id").toString());
			assertTrue(bodyObj.containsKey("member"));
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(4, members.size());
			members.forEach((m) -> {
				assertEquals(JsonValue.ValueType.OBJECT, m.getValueType());
				JsonObject member = (JsonObject) m;
				String memberId = member.get("@id").toString();
				assertTrue(Arrays.asList(GENERAL_MEMBERS).contains(memberId), memberId + " in members");
			});
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?nav=children&id=http://example.com/general") // default explicit
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
			assertEquals("\"http://example.com/general\"", bodyObj.get("@id").toString());
			assertTrue(bodyObj.containsKey("member"));
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(4, members.size());
			members.forEach((m) -> {
				assertEquals(JsonValue.ValueType.OBJECT, m.getValueType());
				JsonObject member = (JsonObject) m;
				String memberId = member.get("@id").toString();
				assertTrue(Arrays.asList(GENERAL_MEMBERS).contains(memberId), memberId + " in members");
			});
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?nav=parents&id=http://example.com/general") // default explicit
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
			assertEquals("\"http://example.com/general\"", bodyObj.get("@id").toString());
			assertFalse(bodyObj.containsKey("member"), "leave has no member");
			//			if (bodyObj.containsKey("member")) {
			//				assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			//				JsonArray members = (JsonArray) bodyObj.get("member");
			//				assertEquals(0, members.size());
			//			}
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?id=http://example.com/apocryphs")
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
			assertEquals("\"http://example.com/apocryphs\"", bodyObj.get("@id").toString());
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(2, members.size());
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?nav=parents&id=http://example.com/apocryphs")
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
			assertEquals("\"http://example.com/apocryphs\"", bodyObj.get("@id").toString());
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(1, members.size());
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?id=http://example.com/matt.xml")
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
			assertEquals("\"http://example.com/matt.xml\"", bodyObj.get("@id").toString());
			assertFalse(bodyObj.containsKey("member"), "has no member");
			//			if (bodyObj.containsKey("member")) {
			//				assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			//				JsonArray members = (JsonArray) bodyObj.get("member");
			//				assertEquals(0, members.size());
			//			}
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?nav=parents&id=http://example.com/matt.xml")
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
			assertEquals("\"http://example.com/matt.xml\"", bodyObj.get("@id").toString());
			assertTrue(bodyObj.containsKey("member"), "has member");
			assertEquals(JsonValue.ValueType.ARRAY, bodyObj.get("member").getValueType());
			JsonArray members = (JsonArray) bodyObj.get("member");
			assertEquals(1, members.size());
		}
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?nav=parents&id=http://example.com/unknown")
	URL urlUnknownParents;

	@Test
	public void testUnknownParents() throws IOException {
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
}
