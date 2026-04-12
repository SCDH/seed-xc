package de.ulbms.scdh.seed.xc.harden;

import java.io.Reader;
import java.net.URI;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.trans.XPathException;

/**
 * Albeit chaining is not a feature of {@link UnparsedTextURIResolver},
 * this resolver catches exceptions thrown by the first resolver and
 * delegates the request to the second one.
 */
public class ChainedUnparsedTextURIResolver implements UnparsedTextURIResolver {

	private final UnparsedTextURIResolver first, second;

	public ChainedUnparsedTextURIResolver(UnparsedTextURIResolver first, UnparsedTextURIResolver second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public Reader resolve(URI uri, String s, Configuration configuration) throws XPathException {
		try {
			return first.resolve(uri, s, configuration);
		} catch (XPathException e) {
			return second.resolve(uri, s, configuration);
		}
	}
}
