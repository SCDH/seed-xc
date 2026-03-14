package de.ulbms.scdh.seed.xc.xslt;

/**
 * A {@link ServiceConfiguration} for testing classes that are not
 * managed beans.
 */
public class TestingConfiguration extends ServiceConfiguration {

	protected boolean nonProtocolURIsAllowed;

	public TestingConfiguration() { this.nonProtocolURIsAllowed = true; }
}
