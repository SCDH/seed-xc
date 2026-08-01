package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StandoffEndpointTest {

	private static final String BASE = "https://dts.example.com";

	private static final File ANNOT_DIR =
			Paths.get("src", "test", "resources", "annotations").toFile();

	private static final File ANNOT_JOHN = new File(ANNOT_DIR, "John.fw.json");

	@Test
	public void testDocumentJohnXmlStatus200() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(200);
	}

	@Test
	public void testDocumentJohnXmlStatus201() {
		given().when().get("/file/sample/document/john.xml").then().statusCode(201);
	}

	@Test
	public void testJohnForwardBaseRepresentation() {
		given().multiPart("annotations", ANNOT_JOHN, "application/ld+json")
				.when()
				.post("/file/sample/oa/forward/john.xml")
				.then()
				.statusCode(405)
				.header("Allow", "");
	}
}
