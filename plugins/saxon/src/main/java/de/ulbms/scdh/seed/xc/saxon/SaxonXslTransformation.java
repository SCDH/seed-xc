package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.saxon.harden.ChainingResourceResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.ChainingUnparsedTextURIResolver;
import de.ulbms.scdh.seed.xc.saxon.harden.ServiceConfiguration;
import de.ulbms.scdh.seed.xc.saxon.harden.ZipFileURIResolver;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Rewriter;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;
import de.wwu.scdh.annotation.selection.rewriter.BackwardMappingFactory;
import de.wwu.scdh.annotation.selection.rewriter.ForwardMappingFactory;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltExecutable.ParameterDetails;
import net.sf.saxon.str.StringView;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transformation using the Saxon XSLT processor. The stylesheet is
 * compiled once and then used throughout the lifecycle of the
 * service. Therefore, the bean, that creates an instance of this
 * class must be application scoped.
 */
@Dependent
public class SaxonXslTransformation extends TransformationBase
		implements Transformation, MappingTransformation, ExportingCompiler {

	private static final Logger LOG = LoggerFactory.getLogger(SaxonXslTransformation.class);

	public static final String TRANSFORMATION_TYPE = "xslt";

	@ConfigProperty(name = "selene-xslt-tracing-library", defaultValue = "libtrace.xsl")
	protected String seleneTracingLibrary;

	@ConfigProperty(name = "selene-xpath-library", defaultValue = "selene/xpath.xsl")
	protected String seleneXPathLibrary;

	@ConfigProperty(
			name = "selene-xpath-library-namespace",
			defaultValue = "http://wwu.de/scdh/selection-engine/xpaths")
	protected String seleneTracingLibraryNamespace;

	@ConfigProperty(
			name = "selene-forward-xpath-default",
			defaultValue = "Q{http://wwu.de/scdh/selection-engine/xpaths}to-element")
	protected String seleneForwardXPathDefaultClarkName;

	private String seleneForwardXPathDefault;

	@ConfigProperty(
			name = "selene-backward-xpath-default",
			defaultValue = "Q{http://www.w3.org/2005/xpath-functions}path")
	protected String seleneBackwardXPathDefaultClarkName;

	private String seleneBackwardXPathDefault;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClazz() {
		return SaxonXslTransformation.TRANSFORMATION_TYPE;
	}

	@Inject
	protected ServiceConfiguration serviceConfig;

	@Inject
	protected ZipFileURIResolver zipResourceResolver;

	private XsltExecutable executable, mappingExecutable = null;

	private XPathCompiler mappingXPathCompiler = null;

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
	public void setup(TransformationInfo transformationInfo, File configFile) throws ConfigurationException {
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
			// set up the mapping compiler
			XsltCompiler mappingCompiler = processor.newXsltCompiler();
			mappingCompiler.setJustInTimeCompilation(false);
			mappingCompiler.setResourceResolver(compileTimeResourceResolver);
			boolean isMappingTransformation = false; // state: set true, when transformation depends on libtrace.xsl
			boolean mappingCompiled =
					true; // state: set false when a compilation step of the mapping transformation failed
			// compile tracing library with appropriate parameters
			try {
				XsltPackage tracePkg = ResourceBuilder.compileTracingPackage(mappingCompiler, getSeleneOutputMethod());
				mappingCompiler.importPackage(tracePkg);
			} catch (de.wwu.scdh.annotation.selection.ResourceException | SaxonApiException e) {
				mappingCompiled = false;
				LOG.warn(
						"failed to compile the Selene tracing library. No mapping will be available for {}: {}",
						transformationInfo.getIdent(),
						e.getMessage());
			}
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
						this.setAtomicParameter(mappingCompiler, compileTimeParam, stringToStringConverter);
					} else {
						SchemaType schemaType = BuiltInType.getSchemaTypeByLocalName(compileTimeParam.getType());
						if (schemaType == null) {
							// try xs:string type
							this.setAtomicParameter(compiler, compileTimeParam, stringToStringConverter);
							this.setAtomicParameter(mappingCompiler, compileTimeParam, stringToStringConverter);
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
								this.setAtomicParameter(mappingCompiler, compileTimeParam, converter);
							}
						} else {
							LOG.error(
									"not implemented: failed to set compile time parameter {}: {}",
									compileTimeParam.getName(),
									compileTimeParam.getType());
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
						if (mappingCompiled) {
							if (library.getLocation().endsWith(seleneTracingLibrary)) {
								isMappingTransformation = true;
							} else {
								try {
									XsltPackage mPkg = mappingCompiler.compilePackage(packageSource);
									if (library.getAsName() != null && library.getAsVersion() != null) {
										mappingCompiler.importPackage(
												mPkg, library.getAsName(), library.getAsVersion());
									} else {
										mappingCompiler.importPackage(mPkg);
									}
								} catch (SaxonApiException e) {
									LOG.warn(
											"failed to compile mapping transformation {}: {}",
											transformationInfo.getIdent(),
											e.getMessage());
									mappingCompiled = false;
								}
							}
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
			if (mappingCompiled && isMappingTransformation) {
				try {
					mappingExecutable = mappingCompiler.compile(stylesheet);
					LOG.info("successfully compiled mapping transformation {}", transformationInfo.getIdent());
				} catch (SaxonApiException e) {
					LOG.error(
							"failed to compile mapping transformation {}: {}",
							transformationInfo.getIdent(),
							e.getMessage());
				}
				mappingXPathCompiler = processor.newXPathCompiler();
				// compile and load Selene XPath function library
				try {
					XsltCompiler mappingXPathXsltCompiler = processor.newXsltCompiler();
					mappingXPathXsltCompiler.setResourceResolver(compileTimeResourceResolver);
					Source packageSource = compileTimeResourceResolver.resolve(mkXsltRequest(seleneXPathLibrary));
					XsltPackage functionLibrary = mappingXPathXsltCompiler.compilePackage(packageSource);
					mappingXPathCompiler.addXsltFunctionLibrary(functionLibrary);
					mappingXPathCompiler.declareNamespace("sel", seleneTracingLibraryNamespace);
					// forward default XPath
					FunctionLibrary functions =
							functionLibrary.getUnderlyingPreparedPackage().getPublicFunctions();
					SymbolicName.F configuredForwardDefault =
							new SymbolicName.F(StructuredQName.fromClarkName(seleneForwardXPathDefaultClarkName), 1);
					if (seleneForwardXPathDefaultClarkName.startsWith("Q{http://www.w3.org/2005/xpath-functions}")) {
						// functions from fn-namespace cannot be looked up in the library!
						StructuredQName qName = StructuredQName.fromClarkName(seleneForwardXPathDefaultClarkName);
						seleneForwardXPathDefault = qName.getLocalPart() + "(.)";
					} else if (!functions.isAvailable(configuredForwardDefault, 31)) {
						seleneForwardXPathDefault = "path(.)";
						LOG.error(
								"configuration error: selene-forward-default-xpath {} is not available. Using fallback instead: {}",
								seleneForwardXPathDefaultClarkName,
								seleneForwardXPathDefault);
					} else {
						StructuredQName qName = functions
								.getFunctionItem(
										configuredForwardDefault, mappingXPathCompiler.getUnderlyingStaticContext())
								.getFunctionName();
						seleneForwardXPathDefault = qName.getDisplayName() + "(.)";
					}
					LOG.info(
							"Selene default XPath for transformation {}: {}",
							transformationInfo.getIdent(),
							seleneForwardXPathDefault);
					// backward
					SymbolicName.F configuredBackwardDefault =
							new SymbolicName.F(StructuredQName.fromClarkName(seleneBackwardXPathDefaultClarkName), 1);
					if (seleneBackwardXPathDefaultClarkName.startsWith("Q{http://www.w3.org/2005/xpath-functions}")) {
						// functions from fn-namespace cannot be looked up in the library!
						StructuredQName qName = StructuredQName.fromClarkName(seleneBackwardXPathDefaultClarkName);
						seleneBackwardXPathDefault = qName.getLocalPart() + "(.)";
					} else if (!functions.isAvailable(configuredBackwardDefault, 31)
							|| seleneBackwardXPathDefaultClarkName.startsWith(
									"Q{http://www.w3.org/2005/xpath-functions}")) {
						seleneBackwardXPathDefault = "path(.)";
						LOG.error(
								"configuration error: selene-backward-default-xpath {} is not available. Using fallback instead: {}",
								seleneBackwardXPathDefaultClarkName,
								seleneBackwardXPathDefault);
					} else {
						StructuredQName qName = functions
								.getFunctionItem(
										configuredBackwardDefault, mappingXPathCompiler.getUnderlyingStaticContext())
								.getFunctionName();
						seleneBackwardXPathDefault = qName.getDisplayName() + "(.)";
					}
					LOG.info(
							"Selene default XPath for transformation {}: {}",
							transformationInfo.getIdent(),
							seleneBackwardXPathDefault);

				} catch (Exception e) {
					seleneForwardXPathDefault = "path(.)";
					seleneBackwardXPathDefault = "path(.)";
					LOG.error("failed to compile Selene XPath library: {}", e.getMessage());
				}
			}
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
	 * Internal method that does the transformation job.
	 */
	@Override
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
		transformer.setResourceResolver(new ChainingResourceResolver(compileTimeResourceResolver, resourceProvider));
		transformer.setUnparsedTextResolver(
				new ChainingUnparsedTextURIResolver(staticAssetsUnparsedTextURIResolver, resourceProvider));

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
		Map<String, ParameterValue> parametersMap = parameters.getGlobalParameters();
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
				XdmValue stringValue =
						new XdmAtomicValue(parametersMap.get(nameString).getFirst()); // TODO: support plural
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
							.convertString(
									StringView.of(parametersMap.get(nameString).getFirst())) // TODO: support plural
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
	public boolean canMapResource() {
		return mappingExecutable != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Class<? extends Point>, Class<? extends Point>> getPointClassMap(Rewriter.Direction direction) {
		return RewriterConfig.getPointClassMapForXslt(getSeleneOutputMethod(), direction);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RewriterFactory getRewriterFactory(Rewriter.Direction direction) {
		// This reuses the same XPath compiler for the whole lifetime of the transformation bean. According to the
		// Saxon documentation, this should be OK, although the compiler is used for compiling path expressions in
		// every Web Annotation selector etc.
		// See
		if (direction.equals(Rewriter.Direction.FORWARD)) {
			return new ForwardMappingFactory(mappingXPathCompiler);
		} else {
			return new BackwardMappingFactory(mappingXPathCompiler);
		}
	}

	@Override
	public String getRewriterConfigXPath(Rewriter.Direction direction) {
		if (direction.equals(Rewriter.Direction.FORWARD)) {
			return seleneForwardXPathDefault;
		} else {
			return seleneBackwardXPathDefault;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<MappedDOMResource> mapResourceAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			URI preimageIri,
			URI imageIri,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider,
			HttpServerRequest request) {
		if (mappingExecutable == null) {
			throw new BadRequestException("pointer transformation not available" + transformationInfo.getIdent());
		}
		return source.onItem()
				.transform(inputStream -> {
					ResourceBuilder resourceBuilder = new ResourceBuilder(processor);
					DOMResource resource;
					try {
						de.wwu.scdh.annotation.selection.Resource<?> parsed = resourceBuilder.parseResource(
								preimageIri, inputStream, systemId, ResourceBuilder.Parser.XML);
						return (DOMResource) parsed;
					} catch (de.wwu.scdh.annotation.selection.ResourceException e) {
						throw new InternalServerErrorException("failed to parse resource " + systemId);
					}
				})
				.onItem()
				.transform(resource -> {
					try {
						Xslt30Transformer transformer = mappingExecutable.load30();
						transformer.setStylesheetParameters(makeStylesheetParameters(parameters));
						// setting the global context item is required for global variables
						transformer.setGlobalContextItem(resource.getContents(), false);
						Class<? extends Point> pointerClass = ResourceBuilder.pointerClassFromOutputMethod(transformer);
						return ResourceBuilder.mapWithXslTransformation(imageIri, resource, transformer, pointerClass);
					} catch (de.wwu.scdh.annotation.selection.ResourceException e) {
						throw new InternalServerErrorException("failed to map resource " + systemId);
					} catch (SaxonApiException | TransformationPreparationException | TransformationException e) {
						LOG.error("failed to set stylesheet parameters to mapping transformation: {}", e.getMessage());
						throw new BadRequestException(
								"failed to set up transformation parameters: " + e.getMessage(), e);
					}
				});
	}
}
