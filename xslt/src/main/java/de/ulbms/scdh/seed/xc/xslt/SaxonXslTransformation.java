package de.ulbms.scdh.seed.xc.xslt;

import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.ParameterDescriptor;
import de.ulbms.scdh.seed.xc.api.RuntimeParameters;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationInfoLibrariesInner;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import de.ulbms.scdh.seed.xc.api.TypedParameter;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetails;
import de.ulbms.scdh.seed.xc.api.XsltParameterDetailsValue;
import de.ulbms.scdh.seed.xc.harden.RestrictiveFileOnlyResolver;
import de.ulbms.scdh.seed.xc.harden.RestrictiveResourceResolver;
import de.ulbms.scdh.seed.xc.harden.ZipFileURIResolver;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.ProtocolRestrictor;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltExecutable.ParameterDetails;
import net.sf.saxon.s9api.XsltPackage;
import net.sf.saxon.str.StringView;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.StringConverter;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.value.AtomicValue;
import org.apache.xerces.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * A transformation using the Saxon XSLT processor. The stylesheet is
 * compile once and then used throughtout the lifecycle of the
 * service. Therefore, the bean, that creates an instance of this
 * class must be application scoped.
 */
@Dependent
public class SaxonXslTransformation implements Transformation {

	private static final Logger LOG =
		LoggerFactory.getLogger(SaxonXslTransformation.class);

	public static final String TRANSFORMATION_TYPE =
		"de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation";

	public static final String FEATURE_XINCLUDE =
		"http://apache.org/xml/features/xinclude";

	@Inject protected Processor processor;

	@Inject protected ServiceConfiguration serviceConfig;

	@Inject protected RestrictiveFileOnlyResolver xsltResourceResolver;

	@Inject protected ZipFileURIResolver zipResourceResolver;

	@Inject protected RestrictiveResourceResolver documentResourceResolver;

	@Inject protected UnparsedTextURIResolver unparsedTextURIResolver;

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

