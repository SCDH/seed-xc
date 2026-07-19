package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.dts.URITemplateBuilder;
import de.ulbms.scdh.seed.xc.dts.model.CitableUnit;
import de.ulbms.scdh.seed.xc.dts.model.Navigation;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NavigationEndpointTest {

	// legacy ?direct parameters are simple ignored

	private static final String BASE = "http://example.com/"; // "http%3A%2F%2Fexample.com%2F";

	@Disabled
	@Test
	public void testNoParams() {
		given().when().get("/file/sample/navigation").then().statusCode(400);
	}

	@Test
	public void testOnlyCollection() {
		given().when().get("/file/sample/navigation/asdf").then().statusCode(404);
	}

	@Test
	public void testJohnXmlDirectTrue200() {
		given().when()
				.get("/file/sample/navigation/john.xml?direct=true")
				.then()
				.statusCode(200);
	}

	@Disabled
	@Test
	public void testJohnXmlDirectFlag200() {
		given().when().get("/file/sample/navigation/john.xml?direct").then().statusCode(200);
	}

	@Test
	public void testJohnXmlIndirect200() {
		given().when().get("/file/sample/navigation/john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnXmlResponseBodyHasLODContext() {
		given().when()
				.get("/file/sample/navigation/john.xml?direct=true")
				.then()
				.statusCode(200)
				.body("@context", notNullValue());
	}

	@Test
	public void testJohnXmlResponseBodyIsNavigationObject() {
		given().when()
				.get("/file/sample/navigation/john.xml?direct=true")
				.then()
				.extract()
				.body()
				.as(Navigation.class);
	}

	@Test
	public void testJohnXmlIndirectResponseBodyIsNavigationObject() {
		given().when()
				.get("/file/sample/navigation/john.xml")
				.then()
				.extract()
				.body()
				.as(Navigation.class);
	}

	@Test
	public void testJohnXmlResponseBodyHasCitationTrees() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml?direct=true")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		assertThat(body.getResource().getCitationTrees().size()).isEqualTo(8);
		// default has no name
		assertThat(body.getResource().getCitationTrees().get(0).getIdentifier()).isNull();
		assertThat(body.getResource().getCitationTrees().get(1).getIdentifier()).isEqualTo("wadm");
	}

	@Test
	public void testJohnXmlResponseBodyDefaultTreeMembers() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml?direct=true")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertThat(body.getMember().size()).isEqualTo(9);
		assertThat(body.getMember().stream().map(CitableUnit::getCiteType).toList())
				.isEqualTo(List.of("book", "chapter", "verse", "verse", "verse", "verse", "verse", "verse", "verse"));
	}

	@Test
	public void testJohnXmlDown0() {
		given().when()
				.get("/file/sample/navigation/john.xml?down=0&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testJohnXmlResponseBodyDown0WithRefMembers() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml?down=0&ref=John&direct=true")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertThat(body.getMember().size()).isEqualTo(1);
		assertThat(body.getMember().stream().map(CitableUnit::getCiteType).toList())
				.isEqualTo(List.of("book"));
	}

	@Test
	public void testJohnXmlDown0WithStartEnd() {
		given().when()
				.get("/file/sample/navigation/john.xml?down=0&start=John:1:2&end=John:1:4&direct=true")
				.then()
				.statusCode(400);
	}

	@Test
	public void testJohnXmlResponseBodyDown1Members() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml?down=1&direct=true")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertThat(body.getMember().size()).isEqualTo(1);
		assertThat(body.getMember().stream().map(CitableUnit::getCiteType).toList())
				.isEqualTo(List.of("book"));
	}

	@Test
	public void testJohnXmlResponseBodyDown2Members() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml?down=2&direct=true")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertThat(body.getMember().size()).isEqualTo(2);
		assertThat(body.getMember().stream().map(CitableUnit::getCiteType).toList())
				.isEqualTo(List.of("book", "chapter"));
	}

	@Test
	public void testUriTemplates() {
		Navigation body = given().when()
				.get("/file/sample/navigation/john.xml")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertNotNull(body.getResource().getCollection(), "collection URI template must be present");
		assertTrue(
				body.getResource()
						.getCollection()
						.endsWith("/collection/john.xml" + URITemplateBuilder.THIS_COLLECTION_TEMPLATE),
				"collection URI template");
		assertNotNull(body.getResource().getNavigation(), "navigation URI template must be present");
		assertTrue(
				body.getResource()
						.getNavigation()
						.endsWith("/navigation/john.xml" + URITemplateBuilder.THIS_NAVIGATION_TEMPLATE),
				"navigation URI template");
		assertNotNull(body.getResource().getDocument(), "document URI template must be present");
		assertTrue(
				body.getResource()
						.getDocument()
						.endsWith("/document/john.xml" + URITemplateBuilder.THIS_DOCUMENT_TEMPLATE),
				"document URI template");
	}

	@Test
	public void testRecordConfig() {
		Navigation body = given().when()
				.get("/file/other/navigation/john.xml")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.as(Navigation.class);
		// no members because tree not selected
		assertNotNull(body.getAtContext());
		assertEquals(
				"https://dtsapi.org/specifications/context/0.0rc0.json",
				body.getAtContext().toString());
	}
}
