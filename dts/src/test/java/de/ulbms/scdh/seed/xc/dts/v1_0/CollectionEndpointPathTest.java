package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionEndpointPathTest {

	@Test
	public void testStatusGeneral() {
		given().when().get("/collection/general").then().statusCode(200);
	}

	@Test
	public void testStatusDefault() {
		given().when().get("/collection/").then().statusCode(200);
	}

	@Test
	public void testStatusUnknown() {
		given().when().get("/collection/unknown").then().statusCode(404);
	}
}
