package de.ulbms.scdh.seed.xc.transformer;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Just testing if endpoint is present.
 */
@QuarkusTest
public class TransformationsEndpointTest {

	@Test
	public void testTransformationsEndpoint() {
		given().when().get("/transformations").then().statusCode(200);
	}
}
