package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionEndpointTest {

	@Test
	public void testNoParams() {
		given().when().get("/collection?id=general").then().statusCode(200);
	}
}
