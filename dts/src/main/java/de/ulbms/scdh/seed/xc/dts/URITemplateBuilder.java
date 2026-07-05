package de.ulbms.scdh.seed.xc.dts;

import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.InternalServerErrorException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class URITemplateBuilder {

	public static final String THIS_COLLECTION_TEMPLATE = "{?nav}";

	public static final String THIS_DOCUMENT_TEMPLATE = "{?tree,ref,start,end,mediaType}";

	public static final String THIS_NAVIGATION_TEMPLATE = "{?tree,ref,start,end,down,page}";

	public static final Map<String, String> RESOURCE_TEMPLATE = Map.of(
			"collection",
			THIS_COLLECTION_TEMPLATE,
			"document",
			THIS_DOCUMENT_TEMPLATE,
			"navigation",
			THIS_NAVIGATION_TEMPLATE);

	private static final Logger LOG = LoggerFactory.getLogger(URITemplateBuilder.class);

	/**
	 * Makes a URI template for a given endpoint from a request.
	 * @param request - the request URI
	 * @param endpoint - the endpoint, e.g. <code>navigation</code>
	 * @return the URI template
	 */
	public String resourceTemplate(HttpServerRequest request, String endpoint) {
		return resourceTemplate(request.absoluteURI(), endpoint);
	}

	/**
	 * Makes a URI template for a given endpoint from a request.
	 * @param request - the request URI
	 * @param endpoint - the endpoint, e.g. <code>navigation</code>
	 * @return the URI template
	 */
	public String resourceTemplate(String request, String endpoint) {
		try {
			return resourceTemplate(new URI(request), endpoint);
		} catch (URISyntaxException e) {
			LOG.error("failed to make a URI from {}", request);
			throw new InternalServerErrorException("failed to make a URI: " + request);
		}
	}

	/**
	 * Makes a URI template from a request URI for a given endpoint.
	 * @param request - the request URI
	 * @param endpoint - the endpoint, e.g. <code>navigation</code>
	 * @return the URI template
	 */
	public String resourceTemplate(URI request, String endpoint) {
		if (!RESOURCE_TEMPLATE.containsKey(endpoint)) {
			LOG.error("missing URI template for endpoint {}", endpoint);
			throw new InternalServerErrorException("missing URI template for endpoint " + endpoint);
		}
		String origPath = request.getRawPath();
		LOG.info("original path: {}", origPath);
		origPath = origPath.replace("%25", "%"); // escaped by new URI(...)
		// the /endpint/ path is the but-last part
		int endpointEndingSlash = origPath.lastIndexOf('/');
		int endpointStartingSlash = origPath.lastIndexOf('/', endpointEndingSlash - 1);
		String path = origPath.substring(0, endpointStartingSlash + 1) + endpoint;
		try {
			URI template = new URI(
					request.getScheme(),
					request.getRawUserInfo(),
					request.getHost(),
					request.getPort(),
					path,
					null,
					null);
			return template + origPath.substring(endpointEndingSlash) + RESOURCE_TEMPLATE.get(endpoint);
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make template from " + request);
		}
	}
}