	public void setup(ZipFile zipFile, String stylesheetPath,
					  String saxonConfigPath) throws ConfigurationException {
		try {
			Processor processor;
			if (saxonConfigPath != null) {
				InputStream saxonConfigInputStream =
					Utils.fromZip(zipFile, saxonConfigPath);
				processor = new Processor(
					new StreamSource(saxonConfigInputStream, saxonConfigPath));
			} else {
				LOG.info("using default processor");
				processor = this.processor;
			}
			// compile stylesheet to an executable that can be used
			// for an abitrary number of transformations
			LOG.debug("Compiling '{}' ...", stylesheetPath);
			XsltCompiler compiler = processor.newXsltCompiler();
			compiler.setJustInTimeCompilation(false);
			// setup the compiler's resource resolver so that it can read files
			// from the zip
			zipResourceResolver.setNonDelegating();
			zipResourceResolver.setup(zipFile, null);
			compiler.setResourceResolver(zipResourceResolver);
			// compile
			InputStream stylesheetInputStream =
				Utils.fromZip(zipFile, stylesheetPath);
			this.executable =
				compiler.compile(new StreamSource(stylesheetInputStream));
		} catch (SaxonApiException e) {
			LOG.error("cannot compile stylesheet: {}", e.getMessage());
			throw new ConfigurationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(TransformationInfo transformationInfo)
		throws ConfigurationException {
		LOG.debug(
			"Setting up new SaxonXslTransformation with identifier '{}' ...",
			transformationInfo.getIdent());
		this.transformationInfo = transformationInfo;
		try {
			// fetch the stylesheet over the web
			Source stylesheet = xsltResourceResolver.resolve(
				mkXsltRequest(transformationInfo.getLocation()));
			// Setting the systemId sets the static context (XML Base). It
			// is important for relative imports, but already done by the
			// resolver!

			// compile stylesheet to an executable that can be used
			// for an abitrary number of transformations
			LOG.debug("Compiling '{}' ...", stylesheet.getSystemId());
			XsltCompiler compiler = processor.newXsltCompiler();
			compiler.setJustInTimeCompilation(false);
			compiler.setResourceResolver(xsltResourceResolver);
			// set compile time parameters
			if (transformationInfo.getCompileTimeParameters() != null) {
				ConversionRules conversionRules =
					processor.getUnderlyingConfiguration().getConversionRules();
				StringConverter stringToStringConverter =
					new StringConverter.StringToString();
				for (TypedParameter compileTimeParam :
					 transformationInfo.getCompileTimeParameters()) {
					LOG.debug("setting compile time parameter {}={}",
							  compileTimeParam.getName(),
							  compileTimeParam.getValue());
					if (compileTimeParam.getType() == null) {
						// assume xs:string type
						this.setAtomicParameter(compiler, compileTimeParam,
												stringToStringConverter);
					} else {
						SchemaType schemaType =
							BuiltInType.getSchemaTypeByLocalName(
								compileTimeParam.getType());
						if (schemaType == null) {
							// try xs:string type
							this.setAtomicParameter(compiler, compileTimeParam,
													stringToStringConverter);
						} else if (schemaType.isAtomicType()) {
							BuiltInAtomicType atomicType =
								(BuiltInAtomicType)schemaType;
							StringConverter converter =
								atomicType.getStringConverter(conversionRules);
							if (converter == null) {
								LOG.error(
									"failed to get converter for compile time "
										+ "parameter {} of type {}",
									compileTimeParam.getName(),
									compileTimeParam.getType());
							} else {
								this.setAtomicParameter(
									compiler, compileTimeParam, converter);
							}
						} else {
							// TODO: convert BuildinListType
						}
					}
				}
			}
			// compile and import packages first
			if (transformationInfo.getLibraries() != null) {
				for (TransformationInfoLibrariesInner library :
					 transformationInfo.getLibraries()) {
					LOG.debug("Compiling package {}", library.getLocation());
					try {
						Source packageSource = xsltResourceResolver.resolve(
							mkXsltRequest(library.getLocation()));
						XsltPackage pkg =
							compiler.compilePackage(packageSource);
						if (library.getAsName() != null &&
							library.getAsVersion() != null) {
							compiler.importPackage(pkg, library.getAsName(),
												   library.getAsVersion());
						} else {
							compiler.importPackage(pkg);
						}
					} catch (SaxonApiException e) {
						LOG.error("Failed to compile package from '{}': {}",
								  library.getLocation(), e.getMessage());
						throw new ConfigurationException(
							"Failed to compile package from '" +
								library.getLocation() + "': " + e.getMessage(),
							e);
					}
				}
			}
			// then compile the stylesheet
			executable = compiler.compile(stylesheet);
		} catch (SaxonApiException e) {
			LOG.error("Failed to setup transformation '{}':\n{}",
					  transformationInfo.getIdent(), e.getMessage());
			throw new ConfigurationException(
				"failed to setup transformation '" +
					transformationInfo.getIdent() + "': " + e.getMessage(),
				e);
		} catch (XPathException e) {
			LOG.error(e.getMessage());
			throw new ConfigurationException(e);
		}

		LOG.debug(
			"Done setting up SaxonXslTransformation with identifier '{}'.",
			transformationInfo.getIdent());
	}

	private void setAtomicParameter(XsltCompiler compiler,
									TypedParameter parameter,
									StringConverter converter) {
		try {
			AtomicValue atomicValue =
				converter.convertString(StringView.of(parameter.getValue()))
					.asAtomic();
			XdmAtomicValue value = XdmAtomicValue.makeAtomicValue(atomicValue);
			compiler.setParameter(QName.fromClarkName(parameter.getName()),
								  value);
		} catch (ValidationException e) {
			LOG.error("failed to convert compile time parameter {}: {}",
					  parameter.getName(), e.getMessage());
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
		Map<QName, ParameterDetails> parameterMap =
			executable.getGlobalParameters();
		for (QName name : parameterMap.keySet()) {
			ParameterDetails details = parameterMap.get(name);
			XsltParameterDetailsValue description =
				new XsltParameterDetailsValue();
			try {
				description.setOccurrenceIndicator(
					details.getDeclaredCardinality().toString());
			} catch (NullPointerException e) {
				LOG.error("cardinality not declared value for parameter {} in "
							  + "transformation {}",
						  name, transformationInfo.getIdent());
			}
			try {
				description.setItemType(
					details.getDeclaredItemType().getTypeName().toString());
			} catch (NullPointerException e) {
				LOG.error("item type not declared for parameter {} in "
							  + "transformation {}",
						  name, transformationInfo.getIdent());
			}
			try {
				description.setUnderlyingDeclaredType(
					details.getUnderlyingDeclaredType().toString());
			} catch (NullPointerException e) {
				LOG.error(
					"underlying item type not declared value for parameter {} "
						+ "in transformation {}",
					name, transformationInfo.getIdent());
			}
			try {
				description.setIsRequired(details.isRequired());
			} catch (NullPointerException e) {
				LOG.error(
					"cannot determine if parameter {} in transformation {} is "
						+ "required or not",
					name, transformationInfo.getIdent());
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
	public byte[] transform(RuntimeParameters parameters, Config config,
							String systemId, InputStream sourceStream)
		throws TransformationPreparationException, TransformationException {

		LOG.debug("Transforming `{}` ... (3)", systemId);

		// test if URI systemId is allowed
		if (!isAllowedURI(systemId)) {
			LOG.error("URI not allowed: {}", systemId);
			throw new TransformationPreparationException("URI not allowed: " +
														 systemId);
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Serializer out = processor.newSerializer(output);

		XMLReader reader = getParser(config);
		Source source = new SAXSource(reader, new InputSource(sourceStream));

		// setting the systemId is needed for the XML base property
		source.setSystemId(systemId);

		transform(parameters, config, source, out);
		return output.toByteArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transform(RuntimeParameters parameters, Config config,
							String systemId)
		throws TransformationPreparationException, TransformationException {

		LOG.debug("Transforming `{}` ... (2)", systemId);

		// test if URI systemId is allowed
		if (!isAllowedURI(systemId)) {
			LOG.error("URI not allowed: {}", systemId);
			throw new TransformationPreparationException("URI not allowed: " +
														 systemId);
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Serializer out = processor.newSerializer(output);

		XMLReader reader = getParser(config);
		Source source = new SAXSource(reader, new InputSource(systemId));

		// setting the systemId is needed for the XML base property
		source.setSystemId(systemId);

		transform(parameters, config, source, out);
		return output.toByteArray();
	}

	/**
	 * Test if the supplied URI (systemId) is allowed with regard to
	 * the allowed protocolls in the Saxon configuration. This will
	 * work if the configured URI resolver provides a predicate for
	 * testing URIs. If the resolver does not provide the predicate,
	 * the test will return <code>true</code>.
	 *
	 * @param systemId {@link String} representation of the URI
	 * @return boolean
	 *
	 * {@see net.sf.saxon.Configuration#getAllowedUriTest()}
	 * {@see net.sf.saxon.lib.StandardURIResolver#getAllowedUriTest()}
	 */
	protected boolean isAllowedURI(String systemId) {
		if (systemId == null) {
			return true;
		} else {
			Configuration config = processor.getUnderlyingConfiguration();
			if (config == null) {
				LOG.debug(
					"no configuration for checking for allowed protocols");
				return true;
			} else {
				URI uri;
				try {
					uri = new URI(systemId);
					if (uri == null) {
						LOG.debug("URI is null");
						return true;
					}
					ProtocolRestrictor restrictor =
						config.getProtocolRestrictor();
					if (restrictor == null) {
						// case no restriction provided
						return true;
					} else {
						return restrictor.test(uri);
					}
				} catch (URISyntaxException e) {
					LOG.error("malformed URI: {}", e.getMessage());
					return false;
				} catch (NullPointerException e) {
					LOG.info("URI '{}' failed test with a "
								 + "NullPointerException exception",
							 systemId);
					return serviceConfig.getNonProtocolURIsAllowed();
				}
			}
		}
	}

	/**
	 * Returns an instance of the {@link XMLReader} SAX parser given
	 * in the per-request {@link Config}. If no parser is requested,
	 * Xerces {@link SAXParser} is returned. Parser features and
	 * properties are set from {@link Config}.
	 *
	 * @param config  {@link Config} REST API parameters
	 * @return {@link XMLReader}
	 * @see {@link DocumentBuilder.build(Source)}
	 */
	protected XMLReader getParser(Config config)
		throws TransformationPreparationException {
		XMLReader parser;
		if (config != null && config.getParser() != null &&
			config.getParser().getPropertyClass() != null) {
			// use parser defined in per-request config
			String className = config.getParser().getPropertyClass();
			try {
				Class<?> clas = Class.forName(className);
				if (XMLReader.class.isAssignableFrom(clas)) {
					Constructor<XMLReader> constr =
						(Constructor<XMLReader>)clas.getConstructor();
					parser = constr.newInstance();
				} else {
					LOG.error("{} is not an XMLReader", className);
					throw new TransformationPreparationException(
						className + " is not an XMLReader");
				}
			} catch (Exception e) {
				LOG.error("error setting up parser: {}", e.getMessage());
				throw new TransformationPreparationException(e.getMessage());
			}
		} else if (transformationInfo.getParser() != null &&
				   transformationInfo.getParser().getPropertyClass() != null) {
			// use parser defined for the transformation
			String className =
				transformationInfo.getParser().getPropertyClass();
			try {
				Class<?> clas = Class.forName(className);
				if (XMLReader.class.isAssignableFrom(clas)) {
					Constructor<XMLReader> constr =
						(Constructor<XMLReader>)clas.getConstructor();
					parser = constr.newInstance();
				} else {
					LOG.error("{} is not an XMLReader", className);
					throw new TransformationPreparationException(
						className + " is not an XMLReader");
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
		if (config != null && config.getParser() != null &&
			config.getParser().getXinclude() != null) {
			boolean xincludeAware = config.getParser().getXinclude();
			try {
				parser.setFeature(FEATURE_XINCLUDE, xincludeAware);
				LOG.debug("feature {} set to {}", FEATURE_XINCLUDE,
						  xincludeAware);
			} catch (Exception e) {
				LOG.error("xinclude-aware parsing not supported by {}",
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
	protected synchronized void transform(RuntimeParameters parameters,
										  Config config, Source source,
										  Serializer serializer)
		throws TransformationPreparationException, TransformationException {

		Xslt30Transformer transformer = executable.load30();

		// add file system restriction on top of configured resource resolvers
		transformer.setResourceResolver(documentResourceResolver);
		transformer.setUnparsedTextResolver(unparsedTextURIResolver);

		try {
			transformer.setStylesheetParameters(
				makeStylesheetParameters(parameters));
		} catch (SaxonApiException e) {
			LOG.error("failed to set stylesheet parameters: {}",
					  e.getMessage());
			throw new TransformationPreparationException(
				"failed to set up transformation parameters: " + e.getMessage(),
				e);
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
			throw new TransformationException(
				"transformation failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Make a map of stylesheet parameters from runtime parameters. We
	 * can use the information provided by the compiled stylesheet for
	 * this: {@link XsltExecutable#getGlobalParameters()}
	 */
	protected Map<QName, XdmValue>
	makeStylesheetParameters(RuntimeParameters parameters)
		throws TransformationPreparationException, TransformationException {
		Map<QName, XdmValue> stylesheetParameters =
			new HashMap<QName, XdmValue>();
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
			ParameterDetails parameterDetails =
				executable.getGlobalParameters().get(name);
			ConversionRules conversionRules =
				processor.getUnderlyingConfiguration().getConversionRules();
			if (parametersMap.containsKey(nameString)) {
				XdmValue stringValue =
					new XdmAtomicValue(parametersMap.get(nameString));
				// TODO: evaluate type description
				ItemType itemType = parameterDetails.getDeclaredItemType();
				try {
					LOG.debug("converting parameter '{}' ('{}') to {}",
							  nameString, stringValue.toString(),
							  itemType.getTypeName());
					// ConversionRules conversionRules2 =
					// itemType.getConversionRules();
					AtomicType atomicType =
						(AtomicType)itemType.getUnderlyingItemType()
							.getPrimitiveItemType(); // FIXME: what about
													 // sequence types
					StringConverter converter =
						conversionRules.makeStringConverter(atomicType);
					AtomicValue atomicValue =
						converter
							.convertString(
								StringView.of(parametersMap.get(nameString)))
							.asAtomic();
					XdmAtomicValue value =
						XdmAtomicValue.makeAtomicValue(atomicValue);
					stylesheetParameters.put(name, value);
				} catch (ValidationException e) {
					LOG.error(
						"failed to convert '{}' parameter value '{}' to {}",
						nameString,
						parameters.getGlobalParameters().get(nameString),
						itemType.getTypeName());
					throw new TransformationPreparationException(
						"failed to convert '" + nameString +
							"' parameter value '" +
							parameters.getGlobalParameters().get(nameString) +
							"' to " + itemType.getTypeName().toString(),
						e);
				} catch (NullPointerException e) {
					LOG.error(
						"failed to convert '{}' parameter value due to missing "
							+ "type declaration. Transformation '{}'",
						nameString, transformationInfo.getIdent());
					throw new TransformationException(
						"failed to convert '" + nameString +
						"' parameter value due to missing type declaration");
				}
			} else if (parameterDetails.isRequired()) {
				LOG.error("required parameter '{}' missing", name);
				// throw new TransformationPreparationException("required
				// parameter '" + nameString + "' missing");
			}
		}
		LOG.debug("made stylesheet parameters '{}'", stylesheetParameters);
		return stylesheetParameters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transform(RuntimeParameters parameters, Config config)
		throws TransformationPreparationException, TransformationException {
		// TODO
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}
}
