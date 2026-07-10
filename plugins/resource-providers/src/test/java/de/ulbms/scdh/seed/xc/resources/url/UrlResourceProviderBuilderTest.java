package de.ulbms.scdh.seed.xc.resources.url;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UrlResourceProviderBuilderTest {

	private static final String DOMAINS_WHITE_MS = "^.*\\.uni-muenster.de$";

	private static final String DOMAINS_WHITE_RUB = "^.*\\.ruhr-uni-bochum.de$";

	private static final String DOMAINS_WHITE_ALL = "";

	private static final String DOMAINS_BLACK_COM = ".*\\.com$";

	UrlResourceProviderBuilder builder;

	ResourceProvider provider;

	@BeforeEach
	public void createBuilder() {
		builder = new UrlResourceProviderBuilder();
	}

	@Test
	void testInPattern() {
		List<Pattern> whiteMs = List.of(Pattern.compile(DOMAINS_WHITE_MS));
		assertTrue(UrlResourceProviderBuilder.inPatterns(whiteMs, "zivgitlab.uni-muenster.de"));
		List<Pattern> all = List.of(Pattern.compile(DOMAINS_WHITE_ALL));
		assertTrue(UrlResourceProviderBuilder.inPatterns(whiteMs, "zivgitlab.uni-muenster.de"));
	}

	@Test
	void testInPatternAll() {
		List<Pattern> whiteMs = List.of(Pattern.compile(".*"));
		assertTrue(UrlResourceProviderBuilder.inPatterns(whiteMs, "zivgitlab.uni-muenster.de"));
	}

	@Test
	void testInPatternEmpty() {
		List<Pattern> whiteMs = List.of(Pattern.compile(""));
		assertFalse(UrlResourceProviderBuilder.inPatterns(whiteMs, "zivgitlab.uni-muenster.de"));
	}

	@Test
	public void testWhiteMS() throws URISyntaxException {
		builder.allowedProtocols = "https";
		builder.domainWhiteList = DOMAINS_WHITE_MS;
		builder.domainBlackList = "";
		assertDoesNotThrow(
				() -> {
					provider = builder.withBase(new URI("https://zivgitlab.uni-muenster.de"));
				},
				"zivgitlab.uni-muenster.de allowed");
		assertThrows(
				ResourceException.class,
				() -> {
					provider = builder.withBase(new URI("https://gitlab.ruhr-uni-bochum.de"));
				},
				"gitlab.ruhr-uni-bochum.de not in whitelist and thus not allowed");
		assertThrows(
				ResourceException.class,
				() -> {
					provider = builder.withBase(new URI("https://example.com/edition"));
				},
				"example.com not in whitelist and thus not allowed");
	}

	@Test
	public void testWhiteMSRUB() throws URISyntaxException {
		builder.allowedProtocols = "https";
		builder.domainWhiteList = DOMAINS_WHITE_MS + "," + DOMAINS_WHITE_RUB;
		builder.domainBlackList = "";
		assertDoesNotThrow(
				() -> {
					provider = builder.withBase(new URI("https://zivgitlab.uni-muenster.de"));
				},
				"zivgitlab.uni-muenster.de allowed");
		assertDoesNotThrow(
				() -> {
					provider = builder.withBase(new URI("https://gitlab.ruhr-uni-bochum.de"));
				},
				"gitlab.ruhr-uni-bochum.de  in whitelist and thus allowed");
		assertThrows(
				ResourceException.class,
				() -> {
					provider = builder.withBase(new URI("https://example.com/edition"));
				},
				"example.com not in whitelist and thus not allowed");
	}

	@Test
	public void testBlackComOnly() throws URISyntaxException {
		builder.allowedProtocols = "https";
		builder.domainWhiteList = "";
		builder.domainBlackList = DOMAINS_BLACK_COM;
		assertDoesNotThrow(
				() -> {
					provider = builder.withBase(new URI("https://zivgitlab.uni-muenster.de"));
				},
				"zivgitlab.uni-muenster.de allowed");
		assertDoesNotThrow(
				() -> {
					provider = builder.withBase(new URI("https://gitlab.ruhr-uni-bochum.de"));
				},
				"gitlab.ruhr-uni-bochum.de  in whitelist and thus allowed");
		assertThrows(
				ResourceException.class,
				() -> {
					provider = builder.withBase(new URI("https://example.com/edition"));
				},
				"example.com not in whitelist and thus not allowed");
	}
}
