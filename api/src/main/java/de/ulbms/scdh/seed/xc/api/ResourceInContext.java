package de.ulbms.scdh.seed.xc.api;

public class ResourceInContext {

	private final String context;
	private final String resource;

	public ResourceInContext(String context, String resource) {
		this.context = context;
		this.resource = resource;
	}

	public String getContext() {
		return context;
	}

	public String getResource() {
		return resource;
	}
}
