package de.ulbms.scdh.seed.xc.harden;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;

/**
 * A resource resolver that resolves request to the contents of a zip
 * file.
 */
@Dependent
public class ZipFileURIResolver implements ResourceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ZipFileURIResolver.class);

    /**
     * Only paths under this path will be accessible through this resource resolver.
     */
    private ZipFile zipFile;

    /**
     * URI derived from {@link path}. It is used to resolve relative URIs.
     */
    private URI baseUri;

    /**
     * Make a new {@link FileURIResolver}.
     */
    public ZipFileURIResolver() {}

    /**
     * Sets the zip file.
     */
    public void setup(ZipFile zip, URI baseUri) throws ConfigurationException {
    if (zip == null) {
        LOG.error("cannot read zip archive: null");
        throw new ConfigurationException("Cannot read zip archive: null");
    }
    this.zipFile = zip;
    this.baseUri = baseUri;
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
        if (!uri.isAbsolute() && baseUri != null) {
        uri = baseUri.resolve(uri);
        LOG.debug("resolved {} in zip file to {}", request.uri, uri.toString());
        }

        // 3. normalize URI, i.e. process '.' and '..'
        uri = uri.normalize();

        // 4. add "file:" scheme if no scheme specified
        if (uri.getScheme() == null) {
        uri = new URI("file:" + uri.toString());
        //uri = new URI("file", uri.getSchemeSpecificPart(), uri.getFragment());
        }

        // check URI
        if (uri.getScheme().equals("file")) {
        // For some reason, uri.getPath() returns null. But for a file scheme,
        // uri.getSchemeSpecificPart() returns the path.
        if (uri.getSchemeSpecificPart() == null) {
            LOG.error("illegal file URI: null path (ssp) in {}", uri.toString());
            throw new XPathException("illegal file URI: null path (ssp)");
        }
        ZipEntry zipEntry = zipFile.getEntry(uri.getSchemeSpecificPart());
        if (zipEntry != null) {
            InputStream is = zipFile.getInputStream(zipEntry);
            return new StreamSource(is, uri.toString());
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
    } catch (Exception e) {
        LOG.error("cannot resolve URI {} in zip file: {}", request.uri, e.getMessage());
        throw new XPathException(e);
    }
    }
}
