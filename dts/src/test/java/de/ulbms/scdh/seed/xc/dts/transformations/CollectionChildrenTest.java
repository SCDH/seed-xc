package de.ulbms.scdh.seed.xc.dts.transformations;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;
import static org.junit.jupiter.api.Assertions.*;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.uri.UriValidationPolicy;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.dts.model.Collection;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdLoader;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdOptions;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CollectionChildrenTest {

	private static final File COLLECTION =
			Paths.get("src", "test", "resources", "sample", "collection.json").toFile();

	private static final String BASE = "http://example.com/";

	private static final File TRANSFORMATIONS_CONTEXT_MAP = Paths.get(
					"target", "dependencies", "dts-transformations", "context-map.json")
			.toFile();

	private static final File SEED_CONTEXT_MAP = Paths.get(
					"src", "main", "resources", "META-INF", "resources", "context-map.json")
			.toFile();

	Config config;

	CollectionChildren transformation;

	private byte[] output;

	private String getOutput() {
		return new String(output, StandardCharsets.UTF_8);
	}

	private JsonObject getJson(byte[] output) {
		String out = new String(output, StandardCharsets.UTF_8);
		JsonReader reader = Json.createReader(new StringReader(out));
		JsonStructure json = reader.read();
		return (JsonObject) json;
	}

	/**
	 * Uses Jackson databind to parse output into a {@link Collection} object.
	 */
	private Collection getCollection(byte[] output) throws IOException {
		ObjectMapper om = new ObjectMapper(new JsonFactory());
		JsonParser parser = om.createParser(output);
		return parser.readValueAs(Collection.class);
	}

	@BeforeEach
	public void createConfig() {
		config = new Config();
		config.base(BASE);
	}

	@BeforeEach
	public void setup() {
		transformation = new CollectionChildren();
		DocumentLoader loader = ConfiguredJsonLdLoader.createJsonLdLoader(
				TRANSFORMATIONS_CONTEXT_MAP.toString() + "," + SEED_CONTEXT_MAP.toString(), 100);
		ConfiguredJsonLdOptions opts = ConfiguredJsonLdOptions.of(loader, "none");
		JsonLdOptions options = opts.getJsonLdOptions();
		options.setUriValidation(UriValidationPolicy.None);
		transformation.jsonLdOptions = options;
	}

	@Disabled
	@Test
	void testGeneral() throws IOException, TransformationPreparationException, TransformationException {
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		map.put("requested", pvOf(BASE + "general"));
		params.setGlobalParameters(map);
		InputStream in = new FileInputStream(COLLECTION);
		assertDoesNotThrow(() -> {
			output = transformation.transform(params, config, COLLECTION.toString(), in, null, null);
		});
		JsonObject json = getJson(output);
		assertTrue(json.containsKey("@id"), "has @id");
		assertEquals(BASE + "general", json.getString("@id"));
	}
}
