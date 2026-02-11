package de.ulbms.scdh.seed.xc.api;

/**
 * An exception that occurred during a performing a transformation.
 */
public class TransformationException extends Exception {
    public TransformationException(String msg) {
        super(msg);
    }
    public TransformationException(Throwable cause) {
        super(cause);
    }
    public TransformationException(String msg, Throwable cause) {
    super(msg, cause);
    }
}
