package de.ulbms.scdh.seed.xc.dts.document;

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
public class DocumentEndpointJohnTest {

	@TestHTTPEndpoint(DocumentEndpoint.class)
	@TestHTTPResource("?resource=john.xml")
	URL url;

	@Test
	public void testJohn() throws IOException {
		InputStream in = url.openStream();
		String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(contents.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}
}
