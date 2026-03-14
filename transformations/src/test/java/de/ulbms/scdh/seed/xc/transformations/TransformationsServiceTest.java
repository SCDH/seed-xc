package de.ulbms.scdh.seed.xc.transformations;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.ArrayMatching.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransformationsServiceTest {

	private final String[] transformations = {"param-integer", "identity",
											  "tagsoup"};

	@Test
	public void testTransformationsEndpoint() {
		given().when().get("/transformations").then().statusCode(200);
	}

	@Test
	public void testTransformationsCompiledSize() {
		given()
			.when()
			.get("/transformations")
			.then()
			.statusCode(200)
			.body("size()", is(transformations.length));
	}

	@Test
	public void testTransformationsCompiled() {
		given()
			.when()
			.get("/transformations")
			.then()
			.statusCode(200)
			.body("$", hasItems(transformations));
	}
}
