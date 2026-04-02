package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
 * are accessible. For path to the outside, an exception is thrown.<P>
 *
 * Configuration properties:
 * <code>de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path</code>
 * takes the path configuration parameter. Relative path names are
 * resolved against the current user directory.
 */
@LookupIfProperty(
		name = "de.ulbms.scdh.seed.xc.api.ResourceProvider",
		stringValue = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider")
@ApplicationScoped
public class FileSystemResourceProvider implements ResourceProvider {

	public static final String NAME = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider";

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemResourceProvider.class);

	private URI path = null;

	private Exception error = null;

	@Inject
	public FileSystemResourceProvider(
			@ConfigProperty(
							name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
							defaultValue = "/")
					String path) {
		LOG.debug("setting up file system resource provider with path {}", path);
		try {
			// resolving relative paths against the current
			// user directory with getAbsoluteFile()
			// simplifies testing and configuration.
			this.path = Paths.get(path)
					.toFile()
					.getAbsoluteFile()
					.getCanonicalFile()
					.toURI();

		} catch (Exception e) {
			LOG.error("invalid path for FileSystemResourceProvider: {}", e.getMessage());
			error = e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getSource(ResourceInContext ric)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException {
		LOG.debug("getting source {} by resolving against {}", ric.getResource(), path);
		if (error != null) {
			LOG.error("failed to setup: {}", error.getMessage());
			throw new ResourceProviderConfigurationException(error);
		}
		try {
			URI normalized = path.resolve(ric.getResource()).normalize();
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<InputStream> getResource(Uni<ResourceInContext> resourceInContext) {
		if (error != null) {
			return resourceInContext.replaceWith(Uni.createFrom().failure(error));
		}

		return resourceInContext.onItem().transform((ric) -> {
			try {
				URI normalized = path.resolve(ric.getResource()).normalize();
				LOG.debug("resolved {} to {}", ric.getResource(), normalized);
				if (!normalized.toString().startsWith(path.toString())) {
					throw new jakarta.ws.rs.NotAllowedException("not allowed");
				}
				return normalized.toURL().openStream();
			} catch (MalformedURLException e) {
				throw new jakarta.ws.rs.BadRequestException(e);
			} catch (IOException e) {
				throw new jakarta.ws.rs.InternalServerErrorException(e);
			}
		});
	}
}
