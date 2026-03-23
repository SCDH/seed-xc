package de.ulbms.scdh.seed.xc.dts.navigation;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.ArrayMatching.*;

import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
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
		given()
			.when()
			.get("/navigation/asdf?resource=hello.xml")
			.then()
			.statusCode(200);
	}
}
