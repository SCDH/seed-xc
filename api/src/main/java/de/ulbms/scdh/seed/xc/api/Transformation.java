package de.ulbms.scdh.seed.xc.api;

import java.io.InputStream;


/**
 * A interface for transformation. A transformation is a process that
 * can be setup (compiled) with a {@link TransformationInfo}, i.e. a
 * configuration object. And it then can be used to transform all
 * subsequent transformation requests without repeating the setup.
 *
 * A transformation class should be annotated with the pseudo-scope
 * {@link jakarta.enterprise.context.Dependent}. It will be a managed
 * bean and can use {@link jakarta.inject.Inject} fields for optaining
 * arbitrary service-wide configurations.
 */
public interface Transformation {

    /**
     * Setup the transformation from a {@link TransformationInfo}. If
     * the transformation can be compiled and the compliled executable
     * can used then used for all subsequent transformations, then the
     * compilation should happen here.
     *
     * @param transformationInfo  a {@link TransformationInfo} object used to configure the transformation
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
     * Returns a compiler-generated description of the transformation parameters.
     * @return {@link XsltParameterDetails}
     */
    XsltParameterDetails getTransformationParameters();

    /**
     * Returns a representation of the compiled stylesheet, in XML
     * form, suitable for distribution and reloading.
     */
    byte[] export() throws UnsupportedOperationException;

    /**
     * Returns a representation of the compiled stylesheet for a
     * particular target environment, in XML form, suitable for
     * distribution and reloading.
     * @param target the target environment (for example "EE" or "JS")
     * @see net.sf.saxon.s9api.XsltExecutable@export
     */
    byte[] export(String target) throws UnsupportedOperationException;

    /*
     * Transform the document given as {@param source}. Its String can be
     * set in order to get XML base property right. <code>null</code>
     * can be supplied as {@param systemId} but this means, that the
     * XML base property is not set for the document node.
     *
     *
     * @param parameters {@link RuntimeParameters} to apply on the transformation
     * @param config     per-request {@link Config} for the transformation
     * @param systemId   {@link String} pointing a the source documents location and set as XML base property
     * @param source     {@link InputStream} with the XML document to be transformed
     *
     * @return This returns the transformed XML as a byte array.
     */
    byte[] transform(RuntimeParameters parameters, Config config, String systemId, InputStream source)
	throws TransformationPreparationException, TransformationException;

    /**
     * Transform the document given as {@param systemId}, which is
     * an {@link String}. The document is fetched by the transformer.
     *
     *
     * @param parameters {@link RuntimeParameters} to apply on the transformation
     * @param config     per-request {@link Config} for the transformation
     * @param systemId   {@link String} pointing a the source documents location and set as XML base property
     *
     * @return This returns the transformed XML as a byte array.
     */
    byte[] transform(RuntimeParameters parameters, Config config, String systemId)
	throws TransformationPreparationException, TransformationException;

    /**
     * Run a transform with {@link RuntimeParameters} only. This will
     * need a start template or start function be set in the
     * parameters object.
     *
     *
     * @param parameters {@link RuntimeParameters} to apply on the transformation
     * @param config     per-request {@link Config} for the transformation
     *
     * @return This returns the transformed XML as a byte array.
     */
    byte[] transform(RuntimeParameters parameters, Config config)
	throws TransformationPreparationException, TransformationException;

    /**
     * Returns the media type. This is used in the response header. So
     * it should be a real media type. It may return <code>null</code>
     * if no information is available. In this case the service will
     * use a default media type.
     *
     * @returns the media type of the transformation output as {@link java.lang.String}
     */
    String getOutputMediaType();

}
