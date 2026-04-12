package de.ulbms.scdh.seed.xc.api;

public interface TransformationExceptionParser {

	/**
	 * Returns an HTTP status code from an exception.
	 */
	int parseCode(TransformationException err);

	/**
	 * Returns an error message from an exception.
	 */
	String message(TransformationException err);
}
