package de.ulbms.scdh.seed.xc.dts.v1_0;

import static de.ulbms.scdh.seed.xc.api.utils.ParameterValueFactory.pvOf;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdEmbed;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.api.inject.TransformTimeProvider;
import de.ulbms.scdh.seed.xc.dts.CollectionMetadataProcessor;
import de.ulbms.scdh.seed.xc.dts.endpoints.StandoffApi;
import de.ulbms.scdh.seed.xc.transformations.TransformationMap;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.rewriter.BackwardMappingFactory;
import de.wwu.scdh.annotation.selection.rewriter.ForwardMappingFactory;
import de.wwu.scdh.annotation.selection.wadm.NormalizeAnnotation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.jsonld.JenaToTitanium;
import org.apache.jena.riot.system.jsonld.TitaniumJsonLdOptions;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
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
			name = "document-endpoint-transformation",
			defaultValue = "dts-transformations-xsl-document")
	protected String TRANSFORMATION;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.TYPE", defaultValue = "DtsDocumentProcessor")
	protected String TYPE;

	@ConfigProperty(name = "de.ulbms.scdh.seed.xc.dts.DocumentEndpoint.SETS_SERIALIZER", defaultValue = "true")
	protected boolean SETS_SERIALIZER;

	@ConfigProperty(name = "service-base-url", defaultValue = ".")
	protected String serviceBaseUrl;

	@ConfigProperty(name = "annotations-default-frame", defaultValue = "https://www.w3.org/ns/anno-frame.json")
	protected String defaultFrame;

	@Inject
	CollectionMetadataProcessor collectionMetadataProc;

	@Inject
	protected TransformationMap transformations;

	@TransformTimeProvider
	@Inject
	ResourceProviderManager resourceProviderManager;

	@Inject
	HttpServerRequest request;

	@Inject
	JsonLdOptions jsonLdOptions;

	@Inject
	de.ulbms.scdh.seed.xc.jena.Serializer serializer;

	private URI preimageIri, imageIri;

	private ResourceProvider resourceProvider;

	/**
	 * Transform Open Annotations pointing into the image back into annotations pointing into the preimage.
	 * The preimage is the default representation of the resource document, the image is determined by the DTS
	 * parameters that are similar to the document endpoint and chase a part and a media type (and profile).
	 * I.e., the image is derived from the preimage by transformation.
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

		return collectionMetadataProc
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
						request))
				.onItem()
				.transform(mappedResource -> NormalizeAnnotation.normalize(
						mappedResource,
						imageIri, // backward!
						getRewriterFactory("backward"),
						getRewriterConfig(),
						getAnnotationsGraph(annotations)))
				.onItem()
				.transform(model -> serialize(model, frame))
				.onItem()
				.transform(bytes -> new String(bytes, Charset.defaultCharset()));
	}

	/**
	 * Transform Open Annotations pointing into the preimage forward into annotations pointing into the image.
	 * The preimage is the default representation of the resource document, the image is determined by the DTS
	 * parameters that are similar to the document endpoint and chase a part and a media type (and profile).
	 * I.e., the image is derived from the preimage by transformation.
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
		LOG.debug("prepared to forward oa annotations from {} to {}", preimageIri, imageIri);

		return collectionMetadataProc
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
						request))
				.onItem()
				.transform(mappedResource -> NormalizeAnnotation.normalize(
						mappedResource,
						preimageIri, // forward!
						getRewriterFactory("forward"),
						getRewriterConfig(),
						getAnnotationsGraph(annotations)))
				.onItem()
				.transform(model -> serialize(model, frame))
				.onItem()
				.transform(bytes -> new String(bytes, Charset.defaultCharset()));
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
	 * Set the IRI of the resource.</P>
	 *
	 * The IRI of the resource (preimage) is the current request, but query part and fragment cut off. The IRI of the
	 * image has the query part.
	 */
	private void setIRI(URI provider, URI location, URI resource, String direction) {
		if (resource == null || resource.toString().isEmpty())
			throw new BadRequestException("resource parameter is required");
		try {
			String front =
					"/" + provider.toString() + "/" + URLEncoder.encode(location.toString(), StandardCharsets.UTF_8);
			String resourceEncoded = URLEncoder.encode(resource.toString(), StandardCharsets.UTF_8);
			URI rqUrl = new URI(request.absoluteURI());
			URI base;
			if (serviceBaseUrl != null && !serviceBaseUrl.isBlank() && !serviceBaseUrl.equals(".")) {
				base = new URI(serviceBaseUrl);
			} else {
				base = new URI(
						rqUrl.getScheme(), rqUrl.getRawUserInfo(), rqUrl.getHost(), rqUrl.getPort(), null, null, null);
			}
			// The preimage and image come out of the document endpoint!
			// use single-argument constructor to avoid extra escaping! see #61
			preimageIri = new URI(base.toString() + front + "/document/" + resourceEncoded);
			if (rqUrl.getQuery() != null) {
				imageIri = new URI(base.toString() + front + "/document/" + resourceEncoded + rqUrl.getQuery());
			} else {
				imageIri = new URI(base.toString() + front + "/document/" + resourceEncoded);
			}
		} catch (URISyntaxException e) {
			throw new InternalServerErrorException("failed to make Base URI");
		}
	}

	private Config getConfig() {
		Config transformationConfig = new Config();
		transformationConfig.base(preimageIri.toString());
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
				throw new InternalServerErrorException("default transformation not available: " + TRANSFORMATION);
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
			LOG.info(
					"transformation {} is not a DOM mapper due to its type class {}",
					transformation.getTransformationInfo().getIdent(),
					transformation.getClazz());
			// Because the server does recognize the method
			// 405 is the right thing here, with empty Allow header
			throw new NotAllowedException(
					"transformation is not a DOM mapper: "
							+ transformation.getTransformationInfo().getIdent(),
					new Exception());
		}
		MappingTransformation finalTransformation = (MappingTransformation) transformation;
		if (finalTransformation.canMapResource()) {
			return finalTransformation;
		} else {
			LOG.info(
					"transformation does not support DOM mapping: {}",
					transformation.getTransformationInfo().getIdent());
			// 405 is the right thing here, with empty Allow header
			throw new NotAllowedException(
					"transformation does not support DOM mapping: "
							+ transformation.getTransformationInfo().getIdent(),
					new Exception());
		}
	}

	private byte[] serialize(Model model, InputStream frame) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		org.apache.jena.riot.Lang lang = RDFLanguages.contentTypeToLang(request.getHeader(HttpHeaders.ACCEPT));
		RDFFormat format = de.ulbms.scdh.seed.xc.jena.Serializer.getFormatVariant(lang, request.getHeader(HttpHeaders.ACCEPT_CHARSET));
		if (!format.getLang().equals(Lang.JSONLD11)) {
			// format differs from JSON-LD or frame is missing
			RDFDataMgr.write(output, model, format);
		} else {
			try {
				// use titanium for framing
				JsonLdOptions options = new JsonLdOptions(jsonLdOptions); // clone
				// options.setBase(null);
				options.setOmitGraph(true);
				options.setEmbed(JsonLdEmbed.ALWAYS);
				// add more options here!
				DatasetGraph dsg = DatasetGraphFactory.create(model.getGraph());
				JsonArray ja = JenaToTitanium.convert(dsg, options);
				JsonDocument jDoc = JsonDocument.of(ja);
				// make the frame or use annot.json context as default
				FramingApi framingApi;
				if (frame != null) {
					Document frameDoc = JsonDocument.of(frame);
					framingApi = JsonLd.frame(jDoc, frameDoc);
				} else {
					framingApi = JsonLd.frame(jDoc, defaultFrame);
				}
				framingApi.loader(options.getDocumentLoader()); // important to set loader!
				JsonObject framed = framingApi.get();
				JsonWriter writer = Json.createWriter(output);
				writer.writeObject(framed);
			} catch (JsonLdError e) {
				if (frame != null) {
					throw new BadRequestException("failed JSON-LD framing of result graph");
				} else {
					throw new InternalServerErrorException("failed JSON-LD framing with the default frame");
				}
			}
		}
		return output.toByteArray();
	}

	private RewriterConfig getRewriterConfig() {
		return new RewriterConfig(Mode.DEEP_NODE_STEP_OVER_END, true, "path(.)"); // TODO
	}

	private RewriterFactory getRewriterFactory(String direction) {
		if (direction.equals("forward")) {
			return new ForwardMappingFactory();
		}
		else {
			return new BackwardMappingFactory();
		}
	}

	private Model getAnnotationsGraph(InputStream inputStream) {
		RDFParserBuilder parserBuilder = RDFParser.source(inputStream);
		// TODO: content negotiation: get lang from the request
		Lang lang = Lang.JSONLD11;
		LOG.debug("trying to parse RDF data as {}", lang);
		if (lang.equals(Lang.JSONLD11)) {
			parserBuilder.set(TitaniumJsonLdOptions.JSONLD_OPTIONS, jsonLdOptions);
		}
		return parserBuilder.lang(lang).toModel();
	}
}
