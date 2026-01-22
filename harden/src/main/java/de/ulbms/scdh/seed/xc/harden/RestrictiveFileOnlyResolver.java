package de.ulbms.scdh.seed.xc.harden;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;

import net.sf.saxon.lib.ChainedResourceResolver;


/**
 * A resource resolver that restricts access to the file system only
 * and the file system access is restricted to a configured path on
 * the base of {@link FileURIResolver}.
 *
 */
@ApplicationScoped
public class RestrictiveFileOnlyResolver extends ChainedResourceResolver {

    /**
     * A dummy constructor needed for CDI. It must be present, but
     * does not get called.
     */
    public RestrictiveFileOnlyResolver() {
	super(new DenyingResourceResolver(), new DenyingResourceResolver());
    }

    /**
     * The constructor to use for this resolver.
     */
    @Inject
    public RestrictiveFileOnlyResolver(@Context FileURIResolver fileResourceResolver) {
	super(fileResourceResolver, new DenyingResourceResolver());
    }

}
