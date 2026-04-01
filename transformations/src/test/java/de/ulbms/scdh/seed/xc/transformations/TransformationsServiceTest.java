package de.ulbms.scdh.seed.xc.transformations;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.ArrayMatching.*;

import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransformationsServiceTest {

	private final String[] transformations = {"param-integer", "identity", "tagsoup"};

	@Test
	public void testTransformationsEndpoint() {
		given().when().get("/transformations").then().statusCode(200);
	}

	@Test
	public void testTransformationsCompiledSize() {
		given().when().get("/transformations").then().statusCode(200).body("size()", is(transformations.length));
	}

	@Test
	public void testTransformationsCompiled() {
		given().when().get("/transformations").then().statusCode(200).body("$", hasItems(transformations));
	}

	@Test
	public void testTransformIdentityInfoGet() {
		given().when()
				.get("/transformations/identity/info")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("ident", is("identity"))
				.body("class", is(SaxonXslTransformation.TRANSFORMATION_TYPE))
				.body("location", endsWith("id.xsl"));
	}

	@Test
	public void testTransformIdentityParametersGet() {
		given().when()
				.get("/transformations/identity/parameters")
				.then()
				.statusCode(200)
				.body("size()", is(0));
	}

	@Test
	public void testTransformParamIntegerInfoGet() {
		given().when()
				.get("/transformations/param-integer/info")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("ident", is("param-integer"))
				.body("class", is(SaxonXslTransformation.TRANSFORMATION_TYPE))
				.body("location", endsWith("param-integer.xsl"));
	}

	@Test
	public void testTransformParamIntegerParametersGet() {
		given().when()
				.get("/transformations/param-integer/parameters")
				.then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("size()", is(1))
				.body("times.occurrenceIndicator", is(""))
				.body("times.itemType", is("xs:integer"))
				.body("times.underlyingDeclaredType", is("xs:integer"))
				.body("times.isRequired", is(true));
	}
}
