package de.ulbms.scdh.seed.xc.service;

import java.io.File;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.io.IOException;

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

    // @Inject
    // SaxonXslTransformation transformation;

    @Override
    public Response bind(String transformationResource, String transformationId) {
	return Response.status(501).build();
    }

    @Override
    public Response compileZip(String stylesheet, File body) {
	try {
	    ZipFile zipFile = new ZipFile(body);

	    // transformation.setup(zipFile, stylesheet, null);

	    return Response.ok().build();

	// } catch (ConfigurationException e) {
	//     LOG.error("compilation failed: {}", e.getMessage());
	//     return Response.status(404, "cannot read zip file: " + e.getMessage()).build();
	} catch (ZipException e) {
	    LOG.error("failed to read zip file: {}", e.getMessage());
	    return Response.status(400, "cannot read zip file: " + e.getMessage()).build();
	} catch (IOException e) {
	    LOG.error("IOException while reading zip file: {}", e.getMessage());
	    return Response.status(422, "cannot read zip file: " + e.getMessage()).build();
	}
    }
}
