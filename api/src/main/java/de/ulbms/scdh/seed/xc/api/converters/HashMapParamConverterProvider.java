package de.ulbms.scdh.seed.xc.api.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

@Provider
public class HashMapParamConverterProvider implements ParamConverterProvider {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (rawType.isAssignableFrom(Map.class)) {
			return (ParamConverter<T>) new HashMapParamConverter();
		}
		return null;
	}
}
