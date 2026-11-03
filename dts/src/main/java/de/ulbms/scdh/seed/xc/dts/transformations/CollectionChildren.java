package de.ulbms.scdh.seed.xc.dts.transformations;

import de.ulbms.scdh.seed.xc.api.*;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import java.io.File;
import java.io.InputStream;

/**
 * A special {@link Transformation} for the DTS collection endpoint.
 */
public class CollectionChildren implements Transformation {

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
		return null;
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
		return new byte[0];
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
		return null;
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
		return new byte[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputMediaType() {
		return "application/ld+json";
	}
}
