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
			assertEquals(10, bodyObj.size());
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
			// assertEquals("", result);
		}
	}
}
