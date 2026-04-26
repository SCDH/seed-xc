package de.ulbms.scdh.seed.xc.saxon.harden;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Albeit chaining is not a feature of {@link UnparsedTextURIResolver},
 * this resolver catches exceptions thrown by the first resolver and
 * delegates the resource provider.
 */
public class ChainingUnparsedTextURIResolver implements UnparsedTextURIResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ChainingUnparsedTextURIResolver.class);

	private final UnparsedTextURIResolver unparsedTextResolver;

	private final ResourceProvider resourceProvider;

	public ChainingUnparsedTextURIResolver(
			UnparsedTextURIResolver unparsedTextResolver, ResourceProvider resourceProvider) {
		this.unparsedTextResolver = unparsedTextResolver;
		this.resourceProvider = resourceProvider;
	}

	@Override
	public Reader resolve(URI uri, String encoding, Configuration configuration) throws XPathException {
		try {
			return unparsedTextResolver.resolve(uri, encoding, configuration);
		} catch (XPathException e) {
			// is it correct to use try-with-resource?
			try (Reader reader = new InputStreamReader(resourceProvider.openStream(uri), encoding)) {
				return reader;
			} catch (UnsupportedEncodingException err) {
				LOG.error("unsupported encoding {}", encoding);
				throw new XPathException(err);
			} catch (ResourceProviderConfigurationException
					| ResourceNotFoundException
					| ResourceException
					| IOException err) {
				LOG.error("failed to read {}: {}", uri, err.getMessage());
				throw new XPathException(err);
			}
		}
	}
}
