package de.ulbms.scdh.seed.xc.api.utils;

import de.ulbms.scdh.seed.xc.api.ParameterValue;
import java.util.Collection;

/**
 * A utility for conveniently making {@link ParameterValue} objects from singletons or collections.
 */
public class ParameterValueFactory {

	public static ParameterValue pvOf(String x) {
		ParameterValue pv = new ParameterValue();
		pv.add(x);
		return pv;
	}

	public static ParameterValue pvOfS(Iterable<String> xs) {
		ParameterValue pv = new ParameterValue();
		for (String x : xs) pv.add(x);
		return pv;
	}

	public static ParameterValue pvOf(Object x) {
		ParameterValue pv = new ParameterValue();
		pv.add(String.valueOf(x));
		return pv;
	}

	public static ParameterValue pvOf(Collection<? extends Object> xs) {
		ParameterValue pv = new ParameterValue();
		pv.addAll(xs.stream().map(String::valueOf).toList());
		return pv;
	}
}
