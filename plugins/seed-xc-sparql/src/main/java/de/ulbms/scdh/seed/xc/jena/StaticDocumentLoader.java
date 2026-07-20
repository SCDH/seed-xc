package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.FileLoader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.ResourceMapping;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link StaticDocumentLoader} is a {@link DocumentLoader} that returns local assets instead of remote one.
 * It is configured with a {@link ResourceMapping} that maps URIs to local assets. Its purpose is to speed up
 * JSON-LD processing with version pinned contexts, as for DTS or DCTerms.<P/>
 *
 * The loader delegated to a fallback loader when the requested URI is not in the {@link ResourceMapping}.<P/>
 *
 * The {@link ResourceMapping} is provided as a JSON file.
 */
public class StaticDocumentLoader implements DocumentLoader {

	private static final Logger LOG = LoggerFactory.getLogger(StaticDocumentLoader.class);

	private final DocumentLoader fallbackLoader;

	private final Map<URI, URI> contextMapping;

	private final boolean delegateOnError;

	/**
	 * Creates a new {@link StaticDocumentLoader} from a resource mapping given by {@link File}.
	 * @param contextMaps - the {@link List<File>} paths to the {@link ResourceMapping} JSON file
	 * @param fallbackLoader - a {@link DocumentLoader} used to handle request for non-mapped resources
	 * @param delegateOnError - whether to delegate the request to the fallback, when loading of a local asset failed.
	 */
	public StaticDocumentLoader(
			final List<File> contextMaps, final DocumentLoader fallbackLoader, boolean delegateOnError) {
		this.fallbackLoader = fallbackLoader;
		this.delegateOnError = delegateOnError;
		Map<URI, URI> assets = new HashMap<>();

		for (File contextMap : contextMaps) {
			URI contextMapUri = contextMap.toURI();
			LOG.info("using context map {}", contextMapUri);

			ObjectMapper om = new ObjectMapper(new JsonFactory());
			try {
				ResourceMapping staticContexts = om.readValue(contextMap, ResourceMapping.class);

				for (String remote : staticContexts.keySet()) {
					String local = staticContexts.get(remote).getPath();
					try {
						URI remoteUri = new URI(remote);
						URI localUri = contextMapUri.resolve(local);
						assets.put(remoteUri, localUri);
						LOG.info("using static asset {} instead of remote {}", local, remote);
					} catch (Exception e) {
						LOG.error(
								"failed to set up JSON-LD context asset for remote {} from {}: {}",
								remote,
								local,
								e.getMessage());
					}
				}
			} catch (Exception e) {
				LOG.error("failed to set up static document loader from {}: {}", contextMap, e.getMessage());
			}
			LOG.info("set up static context loader with {} assets", assets.size());
		}
		contextMapping = Map.copyOf(assets); // makes contextMapping unmodifiable
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
		if (contextMapping.containsKey(url)) {
			URI file = contextMapping.get(url);
			FileLoader fileLoader = new FileLoader();
			try {
				return fileLoader.loadDocument(file, options);
			} catch (JsonLdError e) {
				LOG.error("failed to load static asset for {}: {}", url, e.getMessage());
				if (delegateOnError) {
					return fallbackLoader.loadDocument(url, options);
				} else {
					throw new JsonLdError(e.getCode(), e.getMessage());
				}
			}
		} else {
			// delegate to fallback loader
			return fallbackLoader.loadDocument(url, options);
		}
	}
}
