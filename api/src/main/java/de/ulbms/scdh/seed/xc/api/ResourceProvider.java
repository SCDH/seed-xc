package de.ulbms.scdh.seed.xc.api;

import io.smallrye.mutiny.Uni;
import java.io.InputStream;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;

/**
 * A {@link ResourceProvider} is a plugin, that provides access
 * resources in sense in some persistence layer. Resource here is
 * taken in the sense DTS concepts: It is a document. This interface
 * makes no assumption about the format.<P/>
 *
 * A {@link ResourceProvider} is also a {@link ResourceResolver}
 * and a {@link UnparsedTextURIResolver}: After providing access
 * to the primary resource, the provider is passed to the
 * transformer for subsequent accesses to the persistence layer
 * during the transformation. This may be important e.g. for
 * processing XIncludes or linkage to registry files like
 * bibliographies etc. in non-self-contained interlinked files.
 *
 * Implementations should have the
 * {@link de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider}
 * qualifier assigned in order to avoid ambiguities with Saxon
 * resolver beans.
 */
public interface ResourceProvider extends ResourceResolver, UnparsedTextURIResolver {

	/**
	 * Returns a {@link InputStream} of a resource wrapped in a {@link Uni}.
	 *
	 * @param resourceInContextUni - Information for identifying the resource, wrapped in a {@link Uni}
	 */
	Uni<InputStream> getResource(Uni<ResourceInContext> resourceInContextUni);
}
