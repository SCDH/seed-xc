package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.module.ResolutionException;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ResourceProviderBuilder} implementation for configuring and creating
 * {@link FileSystemResourceProvider} instances.<P/>
 *
 * Locations are interpreted as file paths. Relative paths
 * are resolved against the path in the <code>path</code>
 * configuration parameter. Absolute paths are interpreted as absolute
 * paths.<P>
 *
 * Security: Only file paths under the configured <code>path</code>
 * are accessible. For path to the outside, an exception is thrown.<P>
 *
 * Configuration properties:
 * <code>de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path</code>
 * takes the path configuration parameter. Relative path names are
 * resolved against the current user directory. */
@ApplicationScoped
public class FileSystemResourceProviderBuilder implements ResourceProviderBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemResourceProviderBuilder.class);

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
			defaultValue = "/")
	Path path;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return "file";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResourceProvider withBase(URI base) throws ResourceException {
		// resolving relative paths against the current
		// user directory with getAbsoluteFile()
		// simplifies testing and configuration.
		Path allowedPath = path.toAbsolutePath().normalize();
		if (base.isAbsolute()) {
			LOG.error(
					"only relative URIs are allowed  as base location parameter for the file system resource provider, {}",
					base);
			throw new ResolutionException(
					"only relative URIs are allowed as base location parameter for the file system resource provider");
		}
		// resolve base against path and normalize ../ and ./ segments
		Path basePath = allowedPath.resolve(base.toString()).normalize();
		// assert that resolved path is allowed since ../.. could lead to
		// unwanted access
		if (!basePath.startsWith(allowedPath)) {
			LOG.warn("denying access to {}", basePath);
			throw new ResourceException("not found");
		}
		LOG.info("configured file system resource provider with base path {}", basePath);
		return new FileSystemResourceProvider(basePath);
	}
}
