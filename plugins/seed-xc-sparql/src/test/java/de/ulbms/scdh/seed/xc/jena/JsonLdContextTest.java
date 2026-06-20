package de.ulbms.scdh.seed.xc.jena;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.Context;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationMap;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// @QuarkusTest
public class JsonLdContextTest {

	private static final File CONFIG =
			Paths.get("src", "test", "resources", "config.json").toFile();

	private static final File FRAME = Paths.get("src", "test", "resources", "META-INF", "resources", "person.json")
			.toFile();

	// exposes test resource under src/test/resources/META-INF/resources/person.json
	// @TestHTTPResource("person.json")
	// URL personContextUrl;

	static final TransformationInfo PERSON_FROM_LOCATION;

	static {
		TransformationInfo info = new TransformationInfo();
		info.setPropertyClass(SparqlConstruct.TRANSFORMATION_TYPE);
		Context context = new Context();
		context.setLocation(FRAME.toURI());
		info.setContext(context);
		PERSON_FROM_LOCATION = info;
	}

	JsonLdContext cf;
	TransformationMap transformations;

	@BeforeEach
	void createJsonLdFactory() {
		cf = new JsonLdContext();
	}

	@BeforeEach
	void parseConfig() throws IOException {
		ObjectMapper om = new ObjectMapper(new JsonFactory());
		transformations = om.readValue(CONFIG, TransformationMap.class);
	}

	//    @Test
	//    public void testPersonContextUrlAccessible() throws IOException {
	//        try (InputStream in = personContextUrl.openStream()) {
	//            String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
	//            assertTrue(contents.contains("@context"));
	//        }
	//    }

	@Test
	public void testContextFromPersonContextUrl() throws TransformationPreparationException {
		assertTrue(cf.providesContext(PERSON_FROM_LOCATION));
		assertTrue(
				cf.getContext(PERSON_FROM_LOCATION).getJsonContent().toString().contains("@context"));
		assertTrue(
				cf.getContext(PERSON_FROM_LOCATION).getJsonContent().toString().contains("somewhere"));
	}

	@Test
	public void testContextFromDocument() throws TransformationPreparationException {
		TransformationInfo info = transformations.get("document-context-gh");
		assertTrue(cf.providesContext(info));
		assertNotNull(cf.getContext(info));
		assertTrue(cf.getContext(info).getJsonContent().toString().contains("@context"));
		assertTrue(cf.getContext(info).getJsonContent().toString().contains("modules/1.0rc1.json"));
		assertTrue(cf.getContext(info).getJsonContent().toString().contains("modules/dct-nest.json"));
	}
}
