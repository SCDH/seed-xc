package de.ulbms.scdh.seed.xc.jena;

import com.apicatalog.jsonld.http.DefaultHttpClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.glassfish.json.JsonProviderImpl;

@RegisterForReflection(
		targets = {
			SparqlConstruct.class,
			ConfiguredJsonLdLoader.class,
			ConfiguredJsonLdOptions.class,
			JsonLdContext.class,
			ParameterInjector.class,
			Serializer.class,
			StaticDocumentLoader.class,
			DefaultHttpClient.class,
			JsonProviderImpl.class
		})
public class ReflectionConfiguration {}
