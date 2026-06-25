package de.ulbms.scdh.seed.xc.jena;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;
import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ParameterInjectorTest {

	private final ParameterizedSparqlString q1 =
			new ParameterizedSparqlString("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(?x as ?s) . ?s ?p ?o .}");

	private final ParameterizedSparqlString q1m =
			new ParameterizedSparqlString("CONSTRUCT { ?x ?p ?o . } WHERE { ?x ?p ?o .}");

	private final ParameterizedSparqlString q2 =
			new ParameterizedSparqlString("CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { ?x } . ?s ?p ?o .}");

	private final ParameterizedSparqlString q2m = new ParameterizedSparqlString(
			"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { ?x } . ?s ?p ?o . WHERE { VALUES ?O { ?x } ?s ?p ?O .}}");

	private final ParameterizedSparqlString q2s = new ParameterizedSparqlString(
			"CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . OPTIONAL { SELECT ?spec (COALESCE(?xi, \"empty\") as ?xv) VALUES ?xi { ?x } ?s ?a ?xi. }}");

	ParameterizedSparqlString result;

	ParameterInjector converter;

	@BeforeEach
	void setConverter() {
		converter = new ParameterInjector();
	}

	@Test
	public void testStringLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("hallo"), "xs:string", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"hallo\" as ?s) . ?s ?p ?o .}", result.toString());
	}

	@Test
	public void testIntegerLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("42"), "xs:integer", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42\"^^<http://www.w3.org/2001/XMLSchema#int> as ?s) . ?s ?p ?o .}",
				result.toString());
	}

	@Test
	public void testLongIntegerLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("42"), "xs:long", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42\"^^<http://www.w3.org/2001/XMLSchema#long> as ?s) . ?s ?p ?o .}",
				result.toString());
	}

	@Test
	public void testFloatLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("42.0"), "xs:float", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42.0\"^^<http://www.w3.org/2001/XMLSchema#float> as ?s) . ?s ?p ?o .}",
				result.toString());
	}

	@Test
	public void testDoubleLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("42.0"), "xs:double", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42.0\"^^<http://www.w3.org/2001/XMLSchema#double> as ?s) . ?s ?p ?o .}",
				result.toString());
	}

	@Test
	public void testBooleanLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("true"), "xs:boolean", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(true as ?s) . ?s ?p ?o .}", result.toString());
	}

	@Test
	public void testUriLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("doi:911"), "xs:anyURI", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(<doi:911> as ?s) . ?s ?p ?o .}", result.toString());
	}

	@Disabled
	@Test
	public void testDateLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("2026-06-23"), "xs:date", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(42 as ?s) . ?s ?p ?o .}", result.toString());
	}

	@Test
	public void testUntypedLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("doi:911"), null, q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				result.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testBlankLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("doi:911"), "", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				result.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testUnknownTypeLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("doi:911"), "xs:private", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				result.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testInMultiPlacesUriLiteral() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf("doi:911"), "xs:anyURI", q1m);
		});
		assertEquals("CONSTRUCT { <doi:911> ?p ?o . } WHERE { <doi:911> ?p ?o .}", result.toString());
	}

	@Test
	public void testStringSequence() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of("hello", "world")), "xs:string*", q2);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { \"hello\" \"world\" } . ?s ?p ?o .}", result.toString());
	}

	@Test
	public void testIntegerSequence() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of("1", "2")), "xs:integer*", q2);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { \"1\"^^<http://www.w3.org/2001/XMLSchema#int> \"2\"^^<http://www.w3.org/2001/XMLSchema#int> } . ?s ?p ?o .}",
				result.toString());
	}

	@Test
	public void testEmptySequence() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of()), "xs:string*", q2);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { } . ?s ?p ?o .}", result.toString());
	}

	@Test
	public void testEmptySequenceForAtLeastOne() {
		assertThrows(TransformationPreparationException.class, () -> {
			result = converter.setQueryParameter("x", pvOf(List.of()), "xs:string+", q2);
		});
	}

	@Test
	public void testEmptySequenceForAtMostOne() {
		assertThrows(TransformationPreparationException.class, () -> {
			result = converter.setQueryParameter("x", pvOf(List.of("hello", "world")), "xs:string?", q2);
		});
	}

	@Test
	public void testMultiIntegerSequence() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of("doi:911")), "xs:anyURI?", q2m);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { <doi:911> } . ?s ?p ?o . WHERE { VALUES ?O { <doi:911> } ?s ?p ?O .}}",
				result.toString());
	}

	// @Disabled("injecting VALUES into subqueries seem to be not working")
	@Test
	public void testSubQuerySequence() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of("doi:911", "doi:112")), "xs:anyURI*", q2s);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . OPTIONAL { SELECT ?spec (COALESCE(?xi, \"empty\") as ?xv) VALUES ?xi { <doi:911> <doi:112> } ?s ?a ?xi. }}",
				result.toString());
	}

	@Test
	public void testSingletonIntoValues() {
		assertDoesNotThrow(() -> {
			result = converter.setQueryParameter("x", pvOf(List.of("doi:911")), "xs:anyURI", q2s);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . OPTIONAL { SELECT ?spec (COALESCE(?xi, \"empty\") as ?xv) VALUES ?xi { <doi:911> } ?s ?a ?xi. }}",
				result.toString());
	}
}
