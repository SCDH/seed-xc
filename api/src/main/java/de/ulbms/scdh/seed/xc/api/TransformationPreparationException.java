package de.ulbms.scdh.seed.xc.api;

/**
 * An exception that occurred during the preparation of a
 * transformation, i.e. the setup of the global runtime context of the
 * transformation. This kind of exception does not occur in the
 * compilation phase, but in the runtime phase, for example when
 * building up the map of runtime parameters.
 */
public class TransformationPreparationException extends Exception {
    public TransformationPreparationException(String msg) {
        super(msg);
    }
    public TransformationPreparationException(Throwable cause) {
        super(cause);
    }
    public TransformationPreparationException(String msg, Throwable cause) {
	super(msg, cause);
    }
}
