package de.ulbms.scdh.seed.xc.dts;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * This provides convenient way to use DTS's RDF properties programmatically.<P/>
 *
 * Naming Conventions are the same as for RDF:
 * - starting with small letter: RDF property
 * - starting with capital letter: RDF class
 * - all caps: Java constants that do not represent RDF properties or resources.
 */
public class DTS {

	public static final String VERSION = "1.0";

	public static final String NAMESPACE = "https://w3id.org/dts/api#";

	public static final Property requested = new PropertyImpl(NAMESPACE, "requested");

	public static final Resource Resource = new ResourceImpl(NAMESPACE, "Resource");

	public static final Resource Collection = new ResourceImpl(NAMESPACE, "Collection");

	public static final Property dtsVersion = new PropertyImpl(NAMESPACE, "dtsVersion");

	public static final Property title = new PropertyImpl(NAMESPACE, "title");
}
