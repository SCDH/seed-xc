package de.ulbms.scdh.seed.xc.jena;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;

/**
 * The {@link Serializer} adds sufficient information for getting an RDF output format that works.
 * This includes choosing an encoding variant.
 */
@ApplicationScoped
public class Serializer {

	public static final Lang DEFAULT = Lang.N3;

	public static RDFFormat getFormat(String transformationContentType, String systemId) {
		if (transformationContentType != null) {
			Lang lang = RDFLanguages.contentTypeToLang(transformationContentType);
			if (lang.equals(Lang.NTRIPLES)) {
				return new RDFFormat(lang, RDFFormat.UTF8);
			} else if (lang.equals(Lang.TTL)) {
				return new RDFFormat(lang, RDFFormat.PRETTY);
			} else {
				return new RDFFormat(lang);
			}
		} else {
			return new RDFFormat(RDFLanguages.filenameToLang(systemId, DEFAULT));
		}
	}
}
