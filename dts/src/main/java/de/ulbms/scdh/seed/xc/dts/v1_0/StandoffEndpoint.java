package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.CollectionMetadataProcessor;
import de.ulbms.scdh.seed.xc.dts.endpoints.StandoffApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class StandoffEndpoint implements StandoffApi {

	private static final Logger LOG = LoggerFactory.getLogger(StandoffEndpoint.class);

	public static final String MEDIATYPE_ANNOTATIONS =
			"application/ld+json;text/turtle;application/rdf+xml;application/n-triples;text/trig;application/n-quads;application/trix+xml;application/rdf+thrift;application/rdf+protobuf";

	@ConfigProperty(name = "dts-standoff-transformation-suffix", defaultValue = ".standoff")
	protected String TRANSFORMATION_SUFFIX;

	/**
	 * The ID of the transformation using for transforming a resource.
	 */
	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TRANSFORMATION",
			defaultValue = "dts-transformations-xsl-document")
	protected String TRANSFORMATION;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TYPE", defaultValue = "DtsDocumentProcessor")
	protected String TYPE;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.SETS_SERIALIZER", defaultValue = "true")
	protected boolean SETS_SERIALIZER;

	@Inject
	CollectionMetadataProcessor collectionMetadataProc;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProviderManager resourceProviderManager;

	@Inject
	HttpServerRequest request;

	private URI thisIri;

	private ResourceProvider resourceProvider;

	/**
	 *
	 *
	 *
	 * @param resource - Resource identifier. Passed as runtime parameter to the transformation and also to the resource provider.
	 * @param provider - the type of resource provider
	 * @param location - the base location accessed by the resource provider
	 * @param annotations - an RDF graph with annotations.
	 * @param ref - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param start - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param end - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param tree - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param mediaType - See DTS specs. Passed as runtime parameter to the transformation.
	 * @param frame - A JSON-LD frame for framing the output.
	 * @return The document or parts of it in the requested media type.
	 */
	@Override
	public Uni<String> standoffBackward(
			URI resource,
			URI provider,
			URI location,
			@RestForm @PartType(MEDIATYPE_ANNOTATIONS) InputStream annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			InputStream frame) {
		setIRI();
		setResourceProvider(provider, location);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<String> standoffForward(
			URI resource,
			URI provider,
			URI location,
			@RestForm @PartType(MEDIATYPE_ANNOTATIONS) InputStream annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			InputStream frame) {
		setIRI();
		setResourceProvider(provider, location);
		return null;
	}

	private void setResourceProvider(URI provider, URI location) {
		try {
			ResourceProviderBuilder resourceProviderBuilder = resourceProviderManager.get(provider.toString());
			resourceProvider = resourceProviderBuilder.withBase(location);
		} catch (ResourceProviderConfigurationException e) {
			LOG.error("cannot find resource provider builder type {}", provider);
			throw new BadRequestException("unknown resource provider: " + provider);
		} catch (ResourceNotFoundException e) {
			LOG.error("not found: {}", location);
			throw new NotFoundException("not found");
		} catch (ResourceException e) {
			LOG.error("cannot open base location {} with {} resource provider: {}", location, provider, e.getMessage());
			throw new BadRequestException("cannot open base location: " + e.getMessage());
		}
	}

	/**
	 * Set the IRI of the resource. According to the URI templates,
	 * this is essentially the request URL, but query and fragment parts dropped.
	 */
	private void setIRI() {
		try {
			URI rqUrl = new URI(request.absoluteURI());
			// the IRI of the resource is the current request, but query part and fragment cut off
			thisIri = new URI(
					rqUrl.getScheme(),
					rqUrl.getRawUserInfo(),
					rqUrl.getHost(),
					rqUrl.getPort(),
					rqUrl.getPath(),
					null,
					null);
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make Base URI");
		}
	}

	/**
	 * Similar to Document endpoint: This first gets the resource using the resource provider and then
	 * transforms it. But a Resource mapping is set.
	 */
	private Uni<Void> setMapping(
			URI resource,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType) {

		if (resource == null || resource.toString().isEmpty())
			throw new BadRequestException("resource parameter is required");

		Config transformationConfig = new Config();
		transformationConfig.base(request.absoluteURI());

		// make RuntimeParameter object from parameters
		RuntimeParameters params = new RuntimeParameters();
		Map<String, ParameterValue> map = new HashMap<>();
		if (mediaType != null) map.put("mediaType", pvOf(mediaType));
		map.put("resource", pvOf(resource));
		if (ref != null) map.put("ref", pvOf(ref));
		if (start != null) map.put("start", pvOf(start));
		if (end != null) map.put("end", pvOf(end));
		if (tree != null) map.put("tree", pvOf(tree));
		params.globalParameters(map);
		LOG.info("parameters: {}", map);

		LOG.debug("getting metadata for {}", thisIri);

		Transformation transformation = null;
		if (mediaType == null) {
			// get the default transformation or return failure
			transformation = transformations.get(TRANSFORMATION + TRANSFORMATION_SUFFIX);
			if (transformation == null) {
				LOG.error("mapper transformation not available: {}", TRANSFORMATION + TRANSFORMATION_SUFFIX);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"mapper transformation not available: " + TRANSFORMATION + TRANSFORMATION_SUFFIX));
			}
		} else {
			// try to get a transformation for the requested media type
			LOG.info("searching for mapper transformation to media type {}", mediaType);
			boolean found = false;
			for (String transformationId : transformations.keySet()) {
				transformation = transformations.get(transformationId + TRANSFORMATION_SUFFIX);
				LOG.info(
						"testing mapper transformation {}, with type {}: {}",
						transformationId + TRANSFORMATION_SUFFIX,
						transformation.getType(),
						transformation.getOutputMediaType());
				if (transformation.getOutputMediaType() != null
						&& transformation.getOutputMediaType().equals(mediaType)
						&& transformation.getType() != null
						&& Arrays.asList(transformation.getType()).contains(TYPE)) {
					found = true;
					if (SETS_SERIALIZER) {
						// we have to set the serializer because the called stylesheet is always document.xsl which has
						// output method XML.
						Serializer serializer = new Serializer();
						serializer.setMethod(mediaType);
						transformationConfig.setSerializer(serializer);
					}
					break;
				}
			}
			if (!found) {
				LOG.error("DTS mapper transformation to media type not available: {}", mediaType);
				return Uni.createFrom()
						.failure(new jakarta.ws.rs.BadRequestException(
								"DTS mapper transformation to requested media type not available: " + mediaType));
			}
		}
		if (!MapperTransformation.isAssignableBy(transformation)) {
			throw InternalServerErrorException("transformation is not a DOM mapper");
		}
		final MapperTransformation finalTransformation =
				transformation; // final required for the lambda expression below
		final Config finalConfig = transformationConfig;


		// async processing of
		// 1. get collection.json, 2. lookup the resource's location, 3. get the resource, 4. transform it
		Map<String, String> crContext = Map.of();
		return collectionMetadataProc
				.getResourceAsync(resourceProvider, transformationConfig, crContext, thisIri)
				.plug((s) -> finalTransformation.transformAsync(
						// TODO: systemId from collectionMetadataProc
						params, finalConfig, thisIri.toString(), s, resourceProvider, request));
	}
}
