package de.ulbms.scdh.seed.xc.jena;

import de.ulbms.scdh.seed.xc.api.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SparqlTransformation} is a {@link Transformation}
 * plugin for running SPARQL queries.
 */
public class SparqlTransformation implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(SparqlTransformation.class);

	public static final String TRANSFORMATION_TYPE = "sparql";

	TransformationInfo transformationInfo;

	String query;

	@Inject
	ParameterConverter parameterConverter;

	@Override
	public String getType() {
		return SparqlTransformation.TRANSFORMATION_TYPE;
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

	@Override
	public byte[] transform(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream source,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException {
		// make graph
		Dataset graph = RDFParser.source(source).toDataset();
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
		RDFDataMgr.write(output, resultModel, RDFFormat.NTRIPLES);
		return output.toByteArray();
	}

	@Override
	public Uni<byte[]> transformAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider) {
		return source.onItem().transform((sourceStream) -> {
			try {
				return transform(parameters, config, systemId, sourceStream, resourceProvider);
			} catch (TransformationPreparationException e) {
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}

	@Override
	public String getOutputMediaType() {
		return "";
	}
}
