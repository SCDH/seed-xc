package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import de.ulbms.scdh.seed.xc.api.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIxResolver;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.jsonld.JenaToTitanium;
import org.apache.jena.riot.system.jsonld.TitaniumJsonLdOptions;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SparqlConstruct} is a {@link Transformation}
 * plugin for running SPARQL queries.
 */
@Dependent
public class SparqlConstruct implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(SparqlConstruct.class);

	public static final String TRANSFORMATION_TYPE = "sparql-construct";

	TransformationInfo transformationInfo;

	String query;

	@Inject
	ParameterInjector parameterInjector;

	@Inject
	Serializer serializer;

	@Override
	public String getClazz() {
		return SparqlConstruct.TRANSFORMATION_TYPE;
	}

	@Inject
	JsonLdContext jsonLdContextFactory;

	@Inject
	JsonLdOptions jsonLdOptions;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getType() {
		if (transformationInfo.getType() == null) {
			return new String[] {};
		}
		return transformationInfo.getType().toArray(new String[0]);
	}

	@Override
	public void setup(TransformationInfo transformationInfo, File path) throws ConfigurationException {
		this.transformationInfo = transformationInfo;
		Path configFile = Paths.get(path.toURI()).toAbsolutePath().normalize();
		Path queryFile =
				configFile.getParent().resolve(transformationInfo.getLocation()).normalize();
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
			InputStream buffer = new BufferedInputStream(source);
			RDFParserBuilder parserBuilder = RDFParser.source(buffer);
			Lang lang = RDFLanguages.filenameToLang(systemId, Lang.JSONLD11);
			LOG.debug("trying to parse RDF data from {} as format {}", systemId, lang);
			if (lang.equals(Lang.JSONLD11)) {
				parserBuilder.set(TitaniumJsonLdOptions.JSONLD_OPTIONS, jsonLdOptions);
				// configure IRI resolver for not resolving relative IRIs (issue #22)
				IRIxResolver.Builder iriResolverBuilder =
						IRIxResolver.create(IRIs.stdResolver().clone());
				iriResolverBuilder.allowRelative(true);
				if (config != null && config.getBase() != null) {
					iriResolverBuilder.base(config.getBase());
				}
				parserBuilder.resolver(iriResolverBuilder.build());
			}
			Dataset graph = parserBuilder.lang(lang).toDataset();
			// make query
			ParameterizedSparqlString queryTemplate = new ParameterizedSparqlString(this.query);
			LOG.debug("un-parametrized SPARQL query: {} ...", this.query.lines().findFirst());
			if (parameters != null) {
				for (String key : parameters.getGlobalParameters().keySet()) {
					ParameterDescriptor descriptor =
							transformationInfo.getParameterDescriptors().get(key);
					ParameterValue value = parameters.getGlobalParameters().get(key);
					LOG.debug("setting parameter {} to {} as {}", key, value, descriptor);
					String type = null; // unknown by default
					if (descriptor != null) type = descriptor.getType();
					queryTemplate = parameterInjector.setQueryParameter(key, value, type, queryTemplate);
				}
			}
			Query query = queryTemplate.asQuery();
			LOG.debug("parametrized SPARQL query: {}", query.toString());
			// execute query
			QueryExecution qexec = QueryExecutionFactory.create(query, graph);
			Model resultModel = qexec.execConstruct();
			qexec.close();
			LOG.debug("done processing SPARQL query");
			if (LOG.isDebugEnabled()) {
				StringWriter dbgOut = new StringWriter();
				RDFDataMgr.write(dbgOut, resultModel, RDFFormat.TURTLE_PRETTY);
				LOG.info("turtle result {}", dbgOut);
			}
			// check configured post-conditions
			LOG.info("result encompasses {} triples", resultModel.size());
			if (config != null && config.getEmpty404() != null && config.getEmpty404() && resultModel.isEmpty()) {
				throw new TransformationException("404");
			}
			// write result back to the wire
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			RDFFormat format = serializer.getFormat(transformationInfo.getMediaType(), systemId, request);
			if (!format.getLang().equals(Lang.JSONLD11) || !jsonLdContextFactory.providesContext(transformationInfo)) {
				RDFDataMgr.write(output, resultModel, format);
			} else {
				// use titanium for framing
				JsonLdOptions opts = new JsonLdOptions();
				DatasetGraph dsg = DatasetGraphFactory.create(resultModel.getGraph());
				JsonArray ja = JenaToTitanium.convert(dsg, opts);
				JsonDocument jdoc = JsonDocument.of(ja);
				Document frameDoc = jsonLdContextFactory.getContext(transformationInfo);
				JsonLdOptions options = new JsonLdOptions(jsonLdOptions);
				// options.setBase(null);
				options.setOmitGraph(true);
				// add more options here!
				FramingApi framingApi = JsonLd.frame(jdoc, frameDoc);
				framingApi.loader(options.getDocumentLoader()); // important to set loader!
				framingApi.base("");
				JsonObject framed = framingApi.get();
				JsonWriter writer = Json.createWriter(output);
				writer.writeObject(framed);
			}
			return output.toByteArray();
		} catch (RiotException e) {
			LOG.error("failed to read RDF dataset {}", e.getMessage());
			throw new TransformationException(e);
		} catch (QueryExecException e) {
			LOG.error("failed to execute SPARQL query: {}", e.getMessage());
			throw new TransformationException(e);
		} catch (JsonLdError e) {
			LOG.error("JSON-LD processing failed, {}", e.getMessage());
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
				if (e.getMessage().equals("404")) {
					throw new NotFoundException("not found");
				}
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}

	@Override
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}
}
