package de.ulbms.scdh.seed.xc.harden;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.StandardUnparsedTextResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource resolver for <code>unparsed-text()</code> etc. It
 * restricts access to the file system to a specific path given by
 * configuration. Requests to URI schemes other than <code>file</code>
 * throw an exception.<P>
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
public class RestrictiveUnparsedTextResolver implements UnparsedTextURIResolver {

	private static final Logger LOG = LoggerFactory.getLogger(RestrictiveUnparsedTextResolver.class);

	/**
	 * Only paths under this path will be accessible through this resource
	 * resolver.
	 */
	private final String path;

	/**
	 * URI derived from <code>path</code>. It is used to resolve relative URIs.
	 */
	private final URI baseUri;

	public RestrictiveUnparsedTextResolver(
			@ConfigProperty(name = "de.ulbms.scdh.seed.xc.harden.FileURIResolver.path", defaultValue = "/") String path,
			@ConfigProperty(name = "de.ulbms.scdh.seed.xc.harden.FileURIResolver.baseUri", defaultValue = "/")
					String baseUri)
			throws ConfigurationException {

		// check preconditions
		if (path == null) {
			LOG.error("configuration error: path of FileURIResolver may not be null.");
			throw new ConfigurationException("configuration error: path of FileURIResolver may not be null.");
		} else if (path.startsWith("file:")) {
			LOG.error("configuration error: path of FileURIResolver may not start with 'file:'");
			throw new ConfigurationException("configuration error: path of FileURIResolver may not start with 'file:'");
		} else if (path.isEmpty()) {
			LOG.error("configuration error: path of FileURIResolver may not be the empty string");
			throw new ConfigurationException(
					"configuration error: path of FileURIResolver may not be the empty string");
		}

		try {
			this.path = normalizedBase(path).getSchemeSpecificPart();
		} catch (URISyntaxException e) {
			LOG.error("invalid path configured for FileURIResolver: {}", e.getMessage());
			throw new ConfigurationException("invalid path configured for FileURIResolver: " + e.getMessage());
		}
		LOG.info("allowed path of FileURIResolver configured to '{}'", path);
		LOG.info("allowed path of FileURIResolver set to '{}'", this.path);

		try {
			// make absolute
			this.baseUri = new File(baseUri).getAbsoluteFile().toURI().normalize();
		} catch (SecurityException e) {
			LOG.error("invalid configuration file: {}, {}", baseUri, e.getMessage());
			throw new ConfigurationException("invalid configuration file: " + baseUri, e);
		}
	}

	private static URI normalizedBase(final String path) throws URISyntaxException {
		String normalizedPath = path;
		// make absolute
		normalizedPath = new File(normalizedPath).getAbsolutePath();
		// assert path separator (/) at end
		if (!normalizedPath.endsWith("/") && !normalizedPath.endsWith(File.separator)) {
			// if path does not end with a path separator,
			// resolving against it will interpret the last path
			// segment as a file
			normalizedPath = normalizedPath + File.separator;
		}
		// normalize
		return new URI("file", normalizedPath, "").normalize();
	}

	/**
	 * Resolve the URI passed to the XSLT unparsed-text() function,
	 * after resolving against the base URI. If it is a file URI and
	 * not inside the allowed directory, an exceptions is thrown.
	 */
	@Override
	public Reader resolve(URI absoluteURI, String encoding, Configuration config) throws XPathException {
		LOG.debug("resolving unparsed text URI {}", absoluteURI.toString());

		try {
			URI uri = absoluteURI;

			// 2. make URI absolute, resolve relatives against config file
			if (!uri.isAbsolute()) {
				uri = this.baseUri.resolve(uri);
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
				} else if (uri.getPath().startsWith(this.path)) {
					// let the standard resolver do the work
					UnparsedTextURIResolver resolver = new StandardUnparsedTextResolver();
					return resolver.resolve(uri, encoding, config);
				} else {
					LOG.error("illegal file URI: {}", uri);
					throw new XPathException("illegal file URI: " + uri.toString());
				}
			} else {
				// be restrictive!
				throw new XPathException("illegal URI: " + absoluteURI.toString());
			}

		} catch (NullPointerException | IllegalArgumentException | URISyntaxException e) {
			LOG.error("illegal URI {}: {}", absoluteURI.toString(), e.getMessage());
			throw new XPathException(e);
		}
	}
}
