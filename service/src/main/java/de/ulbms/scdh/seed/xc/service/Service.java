package de.ulbms.scdh.seed.xc.service;

import java.io.File;

import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.Dependent;


import de.ulbms.scdh.seed.xc.api.DefaultApi;

@Dependent
public class Service implements DefaultApi {

    @Override
    public Response bind(String transformationResource, String transformationId) {
	return Response.ok().build();
    }

    @Override
    public Response compileZip(String stylesheet, File body) {
	return Response.ok().build();
    }
}
