package de.ulbms.scdh.seed.xc.service;

import java.io.File;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.io.IOException;
import java.lang.UnsupportedOperationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.RequestScoped;
import org.jboss.resteasy.reactive.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.DefaultApi;
import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;


/**
 * An implementation of the SEED XC compiler's default REST API as
 * declared in the OpenAPI spec.
 */
@RequestScoped
public class Service implements DefaultApi {

    private static final Logger LOG = LoggerFactory.getLogger(Service.class);

    /**
     * As the {@link Service} class is request scoped, the injected
     * {@link SaxonXsltransformation} with dependend scope is request
     * scoped, too.
     */
    @Inject
    SaxonXslTransformation transformation;

    /**
     * Binds the transformation ID to a compiled transformation
     * resource. This only makes sense on a service that offers
     * transformation, not only compilation. Since the implementation
     * at hand does not offer transformation routes, it returns 501.
     */
    @Override
    public Response bind(String transformationResource, String transformationId) {
	return Response.status(501).build();
    }

    /**
     * Compile stylesheet given in a zip file. This is suitable for
     * stylesheets with imports and includes packed altogether in the
     * same zip file.
     */
    @Override
    public Response compileZip(String stylesheet, File body) {
	try {
	    ZipFile zipFile = new ZipFile(body);
	    // compile
	    transformation.setup(zipFile, stylesheet, null);
	    // export
	    byte[] out = transformation.export("JS");
	    return Response.ok(out).build();
	} catch (UnsupportedOperationException e) {
	    LOG.error("not supported: {}", e.getMessage());
	    // we use RestResponse because it makes the error message occur in response body
	    RestResponse<String> response =
		RestResponse.status(Response.Status.NOT_IMPLEMENTED, e.getMessage());
	    return response.toResponse();
	} catch (ConfigurationException e) {
	    LOG.error("compilation failed: {}", e.getMessage());
	    RestResponse<String> response =
		RestResponse.status(Response.Status.NOT_FOUND, "compilation failed:" + e.getMessage());
	    return response.toResponse();
	} catch (ZipException e) {
	    LOG.error("failed to read zip file: {}", e.getMessage());
	    RestResponse<String> response =
		RestResponse.status(Response.Status.BAD_REQUEST, "cannot read zip file: " + e.getMessage());
	    return response.toResponse();
	} catch (IOException e) {
	    LOG.error("IOException while reading zip file: {}", e.getMessage());
	    RestResponse<String> response =
		RestResponse.status(Response.Status.BAD_REQUEST, "cannot read zip file: " + e.getMessage());
	    return response.toResponse();
	}
    }
}
