package de.ulbms.scdh.seed.xc.api;

public interface ExportingCompiler {

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
}
