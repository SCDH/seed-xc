package de.ulbms.scdh.seed.xc.xslt;

import java.io.File;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;
import javax.xml.transform.sax.SAXSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.xml.sax.InputSource;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;
import de.ulbms.scdh.seed.xc.harden.DenyingOutputURIResolver;

/**
 * This class is a managed bean and produces a Saxon {@link Processor}
 * from a configuration file provided in one of the lookup locations
 * declared the app config using
 * <code>de.ulbms.scdh.seed.xc.xslt.ConfiguredProcessor.saxonConfigLocations</code>.
 */
@ApplicationScoped
public class ConfiguredProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguredProcessor.class);

    /**
     * This static method produces a {@link Processor} when the
     * container needs it for injection. It looks up all locations for
     * Saxon configuration files declared in
     * <code>de.ulbms.scdh.seed.xc.xslt.ConfiguredProcessor.saxonConfigLocations</code>.
     */
    @Produces
    public static Processor createConfiguredProcessor
        (@ConfigProperty(name = "de.ulbms.scdh.seed.xc.xslt.ConfiguredProcessor.saxonConfigLocations",
                         defaultValue = "")
         String saxonConfigLocations)
        throws ConfigurationException {
        Processor processor = null;
        // get saxon configuration file and set up a processor based on it
        File saxonConfigFile;
        for (String saxonPath : saxonConfigLocations.split(",")) {
            saxonPath = saxonPath.trim();
            // config files may be placed in ~/.seed/..., so handle the tilde
            saxonPath = saxonPath.replaceFirst("^~", System.getProperty("user.home"));
            saxonConfigFile = new File(saxonPath);
            if (saxonConfigFile.exists() && !saxonConfigFile.isDirectory()) {
                // make a source object
                LOG.info("Reading Saxon configuration from '{}'", saxonPath);
                InputSource configSource = new InputSource(saxonPath);
                SAXSource saxSource = new SAXSource(configSource);
                try {
                    processor = new Processor(saxSource);
                } catch (SaxonApiException e) {
                    LOG.error("Configuring Saxon with '{}' failed:\n{}", saxonPath, e);
                    throw new ConfigurationException(e);
                }
            }
        }
        if (processor == null) {
            LOG.info("No saxon configuration file provided at '{}'. Using default configuration.", saxonConfigLocations);
            processor = new Processor(false);
        }
        // log import configuration features
        LOG.info("allowed protocols: {}", processor.getConfigurationProperty(Feature.ALLOWED_PROTOCOLS));
        LOG.info("configured URI resolver: {}", processor.getConfigurationProperty(Feature.URI_RESOLVER_CLASS));
        LOG.info("configured unparsed text URI resolver: {}", processor.getConfigurationProperty(Feature.UNPARSED_TEXT_URI_RESOLVER_CLASS));
        LOG.info("configured static URI resolver: {}", processor.getConfigurationProperty(Feature.XSLT_STATIC_URI_RESOLVER_CLASS));
        LOG.info("configured output URI resolver: {}", processor.getConfigurationProperty(Feature.OUTPUT_URI_RESOLVER_CLASS));
        return processor;
    }

}
