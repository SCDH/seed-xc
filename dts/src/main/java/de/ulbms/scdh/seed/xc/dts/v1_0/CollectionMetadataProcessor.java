package de.ulbms.scdh.seed.xc.dts.v1_0;

import com.apicatalog.jsonld.JsonLdOptions;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.InputStream;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.jsonld.TitaniumJsonLdOptions;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility for processing the collection metadata provided in <code>collection.json</code>.
 */
@Dependent
public class CollectionMetadataProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionMetadataProcessor.class);

	@Inject
	JsonLdOptions jsonLdOptions;

	/**
	 * Gets the location of a resource from the collection metadata and returns it wrapped
	 * into a {@link Uni}.
	 * @param collectionMetadata - the collection metadata (collection.json) as a wrapped stream
	 * @param systemId - file name of the collection metadata, used for guessing the RDF syntax
	 * @param context - a map of keys provided by the request
	 * @param id - Identifier (IRI) of the resource
	 * @return - The resource as a wrapped stream
	 */
	public Uni<String> getResourceLocation(
			Uni<InputStream> collectionMetadata, String systemId, Map<String, String> context, String id) {

		return collectionMetadata.onItem().transform((inputStream -> {
			// make graph
			RDFParserBuilder parserBuilder = RDFParser.source(inputStream);
			Lang lang = RDFLanguages.filenameToLang(systemId, Lang.JSONLD11);
			LOG.debug("trying to parse RDF data from {} as format {}", systemId, lang);
			if (lang.equals(Lang.JSONLD11)) {
				parserBuilder.set(TitaniumJsonLdOptions.JSONLD_OPTIONS, jsonLdOptions);
			}
			Model graph = parserBuilder.lang(lang).toModel();
			// get the location from the graph
			Resource resource = graph.getResource(id);
			// graph.getModel(id) creates missing, but does not yet add!
			if (!graph.containsResource(resource)) {
				throw new NotFoundException("not found: " + id);
			} else {
				Statement locationStmt = resource.getProperty(SEED.location);
				if (locationStmt != null && locationStmt.getObject().isLiteral()) {
					return locationStmt.getObject().asLiteral().getString();
				} else {
					if (!resource.hasProperty(RDF.type, DTS.Resource))
						throw new BadRequestException(id + " is not a dts:Resource");
					throw new NotFoundException("invalid collection metadata: " + id
							+ " seed:location ? . Property not present or not a literal");
				}
			}
		}));
	}
}
