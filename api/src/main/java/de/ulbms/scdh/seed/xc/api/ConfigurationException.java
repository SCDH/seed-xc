package de.ulbms.scdh.seed.xc.api;

/**
 * An exception that occurred in one of the following phases:
 *
 * 1) during the configuration of the service, e.g. when reading the
 * configuration file.
 *
 * 2) during the compilation of any of the transformations.
 *
 * Since service configuration AND compilation of all transformations
 * take place before processing the first request, we use the same
 * exception class for these kinds of errors.
 */
public class ConfigurationException extends Exception {
    public ConfigurationException(String msg) {
        super(msg);
    }
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
    public ConfigurationException(String msg, Throwable cause) {
    super(msg, cause);
    }
}
