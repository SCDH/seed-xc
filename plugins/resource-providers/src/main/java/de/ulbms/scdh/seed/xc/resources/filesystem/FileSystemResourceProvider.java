package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
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

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.resources.filesystem."
						   + "FileSystemResourceProvider.path",
					defaultValue = "/")
	private String pathConfigured;

	private URI path;

	private Exception error = null;

	public FileSystemResourceProvider() { this.setUp(); }

	@Override
	public void setUp() {
		LOG.info("----------------- PATH ----------------: {}", pathConfigured);
		try {
			this.path = Paths.get(new URI(pathConfigured).normalize()).toUri();
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
		if (error != null)
			throw new ResourceProviderConfigurationException(error);
		try {
			URI normalized = path.resolve(ric.getResource()).normalize();
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
