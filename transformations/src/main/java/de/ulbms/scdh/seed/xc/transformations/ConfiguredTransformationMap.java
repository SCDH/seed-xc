package de.ulbms.scdh.seed.xc.transformations;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
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
public class ConfiguredTransformationMap extends HashMap<String, Transformation> implements TransformationMap {

	private static final Logger LOG = LoggerFactory.getLogger(ConfiguredTransformationMap.class);

	@Inject
	Instance<Transformation> transformationSelector;

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.transformations.ConfiguredTransformationMap."
					+ "ignoreInvalidTransformationTypes",
			defaultValue = "true")
	boolean ignoreInvalidTransformationTypes;

	private final ServiceLoader<Transformation> transformationLoader = ServiceLoader.load(Transformation.class);

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
			@ConfigProperty(
							name = "de.ulbms.scdh.seed.xc.transformations."
									+ "ConfiguredTransformationMap.configLocations",
							defaultValue = "")
					String configLocations)
			throws IOException, ConfigurationException {

		// we don't have any transformations
		if (configLocations != null && !configLocations.isEmpty()) {
			File configFile;
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

	void createTransformations(File configFile) throws IOException, ConfigurationException {
		LOG.info("Creating transformations defined in config file {}", configFile);

		// read the YAML config using jackson object mapper
		ObjectMapper om = new ObjectMapper(new YAMLFactory());

		de.ulbms.scdh.seed.xc.api.TransformationMap transformations;
		try {
			transformations = om.readValue(configFile, de.ulbms.scdh.seed.xc.api.TransformationMap.class);
		} catch (JsonParseException | JsonMappingException e) {
			LOG.error("Invalid configuration file: {}", e.getMessage());
			throw new ConfigurationException(e);
		}

		for (String transformationId : transformations.keySet()) {
			LOG.info("creating transformation '{}' ...", transformationId);

			TransformationInfo info = transformations.get(transformationId);
			// assert that there is an identifier in the info object
			info.setIdent(transformationId);

			String transformationType = info.getPropertyClass();

			// get all transformation plugins through the SPI
			Iterator<Transformation> transformationServices = transformationLoader.iterator();
			boolean serviceFound = false;
			while (transformationServices.hasNext()) {
				Transformation service = transformationServices.next();
				if (transformationType.equals(service.getType())) {
					// we create a bean and therefore use Instance#select(Class) to dynamically
					// create a transformation instance of a dynamically determined class
					Instance<? extends Transformation> transformationInstance =
							transformationSelector.select(service.getClass());
					Transformation transformation = transformationInstance.get();
					transformation.setup(info, configFile);
					this.put(transformationId, transformation);
					serviceFound = true;
					break;
				}
			}
			if (!serviceFound) {
				LOG.error("Unknown transformation type: {}", transformationType);
				if (!ignoreInvalidTransformationTypes) {
					throw new ConfigurationException("transformation '" + transformationId
							+ "' has unknown transformation type: "
							+ transformationType);
				}
			}
		}
	}
}
