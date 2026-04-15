package de.ulbms.scdh.seed.xc.saxon.harden;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.trans.XPathException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource resolver that restricts access to the file system only
 * and the file system access is restricted to a configured path on
 * the base of {@link FileURIResolver}.
 *
 */
@ApplicationScoped
public class RestrictiveFileOnlyModuleResolver implements ModuleURIResolver {

	private static final Logger LOG = LoggerFactory.getLogger(RestrictiveFileOnlyModuleResolver.class);

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.saxon.harden.FileURIResolver.path", defaultValue = "/")
	String allowed;

	@Override
	public StreamSource[] resolve(String moduleUri, String baseUri, String[] locations) throws XPathException {
		try {
			URI base = new URI(baseUri);
			URI module = base.resolve(moduleUri);
			String path = module.normalize().getSchemeSpecificPart();
			if (path.startsWith(allowed)) {
				InputStream input = module.toURL().openStream();
				StreamSource[] source = {new StreamSource(input)};
				source[0].setSystemId(path);
				return source;
			} else {
				LOG.error("illegal file system access: {}", path);
				throw new XPathException("illegal file system access: " + path);
			}
		} catch (MalformedURLException | URISyntaxException e) {
			LOG.error("bad module URI {}", baseUri);
			throw new XPathException("bad module URI " + e.getMessage(), e);
		} catch (IOException e) {
			// TODO: should locations be evaluated?
			LOG.error("failed to read XQuery module {}", moduleUri);
			throw new XPathException("Failed to read XQuery module: " + e.getMessage(), e);
		}
	}
}
