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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

	private URI preimageIri, imageIri;

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
		setIRI(provider, location, resource, "backward");
		setResourceProvider(provider, location);
		final Config config = getConfig();
		final RuntimeParameters parameters = mkParameters(resource, ref, start, end, tree, mediaType);
		final MappingTransformation transformation = getTransformation(mediaType, config);

		collectionMetadataProc
				.getResourceAsync(resourceProvider, config, Map.of(), preimageIri)
				.plug(s -> transformation.mapResourceAsync(
						// TODO: systemId from collectionMetadataProc
						parameters,
						config,
						preimageIri.toString(),
						preimageIri,
						imageIri,
						s,
						resourceProvider,
						request));
		// TODO

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

		setIRI(provider, location, resource, "forward");
		setResourceProvider(provider, location);
		final Config config = getConfig();
		final RuntimeParameters parameters = mkParameters(resource, ref, start, end, tree, mediaType);
		final MappingTransformation transformation = getTransformation(mediaType, config);

		collectionMetadataProc
				.getResourceAsync(resourceProvider, config, Map.of(), preimageIri)
				.plug(s -> transformation.mapResourceAsync(
						// TODO: systemId from collectionMetadataProc
						parameters,
						config,
						preimageIri.toString(),
						preimageIri,
						imageIri,
						s,
						resourceProvider,
						request));
		// TODO

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
	private void setIRI(URI provider, URI location, URI resource, String direction) {
		if (resource == null || resource.toString().isEmpty())
			throw new BadRequestException("resource parameter is required");
		try {
			String front =
					"/" + provider.toString() + "/" + URLEncoder.encode(location.toString(), StandardCharsets.UTF_8);
			String resourceEncoded = URLEncoder.encode(resource.toString(), StandardCharsets.UTF_8);
			URI rqUrl = new URI(request.absoluteURI());
			// the IRI of the resource is the current request, but query part and fragment cut off
			URI base = new URI(
					rqUrl.getScheme(), rqUrl.getRawUserInfo(), rqUrl.getHost(), rqUrl.getPort(), null, null, null);
			// use single-argument constructor to avoid extra escaping! see #61
			preimageIri = new URI(base.toString() + front + "/oa/" + direction + "/" + resourceEncoded);
			imageIri = new URI(base.toString() + front + "/oa/" + direction + "/" + resourceEncoded + rqUrl.getQuery());
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make Base URI");
		}
	}

	private Config getConfig() {
		Config transformationConfig = new Config();
		transformationConfig.base(request.absoluteURI());
		return transformationConfig;
	}

	private RuntimeParameters mkParameters(
			URI resource, String ref, String start, String end, String tree, String mediaType) {
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
		LOG.debug("parameters: {}", map);
		return params;
	}

	private MappingTransformation getTransformation(String mediaType, Config config) {
		Transformation transformation = null;
		if (mediaType == null) {
			// get the default transformation or return failure
			transformation = transformations.get(TRANSFORMATION);
			if (transformation == null) {
				LOG.error("mapper transformation not available: {}", TRANSFORMATION);
				throw new jakarta.ws.rs.BadRequestException("mapper transformation not available: " + TRANSFORMATION);
			}
		} else {
			// try to get a transformation for the requested media type
			LOG.info("searching for mapper transformation to media type {}", mediaType);
			boolean found = false;
			for (String transformationId : transformations.keySet()) {
				transformation = transformations.get(transformationId);
				LOG.info(
						"testing mapper transformation {}, with type {}: {}",
						transformationId,
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
						config.setSerializer(serializer);
					}
					break;
				}
			}
			if (!found) {
				LOG.error("DTS mapper transformation to media type not available: {}", mediaType);
				throw new jakarta.ws.rs.BadRequestException(
						"DTS mapper transformation to requested media type not available: " + mediaType);
			}
		}
		// assert that the transformation is a Mapping Transformation
		if (!MappingTransformation.class.isAssignableFrom(transformation.getClass())) {
			throw new InternalServerErrorException("transformation is not a DOM mapper");
		}
		MappingTransformation finalTransformation = (MappingTransformation) transformation;
		if (finalTransformation.canMapResource()) {
			return finalTransformation;
		} else {
			throw new BadRequestException("transformation is cannot be used to map a DOM: " + transformation);
		}
	}
}
