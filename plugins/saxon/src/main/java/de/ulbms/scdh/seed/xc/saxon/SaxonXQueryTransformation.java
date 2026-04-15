package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.saxon.harden.ChainedUnparsedTextURIResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.RestrictiveFileOnlyResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.ServiceConfiguration;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.lib.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.str.StringView;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import org.apache.xerces.parsers.SAXParser;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * A transformation using the Saxon XQuery processor. The query is
 * compiled once and then used throughout the lifecycle of the
 * service. Therefore, the bean, that creates an instance of this
 * class must be application scoped.
 */
@Dependent
public class SaxonXQueryTransformation implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(SaxonXQueryTransformation.class);

	public static final String TRANSFORMATION_TYPE = "xquery";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType() {
		return SaxonXQueryTransformation.TRANSFORMATION_TYPE;
	}

	public static final String FEATURE_XINCLUDE = "http://apache.org/xml/features/xinclude";

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.transformations.ConfiguredTransformationMap.configLocations",
			defaultValue = "")
	String configLocations;

	@Inject
	protected Processor processor;

	@Inject
	protected ServiceConfiguration serviceConfig;

	@Inject
	protected ModuleURIResolver compileTimeModuleResolver;

	@Inject
	protected RestrictiveFileOnlyResolver compileTimeResourceResolver;

	@Inject
	protected UnparsedTextURIResolver staticAssetsUnparsedTextURIResolver;

	@Inject
	protected TransformationExceptionParser transformationExceptionParser;

	private XQueryExecutable executable;

	protected TransformationInfo transformationInfo;

	/**
	 * Make a {@link ResourceRequest} from a URI given as string.
	 */
	protected ResourceRequest mkXQueryRequest(String uri) {
		ResourceRequest request = new ResourceRequest();
		request.uri = uri;
		request.nature = ResourceRequest.XQUERY_NATURE;
		return request;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(TransformationInfo transformationInfo) throws ConfigurationException {
		LOG.debug("Setting up new SaxonXQueryTransformation with identifier '{}' ...", transformationInfo.getIdent());
		this.transformationInfo = transformationInfo;
		try {
			// fetch the stylesheet over the web
			// TODO: only try the first config location?
			String configBase =
					Arrays.stream(configLocations.split(",")).findFirst().get().trim();
			File query = Paths.get(configBase)
					.resolve(transformationInfo.getLocation())
					.toFile();

			// Setting the systemId sets the static context (XML Base). It
			// is important for relative imports, but already done by the
			// resolver!

			// compile stylesheet to an executable that can be used
			// for an arbitrary number of transformations
			LOG.debug("Compiling from transformation info '{}' ...", query);
			XQueryCompiler compiler = processor.newXQueryCompiler();
			compiler.setFastCompilation(false);
			compiler.setModuleURIResolver(compileTimeModuleResolver);
			// set compile time parameters
			if (transformationInfo.getCompileTimeParameters() != null) {
				ConversionRules conversionRules =
						processor.getUnderlyingConfiguration().getConversionRules();
				StringConverter stringToStringConverter = new StringConverter.StringToString();
				for (TypedParameter compileTimeParam : transformationInfo.getCompileTimeParameters()) {
					LOG.error(
							"There are no compile parameters for Saxon's XQuery Processor. Cannot set {}={}",
							compileTimeParam.getName(),
							compileTimeParam.getValue());
				}
			}
			// compile and import packages first
			if (transformationInfo.getLibraries() != null) {
				for (TransformationInfoLibrariesInner library : transformationInfo.getLibraries()) {
					LOG.debug("Compiling package {}", library.getLocation());
					try {
						File lib = Paths.get(configBase)
								.resolve(library.getLocation())
								.toFile();
						compiler.compileLibrary(lib);
						// TODO: import required?
					} catch (SaxonApiException e) {
						LOG.error("Failed to compile library from '{}': {}", library.getLocation(), e.getMessage());
						throw new ConfigurationException(
								"Failed to compile library from '" + library.getLocation() + "': " + e.getMessage(), e);
					}
				}
			}
			// then compile the stylesheet
			executable = compiler.compile(query);
		} catch (IndexOutOfBoundsException e) {
			LOG.error("query not found {}", transformationInfo.getLocation());
			throw new ConfigurationException(e);
		} catch (IOException e) {
			LOG.error("not found while compiling transformation {}: {}", transformationInfo.getIdent(), e.getMessage());
			throw new ConfigurationException(e);
		} catch (SaxonApiException e) {
			LOG.error("Failed to setup transformation '{}': {}", transformationInfo.getIdent(), e.getMessage());
			throw new ConfigurationException(
					"failed to setup transformation '" + transformationInfo.getIdent() + "': " + e.getMessage(), e);
		}

		LOG.debug("Done setting up SaxonXslTransformation with identifier '{}'.", transformationInfo.getIdent());
	}

	private void setAtomicParameter(XsltCompiler compiler, TypedParameter parameter, StringConverter converter) {
		try {
			AtomicValue atomicValue =
					converter.convertString(StringView.of(parameter.getValue())).asAtomic();
			XdmAtomicValue value = XdmAtomicValue.makeAtomicValue(atomicValue);
			compiler.setParameter(QName.fromClarkName(parameter.getName()), value);
		} catch (ValidationException e) {
			LOG.error("failed to convert compile time parameter {}: {}", parameter.getName(), e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransformationInfo getTransformationInfo() {
		return transformationInfo;
	}

	@Override
	public XsltParameterDetails getTransformationParameters() {
		return null; // TODO: How to get an iterator over external variables?
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transform(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream sourceStream,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException {

		LOG.debug("Transforming `{}` ... (3)", systemId);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Serializer out = processor.newSerializer(output);

		XMLReader reader = getParser(config);
		Source source = new SAXSource(reader, new InputSource(sourceStream));

		// setting the systemId is needed for the XML base property
		source.setSystemId(systemId);

		transform(parameters, config, source, out, resourceProvider);
		return output.toByteArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<byte[]> transformAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			Uni<? extends InputStream> sourceUni,
			ResourceProvider resourceProvider) {

		return sourceUni.onItem().transform((sourceStream) -> {
			try {
				return transform(parameters, config, systemId, sourceStream, resourceProvider);
			} catch (TransformationPreparationException e) {
				throw new InternalServerErrorException(e.getMessage());
			} catch (TransformationException e) {
				throw new WebApplicationException(
						transformationExceptionParser.message(e), transformationExceptionParser.parseCode(e));
			}
		});
	}

	/**
	 * Returns an instance of the {@link XMLReader} SAX parser given
	 * in the per-request {@link Config}. If no parser is requested,
	 * Xerces {@link SAXParser} is returned. Parser features and
	 * properties are set from {@link Config}.
	 *
	 * @param config  {@link Config} REST API parameters
	 * @return {@link XMLReader}
	 * @see DocumentBuilder#build(Source)
	 */
	protected XMLReader getParser(Config config) throws TransformationPreparationException {
		XMLReader parser;
		if (config != null && config.getParser() != null && config.getParser().getPropertyClass() != null) {
			// use parser defined in per-request config
			String className = config.getParser().getPropertyClass();
			try {
				Class<?> clas = Class.forName(className);
				if (XMLReader.class.isAssignableFrom(clas)) {
					Constructor<XMLReader> constr = (Constructor<XMLReader>) clas.getConstructor();
					parser = constr.newInstance();
				} else {
					LOG.error("{} is not an XMLReader", className);
					throw new TransformationPreparationException(className + " is not an XMLReader");
				}
			} catch (Exception e) {
				LOG.error("error setting up parser: {}", e.getMessage());
				throw new TransformationPreparationException(e.getMessage());
			}
		} else if (transformationInfo.getParser() != null
				&& transformationInfo.getParser().getPropertyClass() != null) {
			// use parser defined for the transformation
			String className = transformationInfo.getParser().getPropertyClass();
			try {
				Class<?> clas = Class.forName(className);
				if (XMLReader.class.isAssignableFrom(clas)) {
					Constructor<XMLReader> constr = (Constructor<XMLReader>) clas.getConstructor();
					parser = constr.newInstance();
				} else {
					LOG.error("{} is not an XMLReader", className);
					throw new TransformationPreparationException(className + " is not an XMLReader");
				}
			} catch (Exception e) {
				LOG.error("error setting up parser: {}", e.getMessage());
				throw new TransformationPreparationException(e.getMessage());
			}
		} else {
			// use Xerces as default parser
			parser = new SAXParser();
		}

		LOG.debug("parsing with {}", parser.getClass().getCanonicalName());
		if (config != null && config.getParser() != null && config.getParser().getXinclude() != null) {
			boolean xincludeAware = config.getParser().getXinclude();
			try {
				parser.setFeature(FEATURE_XINCLUDE, xincludeAware);
				LOG.debug("feature {} set to {}", FEATURE_XINCLUDE, xincludeAware);
			} catch (Exception e) {
				LOG.error(
						"xinclude-aware parsing not supported by {}",
						parser.getClass().getCanonicalName());
				// throw new TransformationPreparationException("xinclude-aware
				// parsing not supported by " +
				// parser.getClass().getCanonicalName());
			}
		}

		return parser;
	}

	/**
	 * Internal method that does the transformation job.
	 *
	 * This sets {@link Processor} features and thus must be made
	 * thread safe by the <code>synchronized</code> keyword.
	 */
	protected synchronized void transform(
			RuntimeParameters parameters,
			Config config,
			Source source,
			Serializer serializer,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException {

		XQueryEvaluator transformer = executable.load();

		// add file system restriction: access to the compiled
		// resources (with fn:static-base-uri()) is allowed as
		// well as access with the resource provider, e.g. for
		// XInclude.

		// 1. resource resolver for accessing XML via fn:doc() etc.
		transformer.setResourceResolver(new ChainedResourceResolver(compileTimeResourceResolver, resourceProvider));
		transformer.setUnparsedTextResolver(
				new ChainedUnparsedTextURIResolver(staticAssetsUnparsedTextURIResolver, resourceProvider));

		try {
			for (String name : parameters.getGlobalParameters().keySet()) {
				QName qname = new QName(name);
				XdmValue defaultValue = transformer.getExternalVariable(qname);
				XdmValue value = null; // TODO: set using type information of default value
				transformer.setExternalVariable(qname, value);
			}
		} catch (SaxonApiUncheckedException e) {
			LOG.error("failed to set stylesheet parameters: {}", e.getMessage());
			throw new TransformationPreparationException(
					"failed to set up transformation parameters: " + e.getMessage(), e);
		}

		// TODO: evaluate evaluate initialTemplate and initialFunction from
		// runtime parameters
		try {
			transformer.setSource(source);
			// transform
			transformer.run(serializer);
		} catch (NullPointerException e) {
			LOG.error("no source defined, {}", e.getMessage());
			throw new TransformationException("no source defined", e);
		} catch (SaxonApiException e) {
			LOG.error("transformation failed: {}", e.getMessage());
			throw new TransformationException("transformation failed: " + e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}
}
