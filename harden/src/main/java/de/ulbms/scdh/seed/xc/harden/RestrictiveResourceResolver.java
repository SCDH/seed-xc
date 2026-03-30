package de.ulbms.scdh.seed.xc.harden;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import net.sf.saxon.lib.ChainedResourceResolver;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.Processor;

/**
 * A resource resolver that chains together the FileURIResolver and
 * the ResourceResolver declared in the Saxon configuration. The
 * resulting resolver adds restriction to file system access on top of
 * the resolver declared in the configuration.
 */
@ApplicationScoped
public class RestrictiveResourceResolver extends ChainedResourceResolver {

	/**
	 * A dummy constructor needed for CDI. It must be present, but
	 * does not get called.
	 */
	public RestrictiveResourceResolver() {
		super(new DenyingResourceResolver(), new DenyingResourceResolver());
	}

	/**
	 * The constructor to use for this resolver.
	 */
	@Inject
	public RestrictiveResourceResolver(@Context FileURIResolver fileResourceResolver, @Context Processor processor) {
		super(fileResourceResolver, (ResourceResolver) processor.getConfigurationProperty(Feature.RESOURCE_RESOLVER));
	}
}
