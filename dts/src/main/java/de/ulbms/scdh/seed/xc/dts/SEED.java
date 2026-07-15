package de.ulbms.scdh.seed.xc.dts;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public class SEED {

	public static final String NAMESPACE =
			"https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/semantics/";

	public static final Property location = new PropertyImpl(NAMESPACE, "location");
}
