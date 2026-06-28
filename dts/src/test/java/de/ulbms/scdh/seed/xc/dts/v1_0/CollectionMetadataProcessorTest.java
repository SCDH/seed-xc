package de.ulbms.scdh.seed.xc.dts.v1_0;

import static org.junit.jupiter.api.Assertions.*;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CollectionMetadataProcessorTest {

	private static final File COLLECTION =
			Paths.get("src", "test", "resources", "collection.json").toFile();

	private static final String BASE = "http://example.com/";

	private Uni<InputStream> input;

	private CollectionMetadataProcessor proc;

	@BeforeEach
	public void readCollection() throws FileNotFoundException {
		input = Uni.createFrom().item(new FileInputStream(COLLECTION));
	}

	@BeforeEach
	public void createProcessor() {
		proc = new CollectionMetadataProcessor();
	}

	@Test
	public void testUnknown() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), Map.of(), BASE + "petite");
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
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), Map.of(), "");
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
	public void testNull() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), Map.of(), null);
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
	public void testGeneral() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), Map.of(), BASE + "general");
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
	public void testMatt() {
		Uni<String> result = proc.getResourceLocation(input, COLLECTION.toString(), Map.of(), BASE + "matt.xml");
		result.subscribe()
				.with(
						s -> {
							assertEquals("matt.xml", s, "path to matt.xml");
						},
						e -> {
							assertEquals(0, 1, "must return path to matt.xml");
						});
	}
}
