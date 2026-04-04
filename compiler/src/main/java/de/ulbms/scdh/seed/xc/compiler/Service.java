package de.ulbms.scdh.seed.xc.compiler;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.XslcApi;
import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
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
	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.service.Service.MAX_ZIP_SIZE", defaultValue = "10485760") // 10 MiB
	private Long MAX_ZIP_SIZE;

	/**
	 * As the {@link Service} class is request scoped, the injected
	 * {@link SaxonXslTransformation} with dependend scope is request
	 * scoped, too.
	 */
	@Inject
	SaxonXslTransformation transformation;

	/**
	 * Compile stylesheet given in a zip file. This is suitable for
	 * stylesheets with imports and includes packed altogether in the
	 * same zip file.
	 */
	@Override
	public Multi<Object> compileZip(String stylesheet, FileUpload zipUpload) {
		if (zipUpload.size() > MAX_ZIP_SIZE) {
			LOG.warn("zip file too large: ", zipUpload.size());
			return Multi.createFrom()
					.failure(
							new WebApplicationException("payload too large", Response.Status.REQUEST_ENTITY_TOO_LARGE));
		}
		try {
			ZipFile zip = new ZipFile(zipUpload.uploadedFile().toFile());
			// compile
			transformation.setup(zip, stylesheet, null);
			// export
			byte[] out = transformation.export("JS");
			zip.close();
			return Multi.createFrom().item(out);
		} catch (UnsupportedOperationException e) {
			LOG.error("not supported: {}", e.getMessage());
			return Multi.createFrom()
					.failure(new WebApplicationException(e.getMessage(), Response.Status.NOT_IMPLEMENTED));
		} catch (ConfigurationException e) {
			LOG.error("compilation failed: {}", e.getMessage());
			// OpenAPI returns 400 StylesheetNotFound, so we use BAD_REQUEST
			// instead of NOT_FOUND
			return Multi.createFrom()
					.failure(new WebApplicationException(
							"compilation failed: " + e.getMessage(), Response.Status.BAD_REQUEST));
		} catch (ZipException e) {
			LOG.error("failed to read zip file: {}", e.getMessage());
			return Multi.createFrom()
					.failure(new WebApplicationException(
							"cannot read zip file: " + e.getMessage(), Response.Status.BAD_REQUEST));
		} catch (IOException e) {
			LOG.error("IOException while reading zip file: {}", e.getMessage());
			return Multi.createFrom()
					.failure(new WebApplicationException(
							"error reading zip file", Response.Status.INTERNAL_SERVER_ERROR));
		}
	}
}
