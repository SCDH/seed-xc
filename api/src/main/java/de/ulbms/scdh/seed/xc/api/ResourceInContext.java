package de.ulbms.scdh.seed.xc.api;

import java.util.Map;

public class ResourceInContext {

	private final Map<String, String> context;
	private final String resource;

	public ResourceInContext(Map<String, String> context, String resource) {
		this.context = context;
		this.resource = resource;
	}

	public Map<String, String> getContext() {
		return context;
	}

	public String getResource() {
		return resource;
	}
}
