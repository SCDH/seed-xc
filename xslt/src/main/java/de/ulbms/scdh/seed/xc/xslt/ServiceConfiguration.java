package de.ulbms.scdh.seed.xc.xslt;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A simple record of service-wide configuration options to be passed
 * to transformations. They are taken from the properties file.
 */
@ApplicationScoped
public class ServiceConfiguration {

    @ConfigProperty(name = "de.ulbms.scdh.seed.xc.xslt.ServiceConfiguration.nonProtocolURIsAllowed",
                    defaultValue = "false")
    protected boolean nonProtocolURIsAllowed;

    public boolean getNonProtocolURIsAllowed() {
        return nonProtocolURIsAllowed;
    }

}
