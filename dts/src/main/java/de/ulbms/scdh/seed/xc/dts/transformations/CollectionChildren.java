package de.ulbms.scdh.seed.xc.dts.transformations;

import de.ulbms.scdh.seed.xc.api.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import java.io.File;
import java.io.InputStream;

/**
 * A special {@link Transformation} for the DTS collection endpoint.
 */
public class CollectionChildren implements Transformation {

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClazz() {
		return CollectionChildren.class.getCanonicalName();
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
	public void setup(TransformationInfo transformationInfo, File path) throws ConfigurationException {}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransformationInfo getTransformationInfo() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XsltParameterDetails getTransformationParameters() {
		return COLLECTION_PARAMETERS;
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
		throw new TransformationException("not yet implemented"); // TODO
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
