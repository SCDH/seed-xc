package de.ulbms.scdh.seed.xc.dts.transformations;

import static de.ulbms.scdh.seed.xc.dts.transformations.RdfUtils.copyComplexProperty;
import static de.ulbms.scdh.seed.xc.dts.transformations.RdfUtils.copySimpleProperty;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import de.ulbms.scdh.seed.xc.api.*;
import de.ulbms.scdh.seed.xc.dts.DTS;
import de.ulbms.scdh.seed.xc.dts.URITemplateBuilder;
import de.ulbms.scdh.seed.xc.jena.JsonLdContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIxResolver;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.jsonld.JenaToTitanium;
import org.apache.jena.riot.system.jsonld.TitaniumJsonLdOptions;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A special {@link Transformation} for the DTS collection endpoint, available as <code>dts-collection-rdf</code>.
 * This transformation processes the <code>collection.json</code> record metadata as RDF, as opposed to simple JSON
 * processing.<P/>
 *
 * This class processes the <code>nav</code> parameter and is thus capable of generating both,
 * children members and parent members.
 */
@ApplicationScoped
public class CollectionRdf implements Transformation {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionRdf.class);

	/**
	 * How this is available through transformation class lookup.
	 */
	public static final String CLAZZ = "dts-collection-rdf";

	public static final XsltParameterDetails COLLECTION_PARAMETERS;

	static {
		XsltParameterDetails params = new XsltParameterDetails();

		// requested is the full IRI of the collection/resource
		XsltParameterDetailsValue requestedValue = new XsltParameterDetailsValue();
		requestedValue.setIsRequired(true);
		requestedValue.setItemType("xs:anyURI");
		params.put("requested", requestedValue);

		// idP has the identifier of the collection/resource only
		XsltParameterDetailsValue idPValue = new XsltParameterDetailsValue();
		idPValue.setIsRequired(true);
		idPValue.setItemType("xs:string");
		params.put("idP", idPValue);

		// base is the base URI of the collection/resource, which contains the base URL of the service
		// Please note, that the base URI is also expected to be passed over by the config!
		XsltParameterDetailsValue baseValue = new XsltParameterDetailsValue();
		baseValue.setIsRequired(true);
		baseValue.setItemType("xs:anyURI");
		params.put("base", baseValue);

		// DTS' nav parameter
		XsltParameterDetailsValue navPValue = new XsltParameterDetailsValue();
		navPValue.setIsRequired(false);
		navPValue.setItemType("xs:string");
		params.put("navP", navPValue);

		// DTS' page parameter
		XsltParameterDetailsValue pagePValue = new XsltParameterDetailsValue();
		pagePValue.setIsRequired(false);
		pagePValue.setItemType("xs:integer");
		params.put("pageP", pagePValue);

		// the media types (content types) which a resource is available in, populated by the service
		XsltParameterDetailsValue mediaTypesPValue = new XsltParameterDetailsValue();
		mediaTypesPValue.setIsRequired(false);
		mediaTypesPValue.setItemType("xs:string");
		mediaTypesPValue.setOccurrenceIndicator("*");
		mediaTypesPValue.setUnderlyingDeclaredType("xs:string*");
		params.put("mediaTypesP", mediaTypesPValue);

		COLLECTION_PARAMETERS = params;
	}

	public static final String FRAME_RESOURCE = "/collection.json";

	private TransformationInfo transformationInfo;

	@Inject
	JsonLdContext jsonLdContextFactory;

	@Inject
	JsonLdOptions jsonLdOptions;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClazz() {
		return CLAZZ;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getType() {
		return new String[] {"DTS", "DTSCollection"};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup(TransformationInfo transformationInfo, File path) {
		this.transformationInfo = transformationInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransformationInfo getTransformationInfo() {
		return transformationInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XsltParameterDetails getTransformationParameters() {
		return COLLECTION_PARAMETERS;
	}

	protected void addDtsVersion(Resource target) {
		target.addLiteral(DTS.dtsVersion, DTS.VERSION);
	}

	protected void addMediaTypes(Resource target, ParameterValue mediaTypes) {
		if (target.hasProperty(RDF.type, DTS.Resource)) {
			for (String mediaType : mediaTypes) {
				target.addLiteral(DTS.mediaTypes, mediaType);
			}
		}
	}

	protected void addParents(
			Model source,
			Model target,
			Resource sourceResource,
			Resource targetResource,
			boolean addMembers,
			ParameterValue mediaTypes) {
		// totalParents
		Model parents = source.listStatements(null, DTS.member, sourceResource).toModel();
		targetResource.addProperty(
				DTS.totalParents, target.createTypedLiteral(new BigInteger(String.valueOf(parents.size()))));
		// members
		if (addMembers) {
			for (Statement s : parents.listStatements().toList()) {
				Resource member = s.getSubject();
				targetResource.addProperty(DTS.member, member);
				Resource targetMember = target.createResource(member.getURI());
				StmtIterator sourceStmtOnMember =
						source.listStatements(source.getResource(member.getURI()), RDF.type, (RDFNode) null);
				Resource sourceMember = sourceStmtOnMember.next().getSubject();
				copySimpleProperty(source, target, member, RDF.type);
				copySimpleProperty(source, target, member, DTS.title);
				copySimpleProperty(source, target, member, DTS.description);
				copySimpleProperty(source, target, member, DTS.download);
				copyComplexProperty(source, target, sourceMember, DTS.dublinCore);
				copyComplexProperty(source, target, sourceMember, DTS.extensions);
				copyComplexProperty(source, target, sourceMember, DTS.citationTrees);
				URITemplateBuilder.addCollectionTemplates(sourceMember, targetMember, sourceMember.getURI());
				addParents(source, target, sourceMember, targetMember, false, mediaTypes);
				addChildren(source, target, sourceMember, targetMember, false, mediaTypes);
				addDtsVersion(member);
				addMediaTypes(targetMember, mediaTypes);
			}
		}
	}

	protected void addChildren(
			Model source,
			Model target,
			Resource sourceResource,
			Resource targetResource,
			boolean addMembers,
			ParameterValue mediaTypes) {
		// totalChildren
		Model children = source.listStatements(sourceResource, DTS.member, (Resource) null)
				.toModel();
		if (sourceResource.hasProperty(RDF.type, DTS.Collection) || !children.isEmpty()) {
			targetResource.addProperty(
					DTS.totalChildren, target.createTypedLiteral(new BigInteger(String.valueOf(children.size()))));
		}
		// members
		if (addMembers) {
			target.add(children);
			for (Statement statement : children.listStatements().toList()) {
				Resource member = statement.getObject().asResource();
				Resource targetMember = target.createResource(member.getURI());
				StmtIterator sourceStmtOnMember =
						source.listStatements(source.getResource(member.getURI()), RDF.type, (RDFNode) null);
				Resource sourceMember = sourceStmtOnMember.next().getSubject();
				LOG.debug("member {}", sourceMember.toString());
				copySimpleProperty(source, target, member, RDF.type);
				copySimpleProperty(source, target, member, DTS.title);
				copySimpleProperty(source, target, member, DTS.description);
				copySimpleProperty(source, target, member, DTS.download);
				copyComplexProperty(source, target, sourceMember, DTS.dublinCore);
				copyComplexProperty(source, target, sourceMember, DTS.extensions);
				copyComplexProperty(source, target, sourceMember, DTS.citationTrees);
				URITemplateBuilder.addCollectionTemplates(sourceMember, targetMember, sourceMember.getURI());
				addParents(source, target, sourceMember, targetMember, false, mediaTypes);
				addChildren(source, target, sourceMember, targetMember, false, mediaTypes);
				addDtsVersion(member);
				addMediaTypes(targetMember, mediaTypes);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transform(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream source,
			ResourceProvider resourceProvider,
			HttpServerRequest request)
			throws TransformationPreparationException, TransformationException {
		try {
			// get the IRI of the requested collection or resource
			if (parameters.getGlobalParameters() == null
					|| !parameters.getGlobalParameters().containsKey("requested")) {
				LOG.error("missing parameter 'required'");
				throw new BadRequestException("parameter requested is required");
			}
			String iri = parameters.getGlobalParameters().get("requested").getFirst();

			String nav;
			if (!parameters.getGlobalParameters().containsKey("navP")) {
				nav = "children";
			} else {
				nav = parameters.getGlobalParameters().get("navP").getFirst();
			}

			ParameterValue mediaTypes;
			if (!parameters.getGlobalParameters().containsKey("mediaTypesP")) {
				mediaTypes = new ParameterValue();
			} else {
				mediaTypes = parameters.getGlobalParameters().get("mediaTypesP");
			}

			// make graph from JSON-LD or other RDF serializations
			RDFParserBuilder parserBuilder = RDFParser.source(source);
			Lang lang = RDFLanguages.filenameToLang(systemId, Lang.JSONLD11);
			LOG.debug("trying to parse RDF data from {} as format {}", systemId, lang);
			if (lang.equals(Lang.JSONLD11)) {
				parserBuilder.set(TitaniumJsonLdOptions.JSONLD_OPTIONS, jsonLdOptions);
			}
			// configure IRI resolver for not resolving relative IRIs (issue #22)
			IRIxResolver.Builder iriResolverBuilder =
					IRIxResolver.create(IRIs.stdResolver().clone());
			iriResolverBuilder.allowRelative(true);
			if (config != null && config.getBase() != null) {
				iriResolverBuilder.base(config.getBase());
			}
			parserBuilder.resolver(iriResolverBuilder.build());
			// according to docs, the parser will close the stream
			Model graph = parserBuilder.lang(lang).toModel();
			try {
				source.close();
				LOG.debug("closed collection metadata stream");
			} catch (IOException e) {
				LOG.debug("collection metadata stream was already closed");
			}
			LOG.debug("done parsing model");

			// check if IRI is in graph
			Resource requested = graph.getResource(iri);
			if (!graph.containsResource(requested)) {
				throw new NotFoundException("not found: " + iri);
			}
			// check if IRI is a Collection or Resource
			if (!(requested.hasProperty(RDF.type, DTS.Resource) || requested.hasProperty(RDF.type, DTS.Collection))) {
				throw new BadRequestException(iri + " is nor a dts:Collection nor a dts:Resource");
			}

			// build new graph with resulting set of statements
			Model result = ModelFactory.createDefaultModel();
			Resource resResource = result.createResource(iri);
			// dts:requested - important for framing
			resResource.addLiteral(DTS.requested, true);
			copySimpleProperty(graph, result, requested, RDF.type);
			copySimpleProperty(graph, result, requested, DTS.title);
			copySimpleProperty(graph, result, requested, DTS.description);
			copySimpleProperty(graph, result, requested, DTS.download);
			copyComplexProperty(graph, result, requested, DTS.dublinCore);
			copyComplexProperty(graph, result, requested, DTS.extensions);
			copyComplexProperty(graph, result, requested, DTS.citationTrees);
			URITemplateBuilder.addCollectionTemplates(requested, resResource, iri);
			addParents(graph, result, requested, resResource, nav.equals("parents"), mediaTypes);
			addChildren(graph, result, requested, resResource, nav.equals("children"), mediaTypes);
			addDtsVersion(resResource);
			addMediaTypes(resResource, mediaTypes);

			LOG.debug("transformation done");

			// write result back to the wire
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			// use titanium for framing
			JsonLdOptions opts = new JsonLdOptions();
			DatasetGraph dsg = DatasetGraphFactory.create(result.getGraph());
			JsonArray ja = JenaToTitanium.convert(dsg, opts);
			JsonDocument jdoc = JsonDocument.of(ja);
			Document frameDoc;
			// per-record context overrides per-transformation and default context
			if (config != null && config.getContext() != null) {
				frameDoc = jsonLdContextFactory.getContext(config.getContext());
			} else if (transformationInfo != null && transformationInfo.getContext() != null) {
				frameDoc = jsonLdContextFactory.getContext(transformationInfo.getContext());
			} else {
				LOG.debug("using default JSON-LD frame {}", getClass().getResource(FRAME_RESOURCE));
				InputStream frame = getClass().getResourceAsStream(FRAME_RESOURCE);
				assert frame != null;
				frameDoc = JsonDocument.of(frame);
			}
			JsonLdOptions options = new JsonLdOptions(jsonLdOptions);
			// options.setBase(null);
			options.setOmitGraph(true);
			// add more options here!
			FramingApi framingApi = JsonLd.frame(jdoc, frameDoc);
			framingApi.loader(options.getDocumentLoader()); // important to set loader!
			framingApi.base("");
			JsonObject framed = framingApi.get();
			JsonWriter writer = Json.createWriter(output);
			writer.writeObject(framed);
			return output.toByteArray();
		} catch (RiotException e) {
			LOG.error("failed to read RDF dataset {}", e.getMessage());
			throw new TransformationException(e);
		} catch (JsonLdError e) {
			LOG.error("JSON-LD processing failed, {}", e.getMessage());
			throw new TransformationException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<byte[]> transformAsync(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			Uni<? extends InputStream> source,
			ResourceProvider resourceProvider,
			HttpServerRequest request) {
		return source.onItem().transform((sourceStream) -> {
			try {
				return transform(parameters, config, systemId, sourceStream, resourceProvider, request);
			} catch (TransformationPreparationException | TransformationException e) {
				if (e.getMessage().equals("404")) {
					throw new NotFoundException("not found");
				}
				throw new InternalServerErrorException(e.getMessage());
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transformRT(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream source,
			ResourceProvider resourceProvider,
			HttpServerRequest request) {
		try {
			return transform(parameters, config, systemId, source, resourceProvider, request);
		} catch (TransformationPreparationException | TransformationException e) {
			if (e.getMessage().equals("404")) {
				throw new NotFoundException("not found");
			}
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputMediaType() {
		return "application/ld+json";
	}
}
