package de.ulbms.scdh.seed.xc.dts;

import de.ulbms.scdh.seed.xc.api.Transformation;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class NavigationEndpoint implements NavigationApi {

	@ConfigProperty(
		name = "de.ulbms.scdh.seed.xc.dts.NavigationEndpoint.TRANSFORMATION",
		defaultValue = "navigation")
	String TRANSFORMATION;

	@Inject TransformationMap transformationMap;

	@Override
	public Uni<Response> navigation(URI collection, String resource, String ref,
									String start, String end, Integer down,
									String tree, Integer page) {
		Transformation transformation = transformationMap.get(TRANSFORMATION);
		return null;
	}
}
