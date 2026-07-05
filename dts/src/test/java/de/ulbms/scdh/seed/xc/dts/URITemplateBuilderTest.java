package de.ulbms.scdh.seed.xc.dts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class URITemplateBuilderTest {

	URITemplateBuilder templateBuilder;

	@BeforeEach
	public void setupTemplateBuilder() {
		templateBuilder = new URITemplateBuilder();
	}

	@Test
	void collectionTest() {
		String endpoint = "collection";
		assertEquals(
				"https://example.com/dts/collection/general{?nav}",
				templateBuilder.resourceTemplate("https://example.com/dts/collection/general", endpoint));
		// from navigation
		assertEquals(
				"https://example.com/dts/collection/john.xml{?nav}",
				templateBuilder.resourceTemplate("https://example.com/dts/navigation/john.xml", endpoint));
		assertEquals(
				"https://example.com/dts/collection/john.xml{?nav}",
				templateBuilder.resourceTemplate(
						"https://example.com/dts/navigation/john.xml?tree=chapter&down=1", endpoint));

		assertEquals(
				"https://example.com/dts/collection/gospels%2Fjohn{?nav}",
				templateBuilder.resourceTemplate("https://example.com/dts/document/gospels%2Fjohn", endpoint));
		assertEquals(
				"https://example.com/dts/collection/gospels%2Fjohn{?nav}",
				templateBuilder.resourceTemplate(
						"https://example.com/dts/document/gospels%2Fjohn?tree=chapter,ref=3", endpoint));
	}

	@Test
	void documentTest() {
		String endpoint = "document";
		String tpl = URITemplateBuilder.THIS_DOCUMENT_TEMPLATE;
		assertEquals(
				"https://example.com/dts/document/general" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/collection/general", endpoint));
		// from navigation
		assertEquals(
				"https://example.com/dts/document/john.xml" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/navigation/john.xml", endpoint));
		assertEquals(
				"https://example.com/dts/document/john.xml" + tpl,
				templateBuilder.resourceTemplate(
						"https://example.com/dts/navigation/john.xml?tree=chapter&down=1", endpoint));

		assertEquals(
				"https://example.com/dts/document/gospels%2Fjohn" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/document/gospels%2Fjohn", endpoint));
		assertEquals(
				"https://example.com/dts/document/gospels%2Fjohn" + tpl,
				templateBuilder.resourceTemplate(
						"https://example.com/dts/document/gospels%2Fjohn?tree=chapter,ref=3", endpoint));
	}

	@Test
	void navigationTest() {
		String endpoint = "navigation";
		String tpl = URITemplateBuilder.THIS_NAVIGATION_TEMPLATE;
		assertEquals(
				"https://example.com/dts/navigation/general" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/collection/general", endpoint));
		// from navigation
		assertEquals(
				"https://example.com/dts/navigation/john.xml" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/navigation/john.xml", endpoint));
		assertEquals(
				"https://example.com/dts/navigation/john.xml" + tpl,
				templateBuilder.resourceTemplate(
						"https://example.com/dts/navigation/john.xml?tree=chapter&down=1", endpoint));

		assertEquals(
				"https://example.com/dts/navigation/gospels%2Fjohn" + tpl,
				templateBuilder.resourceTemplate("https://example.com/dts/document/gospels%2Fjohn", endpoint));
		assertEquals(
				"https://example.com/dts/navigation/gospels%2Fjohn" + tpl,
				templateBuilder.resourceTemplate(
						"https://example.com/dts/document/gospels%2Fjohn?tree=chapter,ref=3", endpoint));
	}
}
