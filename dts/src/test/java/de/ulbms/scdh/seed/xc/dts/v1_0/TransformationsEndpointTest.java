package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransformationsEndpointTest {

	private final String[] transformations = {"dts-transformations-xsl-navigation", "dts-transformations-xsl-document"};

	@Test
	public void testTransformationsEndpoint() {
		given().when().get("/transformations").then().statusCode(200);
	}

	@Test
	public void testTransformationsCompiledSize() {
		given().when()
				.get("/transformations")
				.then()
				.statusCode(200)
				.body("size()", is(greaterThan(transformations.length - 1)));
	}

	@Test
	public void testTransformationsCompiled() {
		given().when().get("/transformations").then().statusCode(200).body("$", hasItems(transformations));
	}
}
