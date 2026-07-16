package de.ulbms.scdh.seed.xc.dts;

import static org.junit.jupiter.api.Assertions.*;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.uri.UriValidationPolicy;
import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdLoader;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdOptions;
import de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CollectionMetadataProcessorTest {

	private static final File COLLECTION =
			Paths.get("src", "test", "resources", "sample", "collection.json").toFile();

	private static final Path PROJECT = Paths.get("src", "test", "resources", "sample");

	private static final File TRANSFORMATIONS_CONTEXT_MAP = Paths.get(
					"target", "dependencies", "dts-transformations", "context-map.json")
			.toFile();

	private static final File SEED_CONTEXT_MAP = Paths.get(
					"src", "main", "resources", "META-INF", "resources", "context-map.json")
			.toFile();

	private static final String BASE = "http://example.com/";

	private Uni<InputStream> input;

	private CollectionMetadataProcessor proc;

	private Config config;

	@BeforeEach
	public void readCollection() throws FileNotFoundException {
		input = Uni.createFrom().item(new FileInputStream(COLLECTION));
	}

	private ResourceProvider resourceProvider = new FileSystemResourceProvider(PROJECT);

	@BeforeEach
	public void createProcessor() {
		proc = new CollectionMetadataProcessor();
		DocumentLoader loader = ConfiguredJsonLdLoader.createJsonLdLoader(
				TRANSFORMATIONS_CONTEXT_MAP.toString() + "," + SEED_CONTEXT_MAP.toString(), 100);
		ConfiguredJsonLdOptions opts = ConfiguredJsonLdOptions.of(loader, "none");
		JsonLdOptions options = opts.getJsonLdOptions();
		options.setUriValidation(UriValidationPolicy.None);
		proc.jsonLdOptions = options;
		proc.GRAPH = "collection.json";
	}

	@BeforeEach
	public void createConfig() {
		config = new Config();
		config.base(BASE);
	}

	@Test
	public void testUnknown() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), BASE + "petite");
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "unknown is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "unknown not found");
						});
	}

	@Test
	public void testUnknownStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), new URI(BASE + "petite"));
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "unknown is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "unknown not found");
						});
	}

	@Test
	public void testEmpty() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), "");
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, "[empty string] is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "[empty string] not found");
						});
	}

	@Test
	public void testEmptyStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), new URI(""));
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "[empty string] is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "[empty string] not found");
						});
	}

	@Test
	public void testNull() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), null);
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, "[null] is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "[null] not found");
						});
	}

	@Test
	public void testNullStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), null);
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "[null] is not in the graph");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(NotFoundException.class, e.getClass(), "[null] not found");
						});
	}

	@Test
	public void testGeneral() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), BASE + "general");
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "general is not a dts:Resource");
						},
						e -> {
							assertEquals(BadRequestException.class, e.getClass(), BASE + "general is not a resource");
							assertTrue(e.getMessage().contains("not a dts:Resource"));
						});
	}

	@Test
	public void testGeneralStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), new URI(BASE + "general"));
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "general is not a dts:Resource");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(BadRequestException.class, e.getClass(), "general is not a resource");
							assertTrue(e.getMessage().contains("not a dts:Resource"));
						});
	}

	@Test
	public void testLocationStringMatt() {
		Uni<String> result =
				proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), BASE + "matt.xml");
		result.subscribe()
				.with(
						s -> {
							assertEquals("matt.xml", s, "path to matt.xml");
						},
						e -> {
							assertEquals(0, 1, "must return path to matt.xml");
						});
	}

	@Test
	public void testLocationStringMattStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), new URI(BASE + "matt.xml"));
		result.subscribe()
				.with(
						s -> {
							assertEquals(0, 1, BASE + "matt.xml is a resource, but the file is not present");
						},
						e -> {
							// assertEquals("", e.getMessage());
							assertEquals(
									InternalServerErrorException.class,
									e.getClass(),
									"cannot open input stream for missing file");
							assertTrue(e.getMessage().contains("cannot open"));
						});
	}

	@Test
	public void testLocationPathWithJohn() {
		Uni<String> result =
				proc.getResourceLocation(input, COLLECTION.toString(), config, Map.of(), BASE + "john.xml");
		result.subscribe()
				.with(
						s -> {
							assertEquals("john.xml", s, "path to john.xml");
						},
						e -> {
							assertEquals(0, 1, "must return path to john.xml");
						});
	}

	@Test
	public void testLocationPathWithJohnStream() throws URISyntaxException {
		Uni<InputStream> result = proc.getResourceAsync(resourceProvider, config, Map.of(), new URI(BASE + "john.xml"));
		result.subscribe()
				.with(
						s -> {
							assertDoesNotThrow(() -> s.read(), "can read from the stream");
							assertDoesNotThrow(s::close, "stream can be closed");
						},
						e -> {
							assertEquals(0, 1, "must return stream from john.xml");
						});
	}
}
