package de.ulbms.scdh.seed.xc.api.converters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * This converter is required for the {@link RuntimeParameters} passed
 * in as a part of multipart form-data.
 */
@Provider
public class RuntimeParametersParamConverter
	implements ParamConverter<RuntimeParameters> {

	/**
	 * {@inheritDoc}
	 */
	public RuntimeParameters fromString(String value) {
		try {
			ObjectMapper om = new ObjectMapper(new JsonFactory());
			RuntimeParameters parameters =
				om.readValue(value, RuntimeParameters.class);
			return parameters;
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
	public String toString(RuntimeParameters parameters) {
		return parameters.toString();
	}
}
