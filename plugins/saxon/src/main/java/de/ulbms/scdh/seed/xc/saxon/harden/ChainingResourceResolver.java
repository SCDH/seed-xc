package de.ulbms.scdh.seed.xc.saxon.harden;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProvider;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chains a {@link ResourceResolver} and a {@link ResourceProvider}.
 */
public class ChainingResourceResolver implements ResourceResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ChainingResourceResolver.class);

	private final ResourceResolver resourceResolver;

	private final ResourceProvider resourceProvider;

	public ChainingResourceResolver(ResourceResolver resourceResolver, ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
		this.resourceResolver = resourceResolver;
	}

	@Override
	public Source resolve(ResourceRequest resourceRequest) throws XPathException {
		try {
			return resourceResolver.resolve(resourceRequest);
		} catch (XPathException e) {
			// is a check of the nature of the request required?
			URI uri = URI.create(resourceRequest.uri);
			// is try-with-resource correct?
			try (InputStream in = resourceProvider.openStream(uri)) {
				return new StreamSource(in, uri.toString());
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
