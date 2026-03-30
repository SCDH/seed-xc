package de.ulbms.scdh.seed.xc.api.converters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ulbms.scdh.seed.xc.api.Config;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * This converter is required for the {@link Config} passed
 * in as a part of multipart form-data.
 */
@Provider
public class ConfigParamConverter implements ParamConverter<Config> {

	/**
	 * {@inheritDoc}
	 */
	public Config fromString(String value) {
		try {
			ObjectMapper om = new ObjectMapper(new JsonFactory());
			Config config = om.readValue(value, Config.class);
			return config;
		} catch (JsonParseException e) {
			return null;
		} catch (JsonMappingException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString(Config config) {
		return config.toString();
	}
}
