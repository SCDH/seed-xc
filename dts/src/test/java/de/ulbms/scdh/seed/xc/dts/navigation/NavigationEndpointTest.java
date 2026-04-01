package de.ulbms.scdh.seed.xc.dts.navigation;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.ArrayMatching.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NavigationEndpointTest {

	@Test
	public void testNoParams() {
		given().when().get("/navigation").then().statusCode(400);
	}

	@Test
	public void testOnlyCollection() {
		given().when().get("/navigation/asdf").then().statusCode(404);
	}

	@Test
	public void testHelloXml() {
		given().when()
				.get("/navigation?collection=file%3A%2F%2Fasdfasd%2F&down=0&" + "resource=john.xml")
				.then()
				.statusCode(200);
	}
}
