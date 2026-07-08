package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.module.ResolutionException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ResourceProviderBuilder} implementation for configuring and creating
 * {@link FileSystemResourceProvider} instances.
 */
@ApplicationScoped
public class FileSystemResourceProviderBuilder implements ResourceProviderBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemResourceProviderBuilder.class);

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
			defaultValue = "/")
	String path;

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
	public ResourceProvider withBase(URI base) {
		Path allowedPath = Paths.get(path).toAbsolutePath().normalize();
		if (base.isAbsolute()) {
			LOG.error(
					"only relative URIs are allowed  as base location parameter for the file system resource provider, {}",
					base);
			throw new ResolutionException(
					"only relative URIs are allowed as base location parameter for the file system resource provider");
		}
		allowedPath.resolve(base.toString()).normalize();
		return new FileSystemResourceProvider(allowedPath.toString());
	}
}
