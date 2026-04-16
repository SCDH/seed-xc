package de.ulbms.scdh.seed.xc.resources.filesystem;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceInContext;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.StandardUnparsedTextResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;
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
		LOG.info("file system provider based on {}", this.path);
	}

	public Exception getError() {
		return error;
	}

	private InputStream getStream(URI uri)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException {
		LOG.debug("getting source {} by resolving against {}", uri, path);
		if (error != null) {
			LOG.error("failed to setup: {}", error.getMessage());
			throw new ResourceProviderConfigurationException(error);
		}
		try {
			URI normalized = path.resolve(uri).normalize();
			LOG.debug("resolved {} to {}", uri, normalized);
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
				throw new jakarta.ws.rs.NotFoundException(e);
			}
		});
	}

	/**
	 * This implements the {@link net.sf.saxon.lib.ResourceResolver} interface.
	 * If the resource can be found in the configured part of the file system,
	 * then it is returned as a {@link StreamSource}. Otherwise, <code>null</code>
	 * is returned, which is the signal that the request is delegated to the next
	 * resolver.
	 *
	 * @param resourceRequest - the requested resource
	 * @return - a {@link StreamSource} or <code>null</code>
	 * @throws XPathException
	 * @see net.sf.saxon.lib.ResourceResolver
	 */
	@Override
	public Source resolve(ResourceRequest resourceRequest) throws XPathException {
		try {
			// is a check of the nature of the request required?
			return new StreamSource(getStream(URI.create(resourceRequest.uri)), resourceRequest.uri.toString());
		} catch (ResourceProviderConfigurationException e) {
			LOG.error("resource provider badly configured, delegating request for {}");
			return null;
		} catch (ResourceNotFoundException | ResourceException e) {
			// delegate the resolution to the next resolver
			return null;
		}
	}

	/**
	 * This implements the {@link UnparsedTextURIResolver} interface.
	 * If the resource can be found in the configured part of the file system,
	 * then it is returned as a {@link Reader}. Otherwise: Since returning
	 * <code>null</code> is not valid according to this interface, an exception
	 * is thrown. It's up to further application code to handle it and to decide,
	 * whether a lookup with another resolver is to be done.
	 *
	 * @param absoluteURI
	 * @param encoding
	 * @param config
	 * @return
	 * @throws XPathException
	 * @see UnparsedTextURIResolver
	 */
	@Override
	public Reader resolve(final URI absoluteURI, String encoding, Configuration config) throws XPathException {

		LOG.debug("resolving unparsed text URI {}", absoluteURI.toString());

		try {
			URI uri = absoluteURI;

			// 2. make URI absulute, resolve relatives against config file
			if (!uri.isAbsolute()) {
				uri = path.resolve(uri);
				uri = new URI("file", uri.getSchemeSpecificPart(), uri.getFragment());
			}

			// 3. normalize URI, i.e. process '.' and '..'
			uri = uri.normalize();

			// 4. add "file:" scheme if no scheme specified
			if (uri.getScheme() == null) {
				// uri = new URI("file:" + uri.toString());
				uri = new URI("file", uri.getSchemeSpecificPart(), uri.getFragment());
			}

			// check URI
			if (uri.getScheme().equals("file")) {
				if (uri.getPath() == null) {
					LOG.error("illegal file URI: null path");
					throw new XPathException("illegal file URI: null path");
				} else if (uri.getPath().startsWith(this.path.getPath())) {
					// let the standard resolver do the work
					UnparsedTextURIResolver standardResolver = new StandardUnparsedTextResolver();
					return standardResolver.resolve(uri, encoding, config);
				} else {
					LOG.error("illegal file URI: {}", uri.toString());
					throw new XPathException("illegal file URI: " + uri.toString());
				}
			} else {
				throw new XPathException("unparsed resource is not a file resource");
			}

		} catch (NullPointerException | IllegalArgumentException | URISyntaxException e) {
			LOG.error("illegal URI {}: {}", absoluteURI.toString(), e.getMessage());
			throw new XPathException(e);
		}
	}
}
