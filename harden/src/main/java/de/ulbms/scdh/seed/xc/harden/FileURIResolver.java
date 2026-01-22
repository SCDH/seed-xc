package de.ulbms.scdh.seed.xc.harden;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;

/**
 * A resource resolver that restricts access to the file system to a
 * specific path given by configuration. Requests to URI schemes other
 * than <code>file</code> will be delegated to the next resource
 * resolver. URIs without a specified scheme will be treated as in the
 * file scheme.
 *
 * Relative paths will be resolved to the resolved against the
 * <code>baseUri</code> argument given to the constructor. In the
 * managed bean container this will be set to the transformer's
 * configuration file, so that relative paths in there will be
 * resolved relative to the file.
 */
@ApplicationScoped
public class FileURIResolver implements ResourceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FileURIResolver.class);

    /**
     * Only paths under this path will be accessible through this resource resolver.
     */
    private final String path;

    /**
     * URI derived from {@link path}. It is used to resolve relative URIs.
     */
    private final URI baseUri;

    /**
     * Make a new {@link FileURIResolver}.
     *
     * @param path only the file system under this path {@link String} will be accessible
     * @param baseUri  path against which relative URIs will be resolved
     */
    public FileURIResolver
	(@ConfigProperty(name = "de.wwu.scdh.seed.xml.transform.saxon.harden.FileURIResolver.path", defaultValue = "/")
	 String path,
	 @ConfigProperty(name = "de.wwu.scdh.seed.xml.transform.ConfiguredTransformationMap.configLocations",
			 defaultValue = "transformer-config.yaml")
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
	    throw new ConfigurationException("configuration error: path of FileURIResolver may not be the empty string");
	}

	try {
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
	    URI uri = new URI("file", normalizedPath, "").normalize();
	    // store to field
	    this.path = uri.getSchemeSpecificPart();
	} catch (URISyntaxException e) {
	    LOG.error("invalid path configured for FileURIResolver: {}", e.getMessage());
	    throw new ConfigurationException("invalid path configured for FileURIResolver: " + e.getMessage());
	}
	LOG.info("allowed path of FileURIResolver configured to '{}'", path);
	LOG.info("allowed path of FileURIResolver set to '{}'", this.path);

	try {
	    String normalizedFile = baseUri;
	    // make absolute
	    this.baseUri = new File(normalizedFile).getAbsoluteFile().toURI().normalize();
	} catch (SecurityException e) {
	    LOG.error("invalid configuration file: {}, {}", baseUri, e.getMessage());
	    throw new ConfigurationException("invalid configuration file: " + baseUri, e);
	}

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Source resolve(ResourceRequest request) throws XPathException {
	LOG.debug("resolving {} URI {} ", request.nature, request.uri);
	// System.out.println(request.uri);

	try {
	    // 1. parse to URI instance
	    URI uri = new URI(request.uri);

	    // 2. resolve relative URIs against the configured path
	    if (!uri.isAbsolute()) {
		uri = this.baseUri.resolve(uri);
		LOG.debug("resolved {} on the base of {} to {}", request.uri, this.path, uri.toString());
		// System.out.println("resolved " + request.uri + " on the base of " + this.path + " to " + uri.toString());
	    }

	    // 3. normalize URI, i.e. process '.' and '..'
	    uri = uri.normalize();

	    // 4. add "file:" scheme if no scheme specified
	    if (uri.getScheme() == null) {
		//uri = new URI("file:" + uri.toString());
		uri = new URI("file", uri.getSchemeSpecificPart(), uri.getFragment());
	    }

	    // check URI
	    if (uri.getScheme().equals("file")) {
		if (uri.getPath() == null) {
		    LOG.error("illegal file URI: null path");
		    throw new XPathException("illegal file URI: null path");
		} else if (uri.getPath().startsWith(this.path)) {
		    File location = new File(uri);
		    return new StreamSource(location);
		} else {
		    LOG.error("illegal file URI: {}", uri.toString());
		    throw new XPathException("illegal file URI: " + uri.toString());
		}
	    } else {
		// delegate to the next resolver in the chain
		return null;
	    }
	} catch (NullPointerException e) {
	    LOG.error("illegal URI {}: {}", request.uri, e.getMessage());
	    throw new XPathException(e);
	} catch (IllegalArgumentException e) {
	    LOG.error("illegal URI {}: {}", request.uri, e.getMessage());
	    throw new XPathException(e);
	} catch (URISyntaxException e) {
	    LOG.error("illegal URI {}: {}", request.uri, e.getMessage());
	    throw new XPathException(e);
	}
    }
}
