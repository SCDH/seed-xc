package de.ulbms.scdh.seed.xc.jena;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
		targets = {
			SparqlConstruct.class,
			ConfiguredJsonLdLoader.class,
			ConfiguredJsonLdOptions.class,
			JsonLdContext.class,
			ParameterInjector.class,
			Serializer.class,
			StaticDocumentLoader.class
		})
public class ReflectionConfiguration {}
