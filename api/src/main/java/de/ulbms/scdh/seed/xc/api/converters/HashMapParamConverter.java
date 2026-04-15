package de.ulbms.scdh.seed.xc.api.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class HashMapParamConverter implements ParamConverter<HashMap<String, String>> {

	private static final Logger LOG = LoggerFactory.getLogger(HashMapParamConverter.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HashMap<String, String> fromString(String value) {
		LOG.info("converting '{}' to Map<String,String>", value);
		/* TODO */
		return new HashMap<String, String>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(HashMap<String, String> value) {
		StringBuffer rc = new StringBuffer();
		value.forEach((k, v) -> {
			rc.append(k + "[" + v + "]");
		});
		return rc.toString();
	}
}
