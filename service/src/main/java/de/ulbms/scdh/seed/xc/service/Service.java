package de.ulbms.scdh.seed.xc.service;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.XslcApi;
import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the SEED XC compiler's default REST API as
 * declared in the OpenAPI spec.
 */
@RequestScoped
public class Service implements XslcApi {

	private static final Logger LOG = LoggerFactory.getLogger(Service.class);

	@Inject
	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.service.Service.MAX_ZIP_SIZE",
					defaultValue = "10485760") // 10 MiB
	private Long MAX_ZIP_SIZE;

	/**
	 * As the {@link Service} class is request scoped, the injected
	 * {@link SaxonXsltransformation} with dependend scope is request
	 * scoped, too.
	 */
	@Inject SaxonXslTransformation transformation;

	/**
	 * Compile stylesheet given in a zip file. This is suitable for
	 * stylesheets with imports and includes packed altogether in the
	 * same zip file.
	 */
	@Override
	public Response compileZip(String stylesheet, File body) {
		if (body.length() > MAX_ZIP_SIZE) {
			LOG.warn("zip file too large: ", body.length());
			return RestResponse
				.status(Response.Status.REQUEST_ENTITY_TOO_LARGE,
						"payload too large")
				.toResponse();
		}
		try (ZipFile zipFile = new ZipFile(body)) {
			// compile
			transformation.setup(zipFile, stylesheet, null);
			// export
			byte[] out = transformation.export("JS");
			return Response.ok(out).build();
		} catch (UnsupportedOperationException e) {
			LOG.error("not supported: {}", e.getMessage());
			return RestResponse
				.status(Response.Status.NOT_IMPLEMENTED, e.getMessage())
				.toResponse();
		} catch (ConfigurationException e) {
			LOG.error("compilation failed: {}", e.getMessage());
			// OpenAPI returns 400 StylesheetNotFound, so we use BAD_REQUEST
			// instead of NOT_FOUND
			return RestResponse
				.status(Response.Status.BAD_REQUEST,
						"compilation failed: " + e.getMessage())
				.toResponse();
		} catch (ZipException e) {
			LOG.error("failed to read zip file: {}", e.getMessage());
			return RestResponse
				.status(Response.Status.BAD_REQUEST,
						"cannot read zip file: " + e.getMessage())
				.toResponse();
		} catch (IOException e) {
			LOG.error("IOException while reading zip file: {}", e.getMessage());
			return RestResponse
				.status(Response.Status.INTERNAL_SERVER_ERROR,
						"error reading zip file")
				.toResponse();
		}
	}
}
