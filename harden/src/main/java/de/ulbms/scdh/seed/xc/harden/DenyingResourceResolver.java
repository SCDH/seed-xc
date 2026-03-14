package de.ulbms.scdh.seed.xc.harden;

import javax.xml.transform.Source;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.trans.XPathException;

/**
 * This implementation of a {@link ResourceResolver} denies all
 * resolution requests. It is intended for chaining as a terminal
 * element in the chain.
 */
public class DenyingResourceResolver implements ResourceResolver {

	public Source resolve(ResourceRequest request) throws XPathException {
		throw new XPathException("illegal resource " + request.uri);
	}
}
