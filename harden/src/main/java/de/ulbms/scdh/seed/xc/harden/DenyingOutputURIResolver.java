package de.ulbms.scdh.seed.xc.harden;

import javax.xml.transform.TransformerException;
import javax.xml.transform.Result;

import io.quarkus.runtime.annotations.RegisterForReflection;

import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.trans.XPathException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link OutputURIResolver} that exits on all resolutions with an
 * exception. It can be used to disallow a writing of
 * <code>&lg;xsl:result-document&gt;</code> in a general way.
 */
@RegisterForReflection
public class DenyingOutputURIResolver implements OutputURIResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DenyingOutputURIResolver.class);

    public DenyingOutputURIResolver() {
    }

    /**
     * Allways throws an exception.
     */
    @Override
    public Result resolve(String href, String base) throws XPathException {
    LOG.warn("a transformation tries to resolve {} on the base of {}! Denying", href, base);
    throw new XPathException("URI not allowed: " + href + " resolved in " + base);
    }

    @Override
    public void close(Result result) throws TransformerException {
    LOG.warn("a transformation tries to close the result {}! Denying", result.getSystemId());
    throw new TransformerException("URI not allowed: " + result.getSystemId());
    }

    @Override
    public OutputURIResolver newInstance() {
    return new DenyingOutputURIResolver();
    }

}
