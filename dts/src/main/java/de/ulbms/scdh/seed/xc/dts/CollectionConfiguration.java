package de.ulbms.scdh.seed.xc.dts;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.Record;
import de.ulbms.scdh.seed.xc.api.RecordConfig;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
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
}
