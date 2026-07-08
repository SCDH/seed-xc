package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import de.ulbms.scdh.seed.xc.api.Context;
import de.ulbms.scdh.seed.xc.api.TransformationInfo;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import jakarta.enterprise.context.Dependent;
import jakarta.json.JsonStructure;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLConnection;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility for making JSON-LD context documents from a transformation info.
 */
@Dependent
public class JsonLdContext {

	private static final Logger LOG = LoggerFactory.getLogger(JsonLdContext.class);

	@ConfigProperty(name = "url-connect-timeout", defaultValue = "10000")
	int contextConnectTimeout;

	@ConfigProperty(name = "url-read-timeout", defaultValue = "10000")
	int contextReadTimeout;

	@ConfigProperty(name = "context-max-size", defaultValue = "1048576")
	long contextMaxSize;

	/**
	 * Indicates, if the context provider can provide a JSON-LD document for the given context.
	 * @param config - the {@link TransformationInfo} configuration
	 * @return whether a context can be provided
	 */
	public boolean providesContext(TransformationInfo config) {
		Context context = config.getContext();
		return context != null && (context.getLocation() != null || context.getDocument() != null);
	}

	/**
	 * Returns the JSON-LD context as a {@link Document}. This method encapsulates
	 * the preparation of the context including all options, e.g. setting a timeout
	 * and caching.
	 * @return the context {@link Document}
	 * @throws TransformationPreparationException when preparation failed
	 */
	public Document getContext(TransformationInfo config) throws TransformationPreparationException {
		Context context = config.getContext();
		if (context == null) {
			return null;
		}
		if (context.getDocument() != null) {
			return fromDocument(context);
		} else {
			return fromUri(context.getLocation());
		}
	}

	private Document fromDocument(Context context) throws TransformationPreparationException {
		ObjectMapper om = JsonMapper.builder().addModule(new JSONPModule()).build();
		JsonStructure jsonStructure = om.convertValue(context.getDocument(), JsonStructure.class);
		return JsonDocument.of(jsonStructure);
	}

	private Document fromUri(URI location) throws TransformationPreparationException {
		URLConnection connection;
		try {
			// toURL cannot not cause NPE because of way this method is used
			connection = location.toURL().openConnection();
			connection.setConnectTimeout(contextConnectTimeout);
			connection.setReadTimeout(contextReadTimeout);
		} catch (IOException | NullPointerException e) {
			LOG.error("JSON-LD framing URI not found {}", location);
			throw new TransformationPreparationException("JSON-LD framing URI not found " + location, e);
		}
		try (InputStream in = connection.getInputStream()) {
			if (contextMaxSize != 0 && connection.getContentLengthLong() > contextMaxSize) {
				throw new TransformationPreparationException("context exceeds size limit");
			}
			return JsonDocument.of(in);
		} catch (SocketTimeoutException e) {
			LOG.warn("timeout when reading JSON-LD context from {}", location);
			throw new TransformationPreparationException("timeout when reading JSON-LD context from " + location, e);
		} catch (JsonLdError | IOException e) {
			LOG.error("failed to read JSON-LD framing context {}", location);
			throw new TransformationPreparationException("failed to read JSON-LD framing context " + location, e);
		}
	}
}
