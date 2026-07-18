package de.ulbms.scdh.seed.xc.dts;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.Record;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import java.io.IOException;
import java.io.InputStream;
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
			LOG.info("getting record config from {}", systemId);
			ObjectMapper om = new ObjectMapper(new JsonFactory());
			try {
				JsonParser parser = om.createParser(inputStream);
				Record record = parser.readValueAs(Record.class);
				inputStream.close();
				if (record == null) return null;
				return record.getConfiguration();
			} catch (IOException e) {
				LOG.info("failed to read record config from {}", e.getMessage());
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
	public Uni<Config> merge(
			ResourceProvider resourceProvider,
			Uni<InputStream> input,
			String systemId,
			final Config defaultConfig,
			Map<String, String> context,
			String endpoint) {
		return input.onItem().transform(inputStream -> {
			LOG.info("getting record config from {}", systemId);
			ObjectMapper om = new ObjectMapper(new JsonFactory());
			try {
				JsonParser parser = om.createParser(inputStream);
				Record record = parser.readValueAs(Record.class);
				inputStream.close();
				if (record == null || record.getConfiguration() == null) return defaultConfig;
				Config config = clone(defaultConfig); // clone, in order to not overwrite the default config
				// do the merge
				mergeContext(record.getConfiguration(), config, endpoint);
				return config;
			} catch (IOException e) {
				LOG.info("failed to read record config from {}", e.getMessage());
				try {
					inputStream.close();
				} catch (IOException ignored) {
				}
				return defaultConfig;
			}
		});
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
}
