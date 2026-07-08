package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
@TransformTimeProvider
@LookupIfProperty(
		name = "de.ulbms.scdh.seed.xc.api.ResourceProvider",
		stringValue = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider")
@ApplicationScoped
public class FileSystemResourceProvider implements ResourceProvider {

	public static final String NAME = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider";

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemResourceProvider.class);

	private Path path = null;

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
			this.path = Paths.get(path).toAbsolutePath().normalize();
		} catch (Exception e) {
			LOG.error("invalid path for FileSystemResourceProvider: {}", e.getMessage());
			error = e;
		}
		LOG.info("file system provider based on {}", this.path);
	}

	public Exception getError() {
		return error;
	}

	@Override
	public InputStream openStream(URI uri)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException {
		LOG.debug("getting source {} by resolving against {}", uri, path);
		if (error != null) {
			LOG.error("failed to setup: {}", error.getMessage());
			throw new ResourceProviderConfigurationException(error);
		}
		if (uri.getScheme() != null) if (!uri.getScheme().equals("file")) throw new ResourceException("not allowed");
		Path filePath;
		try {
			filePath = path.resolve(uri.getSchemeSpecificPart()).normalize();
			if (!filePath.startsWith(path)) {
				LOG.warn("denying access to {}", uri);
				throw new ResourceException("not found");
			}
			return new FileInputStream(filePath.toFile());
		} catch (InvalidPathException e) {
			LOG.warn("invalid path {}", uri);
			throw new ResourceNotFoundException("not found");
		} catch (IOException e) {
			throw new ResourceNotFoundException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<InputStream> asyncOpenStream(Uni<ResourceInContext> resourceInContext, HttpServerRequest request) {
		if (error != null) {
			return resourceInContext.replaceWith(Uni.createFrom().failure(error));
		}

		return resourceInContext.onItem().transform((ric) -> {
			try {
				URI uri = new URI(ric.getResource());
				return openStream(uri);
			} catch (URISyntaxException e) {
				throw new jakarta.ws.rs.BadRequestException(e);
			} catch (ResourceProviderConfigurationException e) {
				throw new jakarta.ws.rs.InternalServerErrorException(e);
			} catch (ResourceException | ResourceNotFoundException e) {
				throw new jakarta.ws.rs.NotFoundException(e);
			}
		});
	}
}
