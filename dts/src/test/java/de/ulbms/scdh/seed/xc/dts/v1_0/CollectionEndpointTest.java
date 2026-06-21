package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionEndpointTest {

	@Test
	public void testStatusGeneral() {
		given().when().get("/collection?id=http://example.com/general").then().statusCode(200);
	}

	@TestHTTPEndpoint(CollectionEndpoint.class)
	@TestHTTPResource("?id=general")
	URL urlGeneral;

	@Test
	public void testGeneral() throws IOException {
		try (InputStream in = urlGeneral.openStream()) {
			String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertEquals("", contents);
		}
	}
}
