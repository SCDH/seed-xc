package de.ulbms.scdh.seed.xc.dts.v1_0;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.URITemplateBuilder;
import de.ulbms.scdh.seed.xc.dts.endpoints.EntryApi;
import de.ulbms.scdh.seed.xc.dts.model.Entry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.net.URI;

@RequestScoped
public class EntryEndpoint implements EntryApi {

	@TransformTimeProvider
	@Inject
	ResourceProviderManager resourceProviderManager;

	@Inject
	HttpServerRequest request;

	@Inject
	URITemplateBuilder templateBuilder;

	@Override
	public Uni<Entry> entry(URI location, URI provider) {

		ResourceProvider resourceProvider;
		try {
			ResourceProviderBuilder resourceProviderBuilder = resourceProviderManager.get(location.toString());
			resourceProvider = resourceProviderBuilder.withBase(location);
		} catch (ResourceProviderConfigurationException e) {
			throw new BadRequestException("no resource provider type " + provider);
		} catch (ResourceNotFoundException e) {
			throw new NotFoundException("not found");
		} catch (ResourceException e) {
			throw new BadRequestException(e.getMessage());
		}
		if (resourceProvider == null) {
			throw new NotFoundException("not found");
		}

		Entry entry = new Entry();
		entry.atId(request.absoluteURI());
		entry.atType("EntryEndpoint");
		entry.dtsVersion(URITemplateBuilder.DTS_VERSION);
		String base = request.absoluteURI().substring(0, request.absoluteURI().lastIndexOf("/entry")) + "/";
		entry.collection(base + URITemplateBuilder.COLLECTION_TEMPLATE);
		entry.navigation(base + URITemplateBuilder.NAVIGATION_TEMPLATE);
		entry.document(base + URITemplateBuilder.DOCUMENT_TEMPLATE);
		entry.atContext(templateBuilder.getEntryContext());
		return Uni.createFrom().item(entry);
	}
}
