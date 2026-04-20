package de.ulbms.scdh.seed.xc.saxon;

import de.ulbms.scdh.seed.xc.api.Config;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import java.lang.reflect.Constructor;
import javax.xml.transform.Source;
import net.sf.saxon.s9api.DocumentBuilder;
import org.apache.xerces.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

public abstract class TransformationBase implements Transformation {

	Logger LOG = LoggerFactory.getLogger(TransformationBase.class);

	public static final String FEATURE_XINCLUDE = "http://apache.org/xml/features/xinclude";

	protected TransformationInfo transformationInfo;

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
