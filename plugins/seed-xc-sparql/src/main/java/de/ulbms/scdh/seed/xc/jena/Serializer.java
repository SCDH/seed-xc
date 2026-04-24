package de.ulbms.scdh.seed.xc.jena;

import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
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

	/**
	 * This implementation prefers the content type declared for the transformation (first argument)
	 * over the accept header of the request. If none is given, the content type is guessed from the
	 * processed file extension. <code>DEFAULT</code> is returned as fallback.
	 *
	 * @param transformationContentType - the content type declared for the transformation
	 * @param systemId - the name of the request file
	 * @param request - HTTP request with accept headers
	 * @return the RDF content type
	 */
	public static RDFFormat getFormat(String transformationContentType, String systemId, HttpServerRequest request)
			throws TransformationPreparationException {
		try {
			if (transformationContentType != null) {
				Lang lang = RDFLanguages.contentTypeToLang(transformationContentType);
				return getFormatVariant(lang, "utf-8");
			} else if (request != null & request.getHeader(HttpHeaders.ACCEPT) != null) {
				Lang lang = RDFLanguages.contentTypeToLang(request.getHeader(HttpHeaders.ACCEPT));
				return getFormatVariant(lang, request.getHeader(HttpHeaders.ACCEPT_CHARSET));
			} else {
				return new RDFFormat(RDFLanguages.filenameToLang(systemId, DEFAULT));
			}
		} catch (Exception e) {
			throw new TransformationPreparationException(
					"unknown RDF format: " + transformationContentType + " " + request.getHeader(HttpHeaders.ACCEPT));
		}
	}

	/**
	 * This adds missing information to get a format variant.
	 *
	 * @param lang - the basic format as {@link Lang}
	 * @param charset - the charset requested
	 * @return - the fully specified format for which a formatter exists
	 */
	protected static RDFFormat getFormatVariant(Lang lang, String charset) {
		if (lang.equals(Lang.NTRIPLES)) {
			return new RDFFormat(lang, RDFFormat.UTF8);
		} else if (lang.equals(Lang.TTL)) {
			return new RDFFormat(lang, RDFFormat.PRETTY);
		} else if (lang.equals(Lang.RDFXML)) {
			return new RDFFormat(lang, RDFFormat.PLAIN);
		} else {
			return new RDFFormat(lang);
		}
	}
}
