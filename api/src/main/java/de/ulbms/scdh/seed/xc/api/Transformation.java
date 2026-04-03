package de.ulbms.scdh.seed.xc.api;

import java.io.InputStream;

/**
 * A {@link Transformation} is a processor that
 * can be setup (compiled) with a {@link TransformationInfo}, i.e. a
 * configuration object. And it then can be used to transform all
 * subsequent transformation requests without repeating the setup.
 *
 * A transformation class should be annotated with the pseudo-scope
 * {@link jakarta.enterprise.context.Dependent}. It will be a managed
 * bean and can use {@link jakarta.inject.Inject} fields for obtaining
 * arbitrary service-wide configurations.
 */
public interface Transformation {

	/**
	 * Set up (compile) the transformation from a {@link TransformationInfo}.
	 * If the transformation is set up successfully, it then can be used for
	 * all subsequent transformations, no need to set up again.
	 *
	 * @param transformationInfo  a {@link TransformationInfo} object used to
	 *     configure the transformation
	 *
	 * @throws {@link ConfigurationException}
	 */
	void setup(TransformationInfo transformationInfo) throws ConfigurationException;

	/**
	 * Returns a description of the transformation.
	 *
	 */
	TransformationInfo getTransformationInfo();

	/**
	 * Returns a compiler-generated description of the transformation
	 * parameters.
	 * @return {@link XsltParameterDetails}
	 */
	XsltParameterDetails getTransformationParameters();

	/*
	 * Transform the document given as {@param source}. Its String can be
	 * set in order to get XML base property right. <code>null</code>
	 * can be supplied as {@param systemId} but this means, that the
	 * XML base property is not set for the document node.
	 *
	 *
	 * @param parameters - {@link RuntimeParameters} to apply on the transformation
	 * @param config - per-request {@link Config} for the transformation
	 * @param systemId - {@link String} pointing to the source documents location and set as XML base property
	 * @param source - {@link InputStream} with the XML document to be transformed
	 * @param resourceProvider - {@link ResourceProvider} used for getting secondary documents
	 *
	 * @return This returns the transformed XML as a byte array.
	 */
	byte[] transform(
			RuntimeParameters parameters,
			Config config,
			String systemId,
			InputStream source,
			ResourceProvider resourceProvider)
			throws TransformationPreparationException, TransformationException;

	/**
	 * Returns the media type. This is used in the response header. So
	 * it should be a real media type. It may return <code>null</code>
	 * if no information is available. In this case the service will
	 * use a default media type.
	 *
	 * @returns the media type of the transformation output as {@link
	 *     java.lang.String}
	 */
	String getOutputMediaType();
}
