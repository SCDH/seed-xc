package de.ulbms.scdh.seed.xc.dts;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.Record;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.io.*;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility for processing the collection configuration in <code>collection.json</code>.
 */
@Dependent
public class CollectionConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionConfiguration.class);

	/**
	 * Location of the collection metadata, same as for Collection endpoint.
	 */
	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.CollectionEndpoint.GRAPH", defaultValue = "collection.json")
	protected String GRAPH;

	@ConfigProperty(name = "dts-context-location-parameter", defaultValue = "context-url")
	protected String contextLocationParameter;

	@ConfigProperty(name = "dts-context-document-parameter", defaultValue = "context-document")
	protected String contextDocumentParameter;

	/**
	 * Reads the {@link RecordConfig} form the input stream and returns it.
	 * @param resourceProvider - a resource provider
	 * @param input - the input stream
	 * @param systemId - the input streams URL
	 * @param config - a configuration
	 * @param context - a context map
	 * @return - the record config
	 */
	public Uni<RecordConfig> getRecordConfig(
			ResourceProvider resourceProvider,
			Uni<InputStream> input,
			String systemId,
			Config config,
			Map<String, String> context) {
		return input.onItem().transform(inputStream -> {
			LOG.debug("getting record config from {}", systemId);
			ObjectMapper om = new ObjectMapper(new JsonFactory());
			try {
				JsonParser parser = om.createParser(inputStream);
				Record record = parser.readValueAs(Record.class);
				inputStream.close();
				if (record == null) return null;
				return record.getConfiguration();
			} catch (IOException e) {
				LOG.debug("failed to read record config from {}", e.getMessage());
				return null;
			}
		});
	}

	/**
	 * Merges the configuration from the input stream into the configuration passed in.
	 * @param resourceProvider - a resource provider
	 * @param input - the input stream
	 * @param systemId - the URI of the input stream
	 * @param defaultConfig - the configuration of the service instance
	 * @param context - a context map
	 * @param endpoint - the endpoint requesting the config
	 * @return - a configuration with the per-record configuration merged
	 */
	public Uni<Config> mergeAsync(
			ResourceProvider resourceProvider,
			Uni<InputStream> input,
			String systemId,
			final Config defaultConfig,
			Map<String, String> context,
			String endpoint) {
		return input.onItem().transform(inputStream -> {
			try {
				byte[] in = inputStream.readAllBytes();
				inputStream.close();
				return merge(in, defaultConfig, endpoint);
			} catch (IOException e) {
				try {
					inputStream.close();
				} catch (IOException ignore) {
				}
				return defaultConfig;
			}
		});
	}

	/**
	 * Merges the configuration from the input stream into the configuration passed in.
	 * @param input - the input stream
	 * @param defaultConfig - the configuration of the service instance
	 * @param endpoint - the endpoint requesting the config
	 * @return - a configuration with the per-record configuration merged
	 */
	public Config merge(byte[] input, final Config defaultConfig, String endpoint) {
		ObjectMapper om = new ObjectMapper(new JsonFactory());
		LOG.debug("merging record config in request {}, {}", endpoint, input.length);
		try (JsonParser parser = om.createParser(input)) {
			Record record = parser.readValueAs(Record.class);
			if (record == null || record.getConfiguration() == null) return defaultConfig;
			LOG.info("using record config for endpoint {}", endpoint); // stats
			Config config = clone(defaultConfig); // clone, do not overwrite the default config
			// do the merge
			mergeContext(record.getConfiguration(), config, endpoint);
			return config;
		} catch (IOException e) {
			LOG.debug("no record config from {}", e.getMessage());
			return defaultConfig;
		}
	}

	/**
	 * Returns a deep clone of the given {@link Config} object.
	 * @param defaultConfig - the configuration to be cloned
	 * @return a deep clone
	 * @throws IOException - when there's a serialization or deserialization problem
	 */
	private Config clone(Config defaultConfig) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		TokenBuffer buffer = new TokenBuffer(new ObjectMapper(), false);
		mapper.writeValue(buffer, defaultConfig);
		return mapper.readValue(buffer.asParser(), Config.class);
	}

	/**
	 * Merges per-record JSON-LD frames into the config.
	 * @param recordConfig - the record config
	 * @param config - the services default config
	 * @param endpoint - the endpoint requested
	 */
	private void mergeContext(RecordConfig recordConfig, Config config, String endpoint) {
		RecordFrames frames = recordConfig.getFrames();
		if (frames == null) return;
		if (endpoint.equals("collection")) {
			if (frames.getCollection() != null) {
				config.setContext(frames.getCollection());
			} else if (frames.getAll() != null) {
				config.setContext(frames.getAll());
			}
		} else if (endpoint.equals("navigation")) {
			if (frames.getNavigation() != null) {
				config.setContext(frames.getNavigation());
			} else if (frames.getAll() != null) {
				config.setContext(frames.getAll());
			}
		}
	}

	/**
	 * Make {@link RuntimeParameters} from the given {@link Config} and append them to the given runtime parameter
	 * (that may ne resulting from request parameters). This is a way for passing per-record config properties to
	 * transformations. It only works, if the according parameters are runtime paramter (as opposed to static
	 * parameters). Parameters from the <code>params</code> woh't get overwritten.
	 * @param params - the {@link RuntimeParameters} to append to
	 * @param config - the per-record {@link Config}
	 * @return - {@link RuntimeParameters} from <code>params</code> with parameters made from config properties appended
	 */
	public RuntimeParameters appendToParameters(RuntimeParameters params, Config config) {
		if (config == null) return params;
		if (config.getContext() != null && !params.getGlobalParameters().containsKey(contextLocationParameter)) {
			Context context = config.getContext();
			if (context.getLocation() != null) {
				params.putGlobalParametersItem(contextLocationParameter, pvOf(context.getLocation()));
			}
			if (context.getDocument() != null && !params.getGlobalParameters().containsKey(contextDocumentParameter)) {
				params.putGlobalParametersItem(contextDocumentParameter, pvOf(context.getDocument()));
			}
		}
		return params;
	}
}
