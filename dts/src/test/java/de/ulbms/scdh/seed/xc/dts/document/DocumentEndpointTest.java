package de.ulbms.scdh.seed.xc.dts.document;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class DocumentEndpointTest {

	@Test
	public void testNoParams() {
		given().when().get("/document").then().statusCode(400);
	}

	@Test
	public void testOnlyCollection() {
		given().when().get("/document/asdf").then().statusCode(404);
	}

	@Test
	public void testJohnXml200() {
		given().when().get("/document?resource=john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnXmlStartEnd() {
		given().when()
				.get("/document?resource=john.xml&start=eins&end=zwei")
				.then()
				.statusCode(500);
	}

	// TODO
	@Disabled
	@Test
	public void testJohnXmlRef() {
		given().when().get("/document?resource=john.xml&start=eins").then().statusCode(500);
	}
}
