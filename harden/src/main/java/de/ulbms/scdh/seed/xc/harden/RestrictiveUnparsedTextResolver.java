package de.ulbms.scdh.seed.xc.harden;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.StandardUnparsedTextResolver;
import net.sf.saxon.trans.XPathException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource resolver for <code>unparsed-text()</code> etc. It
 * restricts access to the file system to a specific path given by
 * configuration. Requests to URI schemes other than <code>file</code>
 * will be delegated to the {@link StandardUnparsedTextResolver}.<P>
 *
 * URIs without a specified scheme will be treated as in the file
 * scheme.<P>
 *
 * While the {@link UnparsedTextURIResolver} only requires a resolver
 * to resolve absolute URIs, this resolver resolves relative URIs
 * against the <code>baseUri</code> argument given to the
 * constructor. In the managed bean container this will be set to the
 * transformer's configuration file, so that relative paths in there
 * will be resolved relative to the file.<P>
 *
 * See also: {@link FileURIResolver}
 */
@ApplicationScoped
public class RestrictiveUnparsedTextResolver
	extends StandardUnparsedTextResolver {

	private static final Logger LOG =
		LoggerFactory.getLogger(FileURIResolver.class);

	/**
	 * Only paths under this path will be accessible through this resource
	 * resolver.
	 */
	private final String path;

	/**
	 * URI derived from {@link path}. It is used to resolve relative URIs.
	 */
	private final URI baseUri;

	// /**
	//  * A dummy constructor needed for CDI. It must be present, but
	//  * does not get called.
	//  */
	// public RestrictiveUnparsedTextResolver() {
	// 	super();
	// }

	public RestrictiveUnparsedTextResolver(
		@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.harden.FileURIResolver.path",
			defaultValue = "/") String path,
		@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.harden.FileURIResolver.baseUri",
			defaultValue = "/") String baseUri) throws ConfigurationException {

		super();

		// check preconditions
		if (path == null) {
			LOG.error("configuration error: path of FileURIResolver may not "
					  + "be null.");
			throw new ConfigurationException(
				"configuration error: path of FileURIResolver may not be "
				+ "null.");
		} else if (path.startsWith("file:")) {
			LOG.error(
				"configuration error: path of FileURIResolver may not start "
				+ "with 'file:'");
			throw new ConfigurationException(
				"configuration error: path of FileURIResolver may not start "
				+ "with "
				+ "'file:'");
		} else if (path.isEmpty()) {
			LOG.error(
				"configuration error: path of FileURIResolver may not be the "
				+ "empty string");
			throw new ConfigurationException(
				"configuration error: path of FileURIResolver may not be the "
				+ "empty "
				+ "string");
		}

		try {
			String normalizedPath = path;
			// make absolute
			normalizedPath = new File(normalizedPath).getAbsolutePath();
			// assert path separator (/) at end
			if (!normalizedPath.endsWith("/") &&
				!normalizedPath.endsWith(File.separator)) {
				// if path does not end with a path separator,
				// resolving against it will interpret the last path
				// segment as a file
				normalizedPath = normalizedPath + File.separator;
			}
			// normalize
			URI uri = new URI("file", normalizedPath, "").normalize();
			// store to field
			this.path = uri.getSchemeSpecificPart();
		} catch (URISyntaxException e) {
			LOG.error("invalid path configured for FileURIResolver: {}",
					  e.getMessage());
			throw new ConfigurationException(
				"invalid path configured for FileURIResolver: " +
				e.getMessage());
		}
		LOG.info("allowed path of FileURIResolver configured to '{}'", path);
		LOG.info("allowed path of FileURIResolver set to '{}'", this.path);

		try {
			String normalizedFile = baseUri;
			// make absolute
			this.baseUri =
				new File(normalizedFile).getAbsoluteFile().toURI().normalize();
		} catch (SecurityException e) {
			LOG.error("invalid configuration file: {}, {}", baseUri,
					  e.getMessage());
			throw new ConfigurationException(
				"invalid configuration file: " + baseUri, e);
		}
	}

	/**
	 * Resolve the URI passed to the XSLT unparsed-text() function,
	 * after resolving against the base URI. If it is a file URI and
	 * not inside the allowed directory, an exceptions is thrown.
	 */
	@Override
	public Reader resolve(URI absoluteURI, String encoding,
						  Configuration config) throws XPathException {
		LOG.debug("resolving unparsed text URI {}", absoluteURI.toString());

		try {
			URI uri = absoluteURI;

			// 2. make URI absulute, resolve relatives against config file
			if (!uri.isAbsolute()) {
				uri = this.baseUri.resolve(uri);
				uri = new URI("file", uri.getSchemeSpecificPart(),
							  uri.getFragment());
			}

			// 3. normalize URI, i.e. process '.' and '..'
			uri = uri.normalize();

			// 4. add "file:" scheme if no scheme specified
			if (uri.getScheme() == null) {
				// uri = new URI("file:" + uri.toString());
				uri = new URI("file", uri.getSchemeSpecificPart(),
							  uri.getFragment());
			}

			// check URI
			if (uri.getScheme().equals("file")) {
				if (uri.getPath() == null) {
					LOG.error("illegal file URI: null path");
					throw new XPathException("illegal file URI: null path");
				} else if (uri.getPath().startsWith(this.path)) {
					// let the standard resolver do the work
					return super.resolve(uri, encoding, config);
				} else {
					LOG.error("illegal file URI: {}", uri.toString());
					throw new XPathException("illegal file URI: " +
											 uri.toString());
				}
			} else {
				// delegate to the standard resolver
				return super.resolve(uri, encoding, config);
			}

		} catch (NullPointerException e) {
			LOG.error("illegal URI {}: {}", absoluteURI.toString(),
					  e.getMessage());
			throw new XPathException(e);
		} catch (IllegalArgumentException e) {
			LOG.error("illegal URI {}: {}", absoluteURI.toString(),
					  e.getMessage());
			throw new XPathException(e);
		} catch (URISyntaxException e) {
			LOG.error("illegal URI {}: {}", absoluteURI.toString(),
					  e.getMessage());
			throw new XPathException(e);
		}
	}
}
