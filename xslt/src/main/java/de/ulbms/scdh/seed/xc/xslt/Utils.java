package de.ulbms.scdh.seed.xc.xslt;


import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ulbms.scdh.seed.xc.api.ConfigurationException;


/**
 * Utilities used in this package.
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Reads the file given as path from the provided ZIP file.
     */
    public static InputStream fromZip(ZipFile zipFile, String path) throws ConfigurationException {
        try {
            ZipEntry zipEntry = zipFile.getEntry(path);
            return zipFile.getInputStream(zipEntry);
        } catch (NullPointerException e) {
            LOG.error("file {} not found in zip package", path);
            throw new ConfigurationException("file not found in zip package: " + path);
        } catch (Exception e) {
            LOG.error("cannot extract file {} from zip package: {}", path, e.getMessage());
            throw new ConfigurationException(e);
        }
    }

}
