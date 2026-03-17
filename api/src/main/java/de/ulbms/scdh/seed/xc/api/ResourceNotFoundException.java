package de.ulbms.scdh.seed.xc.api;

/**
 * An exception that occurred because there's no resource at the
 * requested location.
 */
public class ResourceNotFoundException extends Exception {
	public ResourceNotFoundException(String msg) { super(msg); }
	public ResourceNotFoundException(Throwable cause) { super(cause); }
	public ResourceNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
