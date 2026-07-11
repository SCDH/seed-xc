package de.ulbms.scdh.seed.xc.resources.url;

import static org.junit.jupiter.api.Assertions.*;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UrlResourceProviderBuilderTest {

	private static final URI RESOURCES = Paths.get("src", "test", "resource").toUri();

	private static final String DOMAINS_WHITE_MS = "^.*\\.uni-muenster.de$";

	private static final String DOMAINS_WHITE_RUB = "^.*\\.ruhr-uni-bochum.de$";

	private static final String DOMAINS_WHITE_ALL = "";

	private static final String DOMAINS_BLACK_COM = ".*\\.com$";

	UrlResourceProviderBuilder builder;

	ResourceProvider provider;

	UrlConfig config;

	@BeforeEach
	public void createBuilder() {
		config = new UrlConfig();
		config.allowedProtocols = "file";
		config.domainWhiteList = ".*";
		config.domainBlackList = "asdf";
		config.allowedFilePath = RESOURCES.getSchemeSpecificPart();
		builder = new UrlResourceProviderBuilder();
		builder.config = config;
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
		config.allowedProtocols = "https";
		config.domainWhiteList = DOMAINS_WHITE_MS;
		config.domainBlackList = "";
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
		config.allowedProtocols = "https";
		config.domainWhiteList = DOMAINS_WHITE_MS + "," + DOMAINS_WHITE_RUB;
		config.domainBlackList = "";
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
		config.allowedProtocols = "https";
		config.domainWhiteList = "";
		config.domainBlackList = DOMAINS_BLACK_COM;
		builder.config = config;
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
