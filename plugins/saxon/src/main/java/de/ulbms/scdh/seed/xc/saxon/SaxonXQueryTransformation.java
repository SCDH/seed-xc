package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.saxon.harden.ChainedUnparsedTextURIResolver;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.xml.transform.Source;
import net.sf.saxon.lib.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.str.StringView;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transformation using the Saxon XQuery processor. The query is
 * compiled once and then used throughout the lifecycle of the
 * service. Therefore, the bean, that creates an instance of this
 * class must be application scoped.
 */
@Dependent
public class SaxonXQueryTransformation extends TransformationBase implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(SaxonXQueryTransformation.class);

	public static final String TRANSFORMATION_TYPE = "xquery";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType() {
		return SaxonXQueryTransformation.TRANSFORMATION_TYPE;
	}

	@Inject
	protected ModuleURIResolver compileTimeModuleResolver;

	private XQueryExecutable executable;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(TransformationInfo transformationInfo, File configFile) throws ConfigurationException {
		LOG.debug("Setting up new SaxonXQueryTransformation with identifier '{}' ...", transformationInfo.getIdent());
		this.transformationInfo = transformationInfo;
		try {
			Path config = Paths.get(configFile.toURI());
			File query = config.resolve(transformationInfo.getLocation()).toFile();

			// Setting the systemId sets the static context (XML Base). It
			// is important for relative imports, but already done by the
			// resolver!

			// compile query to an executable that can be used
			// for an arbitrary number of transformations
			LOG.debug("Compiling from transformation info '{}' ...", query);
			XQueryCompiler compiler = processor.newXQueryCompiler();
			compiler.setFastCompilation(false);
			compiler.setModuleURIResolver(compileTimeModuleResolver);
			// set compile time parameters
			if (transformationInfo.getCompileTimeParameters() != null) {
				for (TypedParameter compileTimeParam : transformationInfo.getCompileTimeParameters()) {
					LOG.error(
							"There are no compile-time parameters for Saxon's XQuery Processor. Cannot set {}={}",
							compileTimeParam.getName(),
							compileTimeParam.getValue());
				}
			}
			// compile and import packages first
			if (transformationInfo.getLibraries() != null) {
				for (TransformationInfoLibrariesInner library : transformationInfo.getLibraries()) {
					LOG.debug("Compiling package {}", library.getLocation());
					try {
						File lib = config.resolve(library.getLocation()).toFile();
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

	private void setAtomicParameter(XQueryEvaluator evaluator, TypedParameter parameter, StringConverter converter) {
		try {
			AtomicValue atomicValue =
					converter.convertString(StringView.of(parameter.getValue())).asAtomic();
			XdmAtomicValue value = XdmAtomicValue.makeAtomicValue(atomicValue);
			evaluator.setExternalVariable(QName.fromClarkName(parameter.getName()), value);
		} catch (ValidationException e) {
			LOG.error("failed to convert external variable {}: {}", parameter.getName(), e.getMessage());
		} catch (SaxonApiUncheckedException e) {
			LOG.error("failed to set external {}: {}", parameter.getName(), e.getMessage());
		}
	}

	@Override
	public XsltParameterDetails getTransformationParameters() {
		return null; // TODO: How to get an iterator over external variables?
	}

	/**
	 * Internal method that does the transformation job.
	 */
	@Override
	protected synchronized void transform(
			RuntimeParameters parameters,
			Config config,
			Source source,
			Serializer serializer,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException {

		XQueryEvaluator evaluator = executable.load();

		// add file system restriction: access to the compiled
		// resources (with fn:static-base-uri()) is allowed as
		// well as access with the resource provider, e.g. for
		// XInclude.

		// 1. resource resolver for accessing XML via fn:doc() etc.
		evaluator.setResourceResolver(new ChainedResourceResolver(compileTimeResourceResolver, resourceProvider));
		evaluator.setUnparsedTextResolver(
				new ChainedUnparsedTextURIResolver(staticAssetsUnparsedTextURIResolver, resourceProvider));

		ConversionRules conversionRules = processor.getUnderlyingConfiguration().getConversionRules();
		StringConverter stringToStringConverter = new StringConverter.StringToString();

		try {
			if (parameters != null) {
				for (String name : parameters.getGlobalParameters().keySet()) {
					QName qname = QName.fromClarkName(name);
					XdmValue value;
					Optional<TypedParameter> declared = Optional.empty();
					for (TypedParameter p : getTransformationInfo().getCompileTimeParameters()) {
						if (p.getName().equals(name)) {
							declared = Optional.of(p);
							break;
						}
					}
					if (declared.isEmpty()) {
						// TODO: try to get type from default value: XdmValue defaultValue =
						// evaluator.getExternalVariable(qname);
						// assume xs:string
						value = XdmAtomicValue.makeAtomicValue(stringToStringConverter.convertString(
								StringView.of(parameters.getGlobalParameters().get(name))));
						evaluator.setExternalVariable(qname, value);
					} else {
						SchemaType schemaType = BuiltInType.getSchemaTypeByLocalName(
								declared.get().getType());
						if (schemaType == null) {
							// try xs:string
							setAtomicParameter(evaluator, declared.get(), stringToStringConverter);
						} else if (schemaType.isAtomicType()) {
							BuiltInAtomicType atomicType = (BuiltInAtomicType) schemaType;
							StringConverter converter = atomicType.getStringConverter(conversionRules);
							if (converter == null) {
								LOG.error(
										"failed to get converter for external variable {} of type {}",
										name,
										declared.get().getType());
							} else {
								this.setAtomicParameter(evaluator, declared.get(), converter);
							}
						} else {
							LOG.warn(
									"not implemented: failed to convert external variable {} of type {}",
									name,
									declared.get().getType());
							// TODO: convert BuildinListType
						}
					}
				}
			}
		} catch (SaxonApiUncheckedException e) {
			LOG.error("failed to set stylesheet parameters: {}", e.getMessage());
			throw new TransformationPreparationException(
					"failed to set up transformation parameters: " + e.getMessage(), e);
		}

		// TODO: evaluate evaluate initialTemplate and initialFunction from
		// runtime parameters
		try {
			if (getTransformationInfo().getRequiresSource()) {
				evaluator.setSource(source);
			}
			// transform
			evaluator.run(serializer);
		} catch (NullPointerException e) {
			LOG.error("no source defined, {}", e.getMessage());
			throw new TransformationException("no source defined", e);
		} catch (SaxonApiException e) {
			LOG.error("transformation failed: {}", e.getMessage());
			throw new TransformationException("transformation failed: " + e.getMessage(), e);
		}
	}
}
