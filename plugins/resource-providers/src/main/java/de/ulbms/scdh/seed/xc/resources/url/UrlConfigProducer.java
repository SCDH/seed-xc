package de.ulbms.scdh.seed.xc.resources.url;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UrlConfigProducer {

	private static final Logger LOG = LoggerFactory.getLogger(UrlConfigProducer.class);

	@Produces
	public static UrlConfig get(
			@ConfigProperty(name = "resources-url-allowed-protocols", defaultValue = "file,http,https")
					String allowedProtocols,
			@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = ".*") String domainWhiteList,
			@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = "drive-by-download")
					String domainBlackList,
			@ConfigProperty(
							name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
							defaultValue = "/")
					String allowedFilePath,
			@ConfigProperty(name = "url-connect-timeout", defaultValue = "10000") int connectTimeout,
			@ConfigProperty(name = "url-read-timeout", defaultValue = "10000") int readTimeout,
			@ConfigProperty(name = "resouce-max-size", defaultValue = "1048576") long resourceMaxSize) {
		return new UrlConfig(
				allowedProtocols,
				domainWhiteList,
				domainBlackList,
				allowedFilePath,
				connectTimeout,
				readTimeout,
				resourceMaxSize);
	}
}
