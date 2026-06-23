package de.ulbms.scdh.seed.xc.jena;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;
import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ParameterConverterTest {

	private final ParameterizedSparqlString q1 =
			new ParameterizedSparqlString("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(?x as ?s) . ?s ?p ?o .}");

	private final ParameterizedSparqlString q2 =
			new ParameterizedSparqlString("CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { ?x } . ?s ?p ?o .}");

	ParameterConverter converter;

	@BeforeEach
	void setConverter() {
		converter = new ParameterConverter();
	}

	@Test
	public void testStringLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("hallo"), "xs:string", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"hallo\" as ?s) . ?s ?p ?o .}", q1.toString());
	}

	@Test
	public void testIntegerLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("42"), "xs:integer", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42\"^^<http://www.w3.org/2001/XMLSchema#int> as ?s) . ?s ?p ?o .}",
				q1.toString());
	}

	@Test
	public void testLongIntegerLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("42"), "xs:long", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42\"^^<http://www.w3.org/2001/XMLSchema#long> as ?s) . ?s ?p ?o .}",
				q1.toString());
	}

	@Test
	public void testFloatLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("42.0"), "xs:float", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42.0\"^^<http://www.w3.org/2001/XMLSchema#float> as ?s) . ?s ?p ?o .}",
				q1.toString());
	}

	@Test
	public void testDoubleLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("42.0"), "xs:double", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"42.0\"^^<http://www.w3.org/2001/XMLSchema#double> as ?s) . ?s ?p ?o .}",
				q1.toString());
	}

	@Test
	public void testBooleanLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("true"), "xs:boolean", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(true as ?s) . ?s ?p ?o .}", q1.toString());
	}

	@Test
	public void testUriLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("doi:911"), "xs:anyURI", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(<doi:911> as ?s) . ?s ?p ?o .}", q1.toString());
	}

	@Disabled
	@Test
	public void testDateLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("2026-06-23"), "xs:date", q1);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { BIND(42 as ?s) . ?s ?p ?o .}", q1.toString());
	}

	@Test
	public void testUntypedLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("doi:911"), null, q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				q1.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testBlankLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("doi:911"), "", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				q1.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testUnknownTypeLiteral() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf("doi:911"), "xs:private", q1);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { BIND(\"doi:911\" as ?s) . ?s ?p ?o .}",
				q1.toString(),
				"fallback to xs:string");
	}

	@Test
	public void testStringSequence() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf(List.of("hello", "world")), "xs:string*", q2);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { (\"hello\") (\"world\") } . ?s ?p ?o .}", q2.toString());
	}

	@Test
	public void testIntegerSequence() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf(List.of("1", "2")), "xs:integer*", q2);
		});
		assertEquals(
				"CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s { (\"1\"^^<http://www.w3.org/2001/XMLSchema#int>) (\"2\"^^<http://www.w3.org/2001/XMLSchema#int>) } . ?s ?p ?o .}",
				q2.toString());
	}

	@Test
	public void testEmptySequence() {
		assertDoesNotThrow(() -> {
			converter.setQueryParameter("x", pvOf(List.of()), "xs:string*", q2);
		});
		assertEquals("CONSTRUCT { ?s ?p ?o . } WHERE { VALUES ?s {  } . ?s ?p ?o .}", q2.toString());
	}

	@Test
	public void testEmptySequenceForAtLeastOne() {
		assertThrows(TransformationPreparationException.class, () -> {
			converter.setQueryParameter("x", pvOf(List.of()), "xs:string+", q2);
		});
	}

	@Test
	public void testEmptySequenceForAtMostOne() {
		assertThrows(TransformationPreparationException.class, () -> {
			converter.setQueryParameter("x", pvOf(List.of("hello", "world")), "xs:string?", q2);
		});
	}
}
