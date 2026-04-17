package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.CompileTime;
import de.ulbms.scdh.seed.xc.saxon.harden.ChainedUnparsedTextURIResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.ServiceConfiguration;
import de.ulbms.scdh.seed.xc.saxon.harden.ZipFileURIResolver;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.lib.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltExecutable.ParameterDetails;
import net.sf.saxon.str.StringView;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import org.apache.xerces.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * A transformation using the Saxon XSLT processor. The stylesheet is
 * compiled once and then used throughout the lifecycle of the
 * service. Therefore, the bean, that creates an instance of this
 * class must be application scoped.
 */
@Dependent
public class SaxonXslTransformation implements Transformation, ExportingCompiler {

	private static final Logger LOG = LoggerFactory.getLogger(SaxonXslTransformation.class);

	public static final String TRANSFORMATION_TYPE = "xslt";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType() {
		return SaxonXslTransformation.TRANSFORMATION_TYPE;
	}

	public static final String FEATURE_XINCLUDE = "http://apache.org/xml/features/xinclude";

	@Inject
	protected Processor processor;

	@Inject
	protected ServiceConfiguration serviceConfig;

	@CompileTime
	@Inject
	protected ResourceResolver compileTimeResourceResolver;

	@Inject
	protected UnparsedTextURIResolver staticAssetsUnparsedTextURIResolver;

	@Inject
	protected ZipFileURIResolver zipResourceResolver;

	@Inject
	protected TransformationExceptionParser transformationExceptionParser;

	private XsltExecutable executable;

	protected TransformationInfo transformationInfo;

	/**
	 * Make a {@link ResourceRequest} from a URI given as string.
	 */
	protected ResourceRequest mkXsltRequest(String uri) {
		ResourceRequest request = new ResourceRequest();
		request.uri = uri;
		request.nature = ResourceRequest.XSLT_NATURE;
		return request;
	}

