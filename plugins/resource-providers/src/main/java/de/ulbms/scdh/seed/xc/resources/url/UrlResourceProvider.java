package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TransformTimeProvider
@LookupIfProperty(name = "seed-resource-provider", stringValue = "url")
@RequestScoped
public class UrlResourceProvider extends UrlValidator implements ResourceProvider {

	private static final Logger LOG = LoggerFactory.getLogger(UrlResourceProvider.class);

	private final URI base;

	/**
	 * Constructor used by the {@link UrlResourceProviderBuilder}.
	 */
	protected UrlResourceProvider(
			URI base, String allowedProtocols, String domainWhiteList, String domainBlackList, String allowedFilePath)
			throws ResourceProviderConfigurationException {
		this.base = base;
		this.allowedProtocols = allowedProtocols;
		this.domainWhiteList = domainWhiteList;
		this.domainBlackList = domainBlackList;
		this.allowedFilePath = allowedFilePath;
		configure();
	}

	@Inject
	protected HttpServerRequest request;

	/**
	 * Constructor for bean manager.
	 */
	public UrlResourceProvider() {
		try {
			base = new URI(request.absoluteURI());
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream openStream(URI uri)
			throws ResourceProviderConfigurationException, ResourceNotFoundException, ResourceException {
		configure();
		if (base == null) {
			throw new ResourceProviderConfigurationException("no base URL configured");
		}
		LOG.info("uri {}, resolving on {}", uri, base);
		URI resolved = base.resolve(uri);
		LOG.info("resolved {}", resolved);
		check(resolved);
		LOG.info("reading {}", resolved);
		try {
			URL resolvedUrl = resolved.toURL();
			// return new ByteArrayInputStream(".sdf".getBytes(Charset.defaultCharset()));
			return resolvedUrl.openStream();
		} catch (MalformedURLException e) {
			throw new ResourceException(e.getMessage());
		} catch (IOException e) {
			throw new ResourceNotFoundException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<InputStream> asyncOpenStream(Uni<ResourceInContext> resourceInContextUni, HttpServerRequest request) {
		return resourceInContextUni.onItem().transform((ric) -> {
			try {
				URI resolved = base.resolve(ric.getResource());
				return openStream(resolved);
			} catch (ResourceProviderConfigurationException e) {
				throw new InternalServerErrorException(e.getMessage());
			} catch (ResourceException | ResourceNotFoundException e) {
				throw new NotFoundException(e.getMessage());
			}
		});
	}
}
