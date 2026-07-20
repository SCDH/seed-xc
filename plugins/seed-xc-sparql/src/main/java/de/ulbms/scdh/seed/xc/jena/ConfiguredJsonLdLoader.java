package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A factory for {@link DocumentLoader} objects used for JSON-LD processing.
 */
@ApplicationScoped
public class ConfiguredJsonLdLoader {

	@Produces
	public static DocumentLoader createJsonLdLoader(
			@ConfigProperty(name = "jsonld-context-map", defaultValue = "empty-resource-map.json") String contextMap,
			@ConfigProperty(name = "url-read-timeout", defaultValue = "10000") int readTimeout) {

		HttpLoader httpLoader = (HttpLoader) HttpLoader.defaultInstance();
		httpLoader.fallbackContentType(MediaType.JSON);
		httpLoader.timeout(Duration.ofMillis(readTimeout));

		return new StaticDocumentLoader(
				Arrays.stream(contextMap.split(","))
						.map(String::trim)
						.map(File::new)
						.toList(),
				httpLoader,
				true);
	}
}
