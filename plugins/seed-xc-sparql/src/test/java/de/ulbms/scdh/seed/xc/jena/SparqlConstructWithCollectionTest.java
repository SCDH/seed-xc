package de.ulbms.scdh.seed.xc.jena;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.*;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SparqlConstructWithCollectionTest {

	private static final String BASE_URI = "http://example.com/";

	private byte[] output;

	private static final File CONFIG_DIR = Paths.get("src", "test", "resources").toFile();

	private static final File DATA_DIR =
			Paths.get("src", "test", "resources", "data").toFile();

	private static final File DATA_N3 = new File(DATA_DIR, "collection.n3");

	private static final String[] GENERAL_MEMBERS = {
		"\"http://example.com/agrapha\"",
		"\"http://example.com/apocryphs\"",
		"\"http://example.com/john.xml\"",
		"\"http://example.com/matt.xml\"",
	};

	private static final File COLLECTION_CONFIG = new File(CONFIG_DIR, "config-collection.json");

	private SparqlConstruct CHILDREN;

	@Inject
	HttpServerRequest request;

	@BeforeEach
	public void setupTransformations() throws IOException, ConfigurationException {
		ObjectMapper om = new ObjectMapper(new JsonFactory());
		TransformationMap transformations = om.readValue(COLLECTION_CONFIG, TransformationMap.class);
		TransformationInfo CHILDREN_INFO = transformations.get("dts-transformations-rq-children");
		CHILDREN = new SparqlConstruct();
		CHILDREN.setup(CHILDREN_INFO, COLLECTION_CONFIG);
		//noinspection InstantiationOfUtilityClass
		CHILDREN.serializer = new Serializer();
		CHILDREN.jsonLdContextFactory = new JsonLdContext();
		CHILDREN.jsonLdDocumentLoader = ConfiguredJsonLdLoader.createJsonLdLoader(10000);
		CHILDREN.parameterConverter = new ParameterConverter();
	}

	@Test
	public void testResourceGeneral() throws IOException, TransformationPreparationException, TransformationException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "general"));
			output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			String result = new String(output, StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"));
			assertTrue(bodyObj.containsKey("@context"));
			assertTrue(bodyObj.containsKey("@id"));
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

	@Test
	public void testResourceGeneralNavParents()
			throws IOException, TransformationPreparationException, TransformationException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "general", "nav", "parents"));
			output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			String result = new String(output, StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"));
			assertTrue(bodyObj.containsKey("@id"));
		}
	}

	@Test
	public void testResourceGeneralNavParentsPage1()
			throws IOException, TransformationPreparationException, TransformationException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "general", "nav", "parents", "page", "1"));
			output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			String result = new String(output, StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"));
			assertTrue(bodyObj.containsKey("@id"));
		}
	}

	@Test
	public void testUnknownResource() throws IOException, TransformationPreparationException, TransformationException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "unknown"));
			output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			String result = new String(output, StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"), "no graph");
			assertFalse(bodyObj.containsKey("@id"), "must not have an resource or collection object");
		}
	}

	@Test
	public void testUnknownParam() throws IOException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "general", "lion", "tiger"));
			assertDoesNotThrow(() -> {
				output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			});
			String result = new String(output, StandardCharsets.UTF_8);
			JsonReader reader = Json.createReader(new StringReader(result));
			JsonStructure body = reader.read();
			assertEquals(JsonValue.ValueType.OBJECT, body.getValueType());
			JsonObject bodyObj = (JsonObject) body;
			assertFalse(bodyObj.containsKey("@graph"));
			assertTrue(bodyObj.containsKey("@id"));
		}
	}

	@Test
	public void testUriParamType() throws IOException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", "general"));
			assertDoesNotThrow(() -> {
				output = CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request);
			});
		}
	}

	@Test
	public void testIntegerParamTypeMismatch() throws IOException {
		try (InputStream in = new FileInputStream(DATA_N3)) {
			RuntimeParameters params = new RuntimeParameters();
			params.setGlobalParameters(Map.of("id", BASE_URI + "general", "page", "tiger"));
			assertThrows(
					TransformationPreparationException.class,
					() -> CHILDREN.transform(params, null, DATA_N3.toString(), in, null, request));
		}
	}
}
