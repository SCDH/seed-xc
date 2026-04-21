package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.CompileTime;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import org.apache.xerces.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * {@link TransformationBase} is abstract base class for Saxon-based transformation classes.
 * It provides common attributes and methods.
 */
public abstract class TransformationBase implements Transformation {

	Logger LOG = LoggerFactory.getLogger(TransformationBase.class);

	public static final String FEATURE_XINCLUDE = "http://apache.org/xml/features/xinclude";

	protected TransformationInfo transformationInfo;

	@Inject
	protected Processor processor;

	@CompileTime
	@Inject
	protected ResourceResolver compileTimeResourceResolver;

	@Inject
	protected UnparsedTextURIResolver staticAssetsUnparsedTextURIResolver;

	@Inject
	protected TransformationExceptionParser transformationExceptionParser;

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
	public String getOutputMediaType() {
		return transformationInfo.getMediaType();
	}

	/**
	 * Internal method that does the transformation job.
	 */
	protected abstract void transform(
			RuntimeParameters parameters,
			Config config,
			Source source,
			Serializer serializer,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException;

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

		// hand over to implementation of abstract method
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

		// calls abstract method
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
}
