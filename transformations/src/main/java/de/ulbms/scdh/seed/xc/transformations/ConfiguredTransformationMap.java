package de.ulbms.scdh.seed.xc.transformations;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transformation map created from a configuration file.
 *
 */
@Startup // create eagerly, i.e. at service startup
@ApplicationScoped
public class ConfiguredTransformationMap
	extends HashMap<String, Transformation> implements TransformationMap {

	private static final Logger LOG =
		LoggerFactory.getLogger(ConfiguredTransformationMap.class);

	@Inject Instance<Transformation> transformationSelector;

	@ConfigProperty(
		name =
			"de.ulbms.scdh.seed.xc.transformations.ConfiguredTransformationMap."
			+ "ignoreInvalidTransformationTypes",
		defaultValue = "true")
	boolean ignoreInvalidTransformationTypes;

	private ServiceLoader<Transformation> transformationLoader =
		ServiceLoader.load(Transformation.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Transformation get(String transformationId) {
		return super.get(transformationId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(String transformationId) {
		return super.containsKey(transformationId);
	}

	/**
	 * An initializer method that tries to find a configuration
	 * file. If it finds one, it passes it over to the creation method
	 * {@link ConfiguredTransformationMap#createTransformations(File)}.
	 *
	 * @param configLocations  a comma separated list of config locations
	 */
	@Inject
	void createTransformationsFromConfig(
		@ConfigProperty(name = "de.ulbms.scdh.seed.xc.transformations."
							   + "ConfiguredTransformationMap.configLocations",
						defaultValue = "") String configLocations)
		throws IOException, ConfigurationException {

		if (configLocations == null) {
			// we don't have any transformations
			return;
		} else if (configLocations.isEmpty()) {
			// we don't have any transformations
			return;
		} else {
			File configFile = null;
			for (String path : configLocations.split(",")) {
				path = path.trim();
				// config files may be placed in ~/.seed/..., so handle the
				// tilde
				path = path.replaceFirst("^~", System.getProperty("user.home"));
				configFile = new File(path);
				if (configFile.exists() && !configFile.isDirectory()) {
					this.createTransformations(configFile);
					break;
				} else {
					LOG.info("No config found at {}", path);
				}
			}
		}
	}

	void createTransformations(File configFile)
		throws IOException, ConfigurationException {
		LOG.info("Creating transformations defined in config file {}",
				 configFile);

		// read the yaml config using jackson object mapper
		ObjectMapper om = new ObjectMapper(new YAMLFactory());

		de.ulbms.scdh.seed.xc.api.TransformationMap transformations;
		try {
			transformations = om.readValue(
				configFile, de.ulbms.scdh.seed.xc.api.TransformationMap.class);
		} catch (JsonParseException e) {
			LOG.error("Invalid configuration file:\n{}", e);
			throw new ConfigurationException(e);
		} catch (JsonMappingException e) {
			LOG.error("Invalid configuration file:\n{}", e);
			throw new ConfigurationException(e);
		}

		for (String transformationId : transformations.keySet()) {
			LOG.info("creating transformation '{}' ...", transformationId);

			TransformationInfo info = transformations.get(transformationId);
			// assert that there is an identifier in the info object
			info.setIdent(transformationId);

			String transformationType = info.getPropertyClass();

			// We do not use the SPI pattern since building to native
			// does not work well with plugins
			// However, until version 0.5.0 the SPI pattern was used.

			switch (transformationType) {
			case SaxonXslTransformation.TRANSFORMATION_TYPE:
				Instance<? extends Transformation> transformationInstance =
					transformationSelector.select(SaxonXslTransformation.class);
				Transformation transformation = transformationInstance.get();
				transformation.setup(info);
				this.put(transformationId, transformation);
				break;
			default:
				LOG.error("Unknown transformation type: {}",
						  transformationType);
				if (!ignoreInvalidTransformationTypes) {
					throw new ConfigurationException(
						"transformation '" + transformationId +
						"' has unknown transformation type: " +
						transformationType);
				}
				break;
			}
		}
	}
}
