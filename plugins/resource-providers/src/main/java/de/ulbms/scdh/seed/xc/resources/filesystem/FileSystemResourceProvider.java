package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ResourceProvider} that provides access to resources on the
 * local file system.<P>
 *
 * Resource identifiers are interpreted as file paths. Relative paths
 * are resolved against the path in the <code>path</code>
 * configuration parameter. Absolute paths are interpreted as absolute
 * paths.<P>
 *
 * Security: Only file paths under the configured <code>path</code>
 * are accessible. For path to the outside, an exception is thrown.
 */
@LookupIfProperty(
	name = "de.ulbms.scdh.seed.xc.api.ResourceProvider",
	stringValue =
		"de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider")
@ApplicationScoped
public class FileSystemResourceProvider implements ResourceProvider {

	public static final String NAME =
		"de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider";

	private static final Logger LOG =
		LoggerFactory.getLogger(FileSystemResourceProvider.class);

	private URI path = null;

	private Exception error = null;

	@Inject
	public FileSystemResourceProvider(
		@ConfigProperty(name = "de.ulbms.scdh.seed.xc.resources.filesystem."
							   + "FileSystemResourceProvider.path",
						defaultValue = "file:/") String path) {
		LOG.info("----------------- PATH ----------------: {}", path);
		try {
			if (path.startsWith("file:")) {
				this.path = Paths.get(new URI(path).normalize()).toUri();
			} else {
				this.path =
					Paths.get(new URI("file:" + path).normalize()).toUri();
			}
		} catch (URISyntaxException e) {
			LOG.error("invalid URI (path) for FileSystemResourceProvider: {}",
					  e.getMessage());
			error = e;
		} catch (NullPointerException e) {
			LOG.error("invalid URI (path) for FileSystemResourceProvider: {}",
					  e.getMessage());
			error = e;
		}
	}

	@Override
	public void setUp() {}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getSource(ResourceInContext ric)
		throws ResourceProviderConfigurationException,
			   ResourceNotFoundException, ResourceException {
		LOG.info("----------------- PATH ----------------: {}", path);
		if (error != null) {
			LOG.error("failed to setup: {}", error.getMessage());
			throw new ResourceProviderConfigurationException(error);
		}
		try {
			LOG.debug("resolving {}", ric.getResource());
			URI normalized =
				Paths.get(path.resolve(ric.getResource()).normalize()).toUri();
			LOG.debug("resolved {} to {}", ric.getResource(), normalized);
			if (!normalized.toString().startsWith(path.toString())) {
				throw new ResourceException("not allowed");
			}
			return normalized.toURL().openStream();
		} catch (MalformedURLException e) {
			throw new ResourceNotFoundException(e);
		} catch (IOException e) {
			throw new ResourceNotFoundException(e);
		}
	}
}
