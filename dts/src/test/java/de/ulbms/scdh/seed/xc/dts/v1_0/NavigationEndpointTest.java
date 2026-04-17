package de.ulbms.scdh.seed.xc.dts.v1_0;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

import de.ulbms.scdh.seed.xc.dts.model.CitableUnit;
import de.ulbms.scdh.seed.xc.dts.model.Navigation;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
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
	public void testJohnXml200() {
		given().when().get("/navigation?resource=john.xml").then().statusCode(200);
	}

	@Test
	public void testJohnXmlResponseBodyHasLODContext() {
		given().when()
				.get("/navigation?resource=john.xml")
				.then()
				.statusCode(200)
				.body("@context", notNullValue());
	}

	@Test
	public void testJohnXmlResponseBodyIsNavigationObject() {
		given().when()
				.get("/navigation?resource=john.xml")
				.then()
				.extract()
				.body()
				.as(Navigation.class);
	}

	@Test
	public void testJohnXmlResponseBodyHasCitationTrees() {
		Navigation body = given().when()
				.get("/navigation?resource=john.xml")
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
				.get("/navigation?resource=john.xml")
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
		given().when().get("/navigation?resource=john.xml&down=0").then().statusCode(400);
	}

	@Test
	public void testJohnXmlResponseBodyDown0WithRefMembers() {
		Navigation body = given().when()
				.get("/navigation?resource=john.xml&down=0&ref=John")
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
				.get("/navigation?resource=john.xml&down=0&start=John:1:2&end=John:1:4")
				.then()
				.statusCode(400);
	}

	@Test
	public void testJohnXmlResponseBodyDown1Members() {
		Navigation body = given().when()
				.get("/navigation?resource=john.xml&down=1")
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
				.get("/navigation?resource=john.xml&down=2")
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
}
