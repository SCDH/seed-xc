package de.ulbms.scdh.seed.xc.api.converters;

import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

/**
 * The converter provided by this class is required for the
 * {@link RuntimeParameters} passed in as a part of multipart
 * form-data.
 */
@Provider
public class RuntimeParametersParamConverterProvider
	implements ParamConverterProvider {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T>
	getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (rawType.isAssignableFrom(RuntimeParameters.class)) {
			return (ParamConverter<T>)new RuntimeParametersParamConverter();
		}
		return null;
	}
}
