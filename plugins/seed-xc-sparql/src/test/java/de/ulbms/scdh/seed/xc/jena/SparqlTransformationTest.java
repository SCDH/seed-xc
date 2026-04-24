package de.ulbms.scdh.seed.xc.jena;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.*;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SparqlTransformationTest {

	private static final File DATA_DIR =
			Paths.get("src", "test", "resources", "data").toFile();

	private static final File VCDB1 = new File(DATA_DIR, "vc-db-1.rdf");

	private static final File RQ_DIR =
			Paths.get("src", "test", "resources", "rq").toFile();

	private static final File CONFIG = RQ_DIR;

	private static TransformationInfo QS1;

	private static TransformationInfo QC1;

	private static TransformationInfo QC1_TTL;

	@Inject
	HttpServerRequest request;

	private byte[] output;

	private String getOutput() {
		return new String(output, StandardCharsets.UTF_8);
	}

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SparqlTransformation.TRANSFORMATION_TYPE);
		info.setLocation(new File(RQ_DIR, "qs1.rq").getAbsolutePath());
		QS1 = info;
	}

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SparqlTransformation.TRANSFORMATION_TYPE);
		info.setLocation(new File(RQ_DIR, "qc1.rq").getAbsolutePath());
		info.setMediaType("application/n-triples");
		QC1 = info;
	}

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SparqlTransformation.TRANSFORMATION_TYPE);
		info.setLocation(new File(RQ_DIR, "qc1.rq").getAbsolutePath());
		info.setMediaType("text/turtle");
		QC1_TTL = info;
	}

	SparqlTransformation transformation;

	@BeforeEach
	public void setup() {
		transformation = new SparqlTransformation();
		transformation.serializer = new Serializer();
	}

	@Test
	public void testNoSystemIdNonDefaultLang()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					FileNotFoundException {
		transformation.setup(QS1, CONFIG);
		InputStream in = new FileInputStream(VCDB1);
		assertThrows(
				TransformationException.class, () -> transformation.transform(null, null, null, in, null, request));
	}

	@Test
	public void testSelectQuery()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					FileNotFoundException {
		transformation.setup(QS1, CONFIG);
		InputStream in = new FileInputStream(VCDB1);
		assertThrows(
				TransformationException.class,
				() -> transformation.transform(null, null, VCDB1.getAbsolutePath(), in, null, request));
	}

	@Test
	public void testConstructQuery()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					FileNotFoundException {
		transformation.setup(QC1, CONFIG);
		InputStream in = new FileInputStream(VCDB1);
		output = transformation.transform(null, null, VCDB1.getAbsolutePath(), in, null, request);
		assertTrue(getOutput().startsWith("<http://somewhere/JohnSmith>"));
		assertEquals(1, getOutput().lines().count());
	}

	@Test
	public void testConstructQuerySerializeTurtle()
			throws ConfigurationException, TransformationPreparationException, TransformationException,
					FileNotFoundException {
		transformation.setup(QC1_TTL, CONFIG);
		InputStream in = new FileInputStream(VCDB1);
		output = transformation.transform(null, null, VCDB1.getAbsolutePath(), in, null, request);
		// assertEquals("", getOutput());
		assertTrue(getOutput().startsWith("PREFIX rdf"));
		assertEquals(5, getOutput().lines().count());
	}
}
