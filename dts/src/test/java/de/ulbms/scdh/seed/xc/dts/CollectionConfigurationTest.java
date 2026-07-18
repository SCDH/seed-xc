package de.ulbms.scdh.seed.xc.dts;

import static org.junit.jupiter.api.Assertions.*;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.uri.UriValidationPolicy;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdLoader;
import de.ulbms.scdh.seed.xc.jena.ConfiguredJsonLdOptions;
import de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider;
import io.smallrye.mutiny.Uni;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CollectionConfigurationTest {

	private static final File COLLECTION_WITHOUT_CONFIG =
			Paths.get("src", "test", "resources", "sample", "collection.json").toFile();

	private static final File COLLECTION = Paths.get(
					"src", "test", "resources", "sample", "collection-with-config.json")
			.toFile();

	private static final Path PROJECT = Paths.get("src", "test", "resources", "sample");

	private static final File TRANSFORMATIONS_CONTEXT_MAP = Paths.get(
					"target", "dependencies", "dts-transformations", "context-map.json")
			.toFile();

	private static final File SEED_CONTEXT_MAP = Paths.get(
					"src", "main", "resources", "META-INF", "resources", "context-map.json")
			.toFile();

	private static final String BASE = "http://example.com/";

	private Uni<InputStream> inputWithoutConfig;

	private Uni<InputStream> input;

	private CollectionConfiguration proc;

	private Config config;

	@BeforeEach
	public void readCollection() throws FileNotFoundException {
		// see https://github.com/smallrye/smallrye-mutiny/discussions/1288
		inputWithoutConfig = Uni.createFrom().emitter(emitter -> {
			try {
				emitter.complete(new FileInputStream(COLLECTION_WITHOUT_CONFIG));
			} catch (Exception e) {
				emitter.fail(e);
			}
		});
		input = Uni.createFrom().emitter(emitter -> {
			try {
				emitter.complete(new FileInputStream(COLLECTION));
			} catch (Exception e) {
				emitter.fail(e);
			}
		});
	}

	private ResourceProvider resourceProvider = new FileSystemResourceProvider(PROJECT);

	@BeforeEach
	public void createProcessor() {
		proc = new CollectionConfiguration();
		DocumentLoader loader = ConfiguredJsonLdLoader.createJsonLdLoader(
				TRANSFORMATIONS_CONTEXT_MAP.toString() + "," + SEED_CONTEXT_MAP.toString(), 100);
		ConfiguredJsonLdOptions opts = ConfiguredJsonLdOptions.of(loader, "none");
		JsonLdOptions options = opts.getJsonLdOptions();
		options.setUriValidation(UriValidationPolicy.None);
		proc.GRAPH = "collection.json";
	}

	@BeforeEach
	public void createConfig() {
		config = new Config();
		config.base(BASE);
	}

	@Test
	public void testRecordConfigWithoutConfig() throws ExecutionException, InterruptedException {
		Uni<RecordConfig> result = proc.getRecordConfig(
				resourceProvider, inputWithoutConfig, COLLECTION_WITHOUT_CONFIG.toString(), config, Map.of());
		// see https://github.com/smallrye/smallrye-mutiny/discussions/1288
		AtomicReference<CompletableFuture<RecordConfig>> rc = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			rc.set(result.subscribe().asCompletionStage());
		});
		assertNull(rc.get().get(), "no record config present in the collection");
	}

	@Test
	public void testRecordConfig() throws ExecutionException, InterruptedException {
		Uni<RecordConfig> result =
				proc.getRecordConfig(resourceProvider, input, COLLECTION.toString(), config, Map.of());
		AtomicReference<CompletableFuture<RecordConfig>> rc = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			rc.set(result.subscribe().asCompletionStage());
		});
		assertInstanceOf(RecordConfig.class, rc.get().get());
		assertNotNull(rc.get().get(), "configuration is present in " + COLLECTION);
		assertInstanceOf(RecordConfig.class, rc.get().get());
		assertNotNull(rc.get().get().getFrames(), "frames present in " + COLLECTION);
		assertInstanceOf(RecordFrames.class, rc.get().get().getFrames());
		assertNotNull(rc.get().get().getFrames().getCollection());
		assertNull(rc.get().get().getFrames().getAll());
		assertNull(rc.get().get().getFrames().getNavigation());
	}

	@Test
	public void testMergeForCollection() throws ExecutionException, InterruptedException {
		Uni<Config> result = proc.merge(resourceProvider, input, COLLECTION.toString(), config, Map.of(), "collection");
		AtomicReference<CompletableFuture<Config>> rc = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			rc.set(result.subscribe().asCompletionStage());
		});
		assertInstanceOf(Config.class, rc.get().get());
		assertNotSame(config, rc.get().get(), "must not overwrite the default configuration");
		assertEquals(
				"https://dtsapi.org/specifications/context/1.0rc1.json",
				rc.get().get().getContext().getLocation().toString());
		assertNull(rc.get().get().getContext().getDocument(), "there may not be a document context any more");
	}

	@Test
	public void testMergeForNavigation() throws ExecutionException, InterruptedException {
		Uni<Config> result = proc.merge(resourceProvider, input, COLLECTION.toString(), config, Map.of(), "navigation");
		AtomicReference<CompletableFuture<Config>> rc = new AtomicReference<>();
		assertDoesNotThrow(() -> {
			rc.set(result.subscribe().asCompletionStage());
		});
		assertInstanceOf(Config.class, rc.get().get());
		assertNotSame(config, rc.get().get(), "must not overwrite the default configuration");
		assertNull(rc.get().get().getContext(), "there must still be the test's default context, which was null");
	}
}