	public void setup(ZipFile zip, String stylesheetPath, String saxonConfigPath) throws ConfigurationException {
		try {
			Processor processor;
			if (saxonConfigPath != null) {
				InputStream saxonConfigInputStream = Utils.fromZip(zip, saxonConfigPath);
				processor = new Processor(new StreamSource(saxonConfigInputStream, saxonConfigPath));
			} else {
				LOG.info("using default processor");
				processor = this.processor;
			}
			// compile stylesheet to an executable that can be used
			// for an arbitrary number of transformations
			LOG.debug("Compiling from zip '{}' ...", stylesheetPath);
			XsltCompiler compiler = processor.newXsltCompiler();
			compiler.setJustInTimeCompilation(false);
			// set up the compiler's resource resolver so that it can read files
			// from the zip
			zipResourceResolver.setNonDelegating();
			zipResourceResolver.setup(zip, null);
			compiler.setResourceResolver(zipResourceResolver);
			// compile
			InputStream stylesheetInputStream = Utils.fromZip(zip, stylesheetPath);
			this.executable = compiler.compile(new StreamSource(stylesheetInputStream));
		} catch (SaxonApiException e) {
			LOG.error("cannot compile stylesheet: {}", e.getMessage());
			throw new ConfigurationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(TransformationInfo transformationInfo) throws ConfigurationException {
		LOG.debug("Setting up new SaxonXslTransformation with identifier '{}' ...", transformationInfo.getIdent());
		this.transformationInfo = transformationInfo;
		try {
			// fetch the stylesheet over the web
			Source stylesheet = compileTimeResourceResolver.resolve(mkXsltRequest(transformationInfo.getLocation()));
			// Setting the systemId sets the static context (XML Base). It
			// is important for relative imports, but already done by the
			// resolver!

			// compile stylesheet to an executable that can be used
			// for an arbitrary number of transformations
			LOG.debug("Compiling from transformation info '{}' ...", stylesheet.getSystemId());
			XsltCompiler compiler = processor.newXsltCompiler();
			compiler.setJustInTimeCompilation(false);
			compiler.setResourceResolver(compileTimeResourceResolver);
			// set compile time parameters
			if (transformationInfo.getCompileTimeParameters() != null) {
				ConversionRules conversionRules =
						processor.getUnderlyingConfiguration().getConversionRules();
				StringConverter stringToStringConverter = new StringConverter.StringToString();
				for (TypedParameter compileTimeParam : transformationInfo.getCompileTimeParameters()) {
					LOG.debug(
							"setting compile time parameter {}={}",
							compileTimeParam.getName(),
							compileTimeParam.getValue());
					if (compileTimeParam.getType() == null) {
						// assume xs:string type
						this.setAtomicParameter(compiler, compileTimeParam, stringToStringConverter);
					} else {
						SchemaType schemaType = BuiltInType.getSchemaTypeByLocalName(compileTimeParam.getType());
						if (schemaType == null) {
							// try xs:string type
							this.setAtomicParameter(compiler, compileTimeParam, stringToStringConverter);
						} else if (schemaType.isAtomicType()) {
							BuiltInAtomicType atomicType = (BuiltInAtomicType) schemaType;
							StringConverter converter = atomicType.getStringConverter(conversionRules);
							if (converter == null) {
								LOG.error(
										"failed to get converter for compile time parameter {} of type {}",
										compileTimeParam.getName(),
										compileTimeParam.getType());
							} else {
								this.setAtomicParameter(compiler, compileTimeParam, converter);
							}
						} else {
							// TODO: convert BuildinListType
						}
					}
				}
			}
			// compile and import packages first
			if (transformationInfo.getLibraries() != null) {
				for (TransformationInfoLibrariesInner library : transformationInfo.getLibraries()) {
					LOG.debug("Compiling package {}", library.getLocation());
					try {
						Source packageSource =
								compileTimeResourceResolver.resolve(mkXsltRequest(library.getLocation()));
						XsltPackage pkg = compiler.compilePackage(packageSource);
						if (library.getAsName() != null && library.getAsVersion() != null) {
							compiler.importPackage(pkg, library.getAsName(), library.getAsVersion());
						} else {
							compiler.importPackage(pkg);
						}
					} catch (SaxonApiException e) {
						LOG.error("Failed to compile package from '{}': {}", library.getLocation(), e.getMessage());
						throw new ConfigurationException(
								"Failed to compile package from '" + library.getLocation() + "': " + e.getMessage(), e);
					}
				}
			}
			// then compile the stylesheet
			executable = compiler.compile(stylesheet);
		} catch (SaxonApiException e) {
			LOG.error("Failed to setup transformation '{}':\n{}", transformationInfo.getIdent(), e.getMessage());
			throw new ConfigurationException(
					"failed to setup transformation '" + transformationInfo.getIdent() + "': " + e.getMessage(), e);
		} catch (XPathException e) {
			LOG.error(e.getMessage());
			throw new ConfigurationException(e);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XsltParameterDetails getTransformationParameters() {
		XsltParameterDetails xsltParameterDetails = new XsltParameterDetails();
		Map<QName, ParameterDetails> parameterMap = executable.getGlobalParameters();
		for (QName name : parameterMap.keySet()) {
			ParameterDetails details = parameterMap.get(name);
			XsltParameterDetailsValue description = new XsltParameterDetailsValue();
			try {
				description.setOccurrenceIndicator(
						details.getDeclaredCardinality().toString());
			} catch (NullPointerException e) {
				LOG.error(
						"cardinality not declared value for parameter {} in transformation {}",
						name,
						transformationInfo.getIdent());
			}
			try {
				description.setItemType(
						details.getDeclaredItemType().getTypeName().toString());
			} catch (NullPointerException e) {
				LOG.error(
						"item type not declared for parameter {} in transformation {}",
						name,
						transformationInfo.getIdent());
			}
			try {
				description.setUnderlyingDeclaredType(
						details.getUnderlyingDeclaredType().toString());
			} catch (NullPointerException e) {
				LOG.error(
						"underlying item type not declared value for parameter {} in transformation {}",
						name,
						transformationInfo.getIdent());
			}
			try {
				description.setIsRequired(details.isRequired());
			} catch (NullPointerException e) {
				LOG.error(
						"cannot determine if parameter {} in transformation {} is required or not",
						name,
						transformationInfo.getIdent());
			}
			xsltParameterDetails.put(name.toString(), description);
		}
		return xsltParameterDetails;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] export() throws UnsupportedOperationException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			this.executable.export(output);
			return output.toByteArray();
		} catch (SaxonApiException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] export(String target) throws UnsupportedOperationException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			this.executable.export(output, target);
			return output.toByteArray();
		} catch (SaxonApiException e) {
			throw new UnsupportedOperationException(e);
		}
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
	 * @see net.sf.saxon.s9api.DocumentBuilder#build(Source)
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
	 */
	protected void transform(
			RuntimeParameters parameters,
			Config config,
			Source source,
			Serializer serializer,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException {

		Xslt30Transformer transformer = executable.load30();

		// add file system restriction: access to the compiled
		// resources (with fn:static-base-uri()) is allowed as
		// well as access with the resource provider, e.g. for
		// XInclude.

		// 1. resource resolver for accessing XML via fn:doc() etc.
		transformer.setResourceResolver(new ChainedResourceResolver(compileTimeResourceResolver, resourceProvider));
		transformer.setUnparsedTextResolver(
				new ChainedUnparsedTextURIResolver(staticAssetsUnparsedTextURIResolver, resourceProvider));

		// calling <xsl:result-document> must always throw an error
		transformer.setResultDocumentHandler(null);

		try {
			transformer.setStylesheetParameters(makeStylesheetParameters(parameters));
		} catch (SaxonApiException e) {
			LOG.error("failed to set stylesheet parameters: {}", e.getMessage());
			throw new TransformationPreparationException(
					"failed to set up transformation parameters: " + e.getMessage(), e);
		}

		// TODO: evaluate evaluate initialTemplate and initialFunction from
		// runtime parameters
		try {
			LOG.debug("source: {}, serializer: {}", source, serializer);
			DocumentBuilder documentBuilder = processor.newDocumentBuilder();
			XdmNode docNode = documentBuilder.build(source);
			// setting the global context item is required for global variables
			transformer.setGlobalContextItem(docNode, false);
			// transform
			transformer.applyTemplates(docNode, serializer);
		} catch (NullPointerException e) {
			LOG.error("no source defined, {}", e.getMessage());
			throw new TransformationException("no source defined", e);
		} catch (SaxonApiException e) {
			LOG.error("transformation failed: {}", e.getMessage());
			throw new TransformationException("transformation failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Make a map of stylesheet parameters from runtime parameters. We
	 * can use the information provided by the compiled stylesheet for
	 * this: {@link XsltExecutable#getGlobalParameters()}
	 */
	protected Map<QName, XdmValue> makeStylesheetParameters(RuntimeParameters parameters)
			throws TransformationPreparationException, TransformationException {
		Map<QName, XdmValue> stylesheetParameters = new HashMap<>();
		if (parameters == null) {
			return stylesheetParameters;
		}
		Map<String, String> parametersMap = parameters.getGlobalParameters();
		if (parametersMap == null) {
			return stylesheetParameters;
		}
		// see XsltExecutable#getGlobalParameters()
		for (QName name : executable.getGlobalParameters().keySet()) {
			String nameString = name.toString();
			ParameterDetails parameterDetails = executable.getGlobalParameters().get(name);
			ConversionRules conversionRules =
					processor.getUnderlyingConfiguration().getConversionRules();
			if (parametersMap.containsKey(nameString)) {
				XdmValue stringValue = new XdmAtomicValue(parametersMap.get(nameString));
				// TODO: evaluate type description
				ItemType itemType = parameterDetails.getDeclaredItemType();
				try {
					LOG.debug(
							"converting parameter '{}' ('{}') to {}", nameString, stringValue, itemType.getTypeName());
					// ConversionRules conversionRules2 = itemType.getConversionRules();
					AtomicType atomicType =
							(AtomicType) itemType.getUnderlyingItemType().getPrimitiveItemType();
					// FIXME: what about sequence types
					StringConverter converter = conversionRules.makeStringConverter(atomicType);
					AtomicValue atomicValue = converter
							.convertString(StringView.of(parametersMap.get(nameString)))
							.asAtomic();
					XdmAtomicValue value = XdmAtomicValue.makeAtomicValue(atomicValue);
					stylesheetParameters.put(name, value);
				} catch (ValidationException e) {
					LOG.error(
							"failed to convert '{}' parameter value '{}' to {}",
							nameString,
							parameters.getGlobalParameters().get(nameString),
							itemType.getTypeName());
					throw new TransformationPreparationException(
							"failed to convert '" + nameString + "' parameter value '"
									+ parameters.getGlobalParameters().get(nameString)
									+ "' to "
									+ itemType.getTypeName().toString(),
							e);
				} catch (NullPointerException e) {
					LOG.error(
							"failed to convert '{}' parameter value due to missing type declaration. Transformation '{}'",
							nameString,
							transformationInfo.getIdent());
					throw new TransformationException(
							"failed to convert '" + nameString + "' parameter value due to missing type declaration");
				}
			} else if (parameterDetails.isRequired()) {
				LOG.error("required parameter '{}' missing", name);
				// throw new TransformationPreparationException("required parameter " + nameString + " missing");
			}
		}
		LOG.debug("made stylesheet parameters '{}'", stylesheetParameters);
		return stylesheetParameters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}
}
