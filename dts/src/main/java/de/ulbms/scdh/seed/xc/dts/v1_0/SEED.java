package de.ulbms.scdh.seed.xc.dts.v1_0;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public class SEED {

	public static final String NAMESPACE = "https://github.com/scdh/seed-xc/dts/semantics.ttl#";

	public static final Property LOCATION = new PropertyImpl(NAMESPACE, "location");
}
