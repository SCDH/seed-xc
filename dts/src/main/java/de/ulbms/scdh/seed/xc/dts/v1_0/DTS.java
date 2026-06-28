package de.ulbms.scdh.seed.xc.dts.v1_0;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

/**
 * This provides convenient way to use DTS's RDF properties programmatically.
 */
public class DTS {

	public static final String NAMESPACE = "https://w3id.org/dts/api#";

	public static final Property Resource = new PropertyImpl(NAMESPACE, "Resource");

	public static final Property Collection = new PropertyImpl(NAMESPACE, "Collection");
}
