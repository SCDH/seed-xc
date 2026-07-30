package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class EntryEndpointTest {

	@Test
	public void testStatusWithFileSample() {
		given().when().get("/file/sample/entry").then().statusCode(200);
	}
}
