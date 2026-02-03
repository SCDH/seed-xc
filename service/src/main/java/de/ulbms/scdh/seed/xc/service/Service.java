package de.ulbms.scdh.seed.xc.service;

import java.io.File;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.io.IOException;
import java.lang.UnsupportedOperationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.RequestScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.api.DefaultApi;
import de.ulbms.scdh.seed.xc.xslt.SaxonXslTransformation;


@RequestScoped
public class Service implements DefaultApi {

    public static final Logger LOG = LoggerFactory.getLogger(Service.class);

    @Inject
    SaxonXslTransformation transformation;

    @Override
    public Response bind(String transformationResource, String transformationId) {
	return Response.status(501).build();
    }

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
	    return Response.status(Response.Status.NOT_IMPLEMENTED.getStatusCode(), e.getMessage()).build();
	} catch (ConfigurationException e) {
	    LOG.error("compilation failed: {}", e.getMessage());
	    return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "compilation failed:" + e.getMessage()).build();
	} catch (ZipException e) {
	    LOG.error("failed to read zip file: {}", e.getMessage());
	    return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read zip file: " + e.getMessage()).build();
	} catch (IOException e) {
	    LOG.error("IOException while reading zip file: {}", e.getMessage());
	    return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read zip file: " + e.getMessage()).build();
	}
    }
}
