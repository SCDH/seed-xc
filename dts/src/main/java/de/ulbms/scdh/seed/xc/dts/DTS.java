package de.ulbms.scdh.seed.xc.dts;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

/**
 * This provides convenient way to use DTS's RDF properties programmatically.
 */
public class DTS {

	public static final String NAMESPACE = "https://w3id.org/dts/api#";

	public static final Property Requested = new PropertyImpl(NAMESPACE, "requested");

	public static final Property Resource = new PropertyImpl(NAMESPACE, "Resource");

	public static final Property Collection = new PropertyImpl(NAMESPACE, "Collection");

	public static final String DTS_VERSION = "1.0";

	public static final Property DtsVersion = new PropertyImpl(NAMESPACE, "dtsVersion");

	public static final Property Title = new PropertyImpl(NAMESPACE, "title");
}
