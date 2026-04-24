package de.ulbms.scdh.seed.xc.jena;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class SerializerTest {

	@InjectMock
	HttpServerRequest request;

	@BeforeEach
	public void setup() {
		Mockito.when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("application/rdf+xml");
	}

	@Test
	public void testInfoTurtle() throws TransformationPreparationException {
		assertEquals(RDFFormat.TURTLE_PRETTY, Serializer.getFormat("text/turtle", null, request));
	}

	@Test
	public void testInfoXML() throws TransformationPreparationException {
		assertEquals(RDFFormat.RDFXML_PLAIN, Serializer.getFormat("application/rdf+xml", null, request));
	}

	@Test
	public void testNoInfo() throws TransformationPreparationException {
		assertEquals(RDFFormat.RDFXML_PLAIN, Serializer.getFormat(null, null, request));
	}

	@Test
	public void testInfoYaml() throws TransformationPreparationException {
		assertThrows(
				TransformationPreparationException.class,
				() -> Serializer.getFormat("application/rdf+yaml", null, request));
	}

	@Disabled("cannot set request to null while mocking")
	@Test
	public void testFromSystemId() throws TransformationPreparationException {
		assertEquals(RDFFormat.RDFXML_PLAIN, Serializer.getFormat(null, "data.rdf", null));
	}

	@Test
	public void testJsonLD() throws TransformationPreparationException {
		assertEquals(
				Lang.JSONLD11,
				Serializer.getFormat("application/ld+json", null, request).getLang());
	}
}
