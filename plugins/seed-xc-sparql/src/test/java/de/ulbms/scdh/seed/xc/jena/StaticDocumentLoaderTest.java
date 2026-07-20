package de.ulbms.scdh.seed.xc.jena;

import static org.junit.jupiter.api.Assertions.*;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StaticDocumentLoaderTest {

	private static final List<File> EMPTY_MAP =
			List.of(Paths.get("src", "test", "resources", "context-map.json").toFile());

	private static final List<File> GOOD_MAP =
			List.of(Paths.get("src", "test", "resources", "context-map.json").toFile());

	DocumentLoader loader;

	Document result;

	private static final JsonLdErrorCode FALLBACK_CODE = JsonLdErrorCode.INVALID_FRAME;

	private final DocumentLoader fallback = new DocumentLoader() {
		@Override
		public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
			// throws an error, that may be semantically wrong but can be distinguished
			throw new JsonLdError(FALLBACK_CODE);
		}
	};

	DocumentLoaderOptions options = new DocumentLoaderOptions();

	@Test
	public void testWithEmpty() {
		loader = new StaticDocumentLoader(EMPTY_MAP, fallback, false);
		assertThrows(JsonLdError.class, () -> loader.loadDocument(new URI("doi:911"), options));
	}

	@Test
	public void testPresent() {
		loader = new StaticDocumentLoader(GOOD_MAP, fallback, false);
		assertDoesNotThrow(
				() -> loader.loadDocument(new URI("https://example.com/context/dcterms/nest.json"), options));
	}

	@Test
	public void testNotAvailableWithoutDelegation() throws URISyntaxException {
		loader = new StaticDocumentLoader(GOOD_MAP, fallback, false);
		assertThrows(JsonLdError.class, () -> loader.loadDocument(new URI("https://example.com/broken.json"), options));
		try {
			result = loader.loadDocument(new URI("https://example.com/broken.json"), options);
		} catch (JsonLdError e) {
			assertEquals(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e.getCode(), "thrown by static loader");
		}
	}

	@Test
	public void testNotAvailableWithDelegation() throws URISyntaxException {
		loader = new StaticDocumentLoader(GOOD_MAP, fallback, true);
		assertThrows(JsonLdError.class, () -> loader.loadDocument(new URI("https://example.com/broken.json"), options));
		try {
			result = loader.loadDocument(new URI("https://example.com/broken.json"), options);
		} catch (JsonLdError e) {
			assertEquals(FALLBACK_CODE, e.getCode(), "thrown by fallback loader");
		}
	}
}
