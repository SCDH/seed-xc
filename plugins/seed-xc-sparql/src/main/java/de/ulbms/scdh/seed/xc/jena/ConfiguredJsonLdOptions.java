package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.uri.UriValidationPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A bean for configuring JSON-LD options pass to the JSON-LD processor as {@link JsonLdOptions}.
 */
@ApplicationScoped
public class ConfiguredJsonLdOptions {

	@Inject
	protected DocumentLoader documentLoader;

	/**
	 * Determines the {@link UriValidationPolicy}.<P/>
	 *
	 * <code>"@base": null</code> for leaving relative IRIs as they are,
	 * is supported by setting this to <code>none</code>.
	 */
	@ConfigProperty(name = "jsonld-uri-validation-policy", defaultValue = "none")
	protected String uriValidationPolicy;

	/**
	 * This produces the {@link JsonLdOptions} for injection with {@link Inject}.
	 * @return the configured options
	 */
	@Produces
	public JsonLdOptions getJsonLdOptions() {
		JsonLdOptions options = new JsonLdOptions();
		options.setDocumentLoader(documentLoader);
		if (uriValidationPolicy.equalsIgnoreCase("none")) {
			options.setUriValidation(UriValidationPolicy.None);
		} else if (uriValidationPolicy.equalsIgnoreCase("schemeonly")) {
			options.setUriValidation(UriValidationPolicy.SchemeOnly);
		} else {
			options.setUriValidation(UriValidationPolicy.Full);
		}
		options.setCompactToRelative(true);
		return options;
	}
}
