package de.ulbms.scdh.seed.xc.transformations;

import de.ulbms.scdh.seed.xc.api.Transformation;
import java.util.Set;

/**
 * The {@link TransformationMap} is a registry of all the
 * transformations available on the web service. It maps
 * transformation IDs to {@link Transformation} objects.
 *
 * Implementations are {@link ApplicationScoped}, because there is
 * only one instance during the services lifecycle. This is what we
 * want when we want to compile a stylesheet only once and then use it
 * throughout the services lifecycle.
 */
public interface TransformationMap {

	/**
	 * Returns true if and only if the transformation ID given as
	 * argument is registered in the service.
	 *
	 * @param transformationId  the ID of the transformation
	 * @return {@link boolean}
	 */
	boolean containsKey(String transformationId);

	/**
	 * Returns the compiled {@link Transformation} with the given
	 * identifier or <code>null</code> if there's no such
	 * transformation in the registry. Note, that a compiled
	 * transformation is one, that is ready for calling one of its
	 * <code>transform()</code> methods. The setup of the
	 * transformation is handled by the {@link TransformationMap}
	 * already.
	 *
	 * @param transformationId  the ID of the transformation
	 * @return {@link Transformation} or <code>null</code>
	 */
	Transformation get(String transformationId);

	/**
	 * Returns a {@link Set} view of the transformation identifiers of
	 * this transformation map.
	 *
	 * @return a {@link Set} view of the transformation identifiers
	 */
	Set<String> keySet();
}
