package de.ulbms.scdh.seed.xc.resources.url;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UrlConfig {

	@ConfigProperty(name = "resources-url-allowed-protocols", defaultValue = "file,http,https")
	protected String allowedProtocols;

	@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = ".*")
	protected String domainWhiteList;

	@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = "drive-by-download")
	protected String domainBlackList;

	@ConfigProperty(
			name = "de.ulbms.scdh.seed.xc.resources.filesystem.FileSystemResourceProvider.path",
			defaultValue = "/")
	protected String allowedFilePath;

	@ConfigProperty(name = "url-connect-timeout", defaultValue = "10000")
	int connectTimeout;

	@ConfigProperty(name = "url-read-timeout", defaultValue = "10000")
	int readTimeout;

	@ConfigProperty(name = "resouce-max-size", defaultValue = "1048576")
	long resourceMaxSize;
}
