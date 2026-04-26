package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import de.ulbms.scdh.seed.xc.api.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.jsonld.JenaToTitanium;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SparqlConstruct} is a {@link Transformation}
 * plugin for running SPARQL queries.
 */
public class SparqlConstruct implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(SparqlConstruct.class);

	public static final String TRANSFORMATION_TYPE = "sparql-construct";

	TransformationInfo transformationInfo;

	String query;

	@Inject
	ParameterConverter parameterConverter;

	@Inject
	Serializer serializer;

	@Override
	public String getType() {
		return SparqlConstruct.TRANSFORMATION_TYPE;
	}

	@Override
	public void setup(TransformationInfo transformationInfo, File path) throws ConfigurationException {
		this.transformationInfo = transformationInfo;
		Path configFile = Paths.get(path.toURI()).toAbsolutePath().normalize();
		Path queryFile = configFile.resolve(transformationInfo.getLocation());
		try {
			query = Files.readString(queryFile);
		} catch (IOException e) {
			LOG.error("failed to read SPARQL query: {}", queryFile);
			throw new ConfigurationException("failed to read SPARQL query: " + queryFile);
		}
	}

	@Override
	public TransformationInfo getTransformationInfo() {
		return transformationInfo;
	}

	@Override
	public XsltParameterDetails getTransformationParameters() {
		return new XsltParameterDetails();
	}

	/**
	 * Returns the configured context location or null when there is none.
	 * @return - {@link URI} to the context location
	 */
	private URI getContextUri() {
		URI result;
		if (transformationInfo.getContext() == null) {
			result = null;
		} else {
			result = transformationInfo.getContext().getLocation();
		}
		return result;
	}

	@Override
	public byte[] transform(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream source,
			ResourceProvider resourceProvider,
			HttpServerRequest request)
			throws TransformationPreparationException, TransformationException {
		try {
			// make graph
			Lang lang = RDFLanguages.filenameToLang(systemId, Lang.N3);
			LOG.info("trying to parse RDF data from {} as format {}", systemId, lang);
			Dataset graph = RDFParser.source(source).lang(lang).toDataset();
			// make query
			ParameterizedSparqlString queryTemplate = new ParameterizedSparqlString(this.query);
			if (parameters != null) {
				for (String key : parameters.getGlobalParameters().keySet()) {
					ParameterDescriptor descriptor =
							transformationInfo.getParameterDescriptors().get(key);
					String value = parameters.getGlobalParameters().get(key);
					if (descriptor == null) {
						// assume string
						queryTemplate.setLiteral(key, value);
					} else {
						parameterConverter.setQueryParameter(key, value, descriptor.getType(), queryTemplate);
					}
				}
			}
			Query query = queryTemplate.asQuery();
			// execute query
			QueryExecution qexec = QueryExecutionFactory.create(query, graph);
			Model resultModel = qexec.execConstruct();
			qexec.close();
			// write result back to the wire
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			RDFFormat format = serializer.getFormat(transformationInfo.getMediaType(), systemId, request);
			if (!format.getLang().equals(Lang.JSONLD11) || getContextUri() == null) {
				RDFDataMgr.write(output, resultModel, format);
				return output.toByteArray();
			} else {
				// use titanium for framing
				JsonLdOptions opts = new JsonLdOptions();
				DatasetGraph dsg = DatasetGraphFactory.create(resultModel.getGraph());
				JsonArray ja = JenaToTitanium.convert(dsg, opts);
				JsonDocument jdoc = JsonDocument.of(ja);
				JsonObject framed = JsonLd.frame(jdoc, getContextUri()).get();
				JsonWriter writer = Json.createWriter(output);
				writer.writeObject(framed);
				return output.toByteArray();
			}
		} catch (RiotException e) {
			LOG.error("failed to read RDF dataset {}", e.getMessage());
			throw new TransformationException(e);
		} catch (QueryExecException e) {
			LOG.error("failed to execute SPARQL query: {}", e.getMessage());
			throw new TransformationException(e);
		} catch (JsonLdError e) {
			LOG.error("failed to load into titanium json-ld, {}", e.getMessage());
			throw new TransformationException(e);
		}
	}

	@Override
	public Uni<byte[]> transformAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider,
			HttpServerRequest request) {
		return source.onItem().transform((sourceStream) -> {
			try {
				return transform(parameters, config, systemId, sourceStream, resourceProvider, request);
			} catch (TransformationPreparationException | TransformationException e) {
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}

	@Override
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}
}
