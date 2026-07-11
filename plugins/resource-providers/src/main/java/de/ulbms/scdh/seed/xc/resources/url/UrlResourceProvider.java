package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TransformTimeProvider
@LookupIfProperty(name = "seed-resource-provider", stringValue = "url")
@RequestScoped
public class UrlResourceProvider extends UrlValidator implements ResourceProvider {

	private static final Logger LOG = LoggerFactory.getLogger(UrlResourceProvider.class);

	@Inject
	protected UrlConfig config;

	private final URI base;

	/**
	 * Stores errors in early phase.
	 */
	private Exception error = null;

	/**
	 * Constructor used by the {@link UrlResourceProviderBuilder}.
	 */
	protected UrlResourceProvider(URI base, UrlConfig config) throws ResourceProviderConfigurationException {
		this.base = base;
		this.config = config;
		configure(config);
	}

	/**
	 * Constructor used by bean manager. This sets the base to the file URI of the allowed system path.
	 * @param path - allowed file system path
	 */
	@Inject
	public UrlResourceProvider(
			@ConfigProperty(
							name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
							defaultValue = "/")
					String path) {
		LOG.debug("setting up file system resource provider with path {}", path);
		try {
			// resolving relative paths against the current
			// user directory with getAbsoluteFile()
			// simplifies testing and configuration.
			this.base = Paths.get(path).toAbsolutePath().normalize().toUri();
			LOG.debug("file system provider based on {}", this.base);
		} catch (Exception e) {
			LOG.error("invalid path for FileSystemResourceProvider: {}", e.getMessage());
			error = e;
		}
		throw new RuntimeException("too bad!");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream openStream(URI uri)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException {
		configure(config);
		if (error != null) {
			throw new ResourceProviderConfigurationException(error.getMessage());
		}
		if (base == null) {
			throw new ResourceProviderConfigurationException("no base URL configured");
		}
		URI resolved = base.resolve(uri);
		check(resolved);
		try {
			URL resolvedUrl = resolved.toURL();
			URLConnection conn = resolvedUrl.openConnection();
			conn.setConnectTimeout(config.connectTimeout);
			conn.setReadTimeout(config.readTimeout);
			conn.connect();
			return conn.getInputStream();
		} catch (MalformedURLException e) {
			throw new ResourceException(e.getMessage());
		} catch (IOException e) {
			throw new ResourceNotFoundException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<InputStream> asyncOpenStream(Uni<ResourceInContext> resourceInContextUni, HttpServerRequest request) {
		return resourceInContextUni.onItem().transform((ric) -> {
			try {
				URI resolved = base.resolve(ric.getResource());
				return openStream(resolved);
			} catch (ResourceProviderConfigurationException e) {
				throw new InternalServerErrorException(e.getMessage());
			} catch (ResourceException | ResourceNotFoundException e) {
				throw new NotFoundException(e.getMessage());
			}
		});
	}
}
