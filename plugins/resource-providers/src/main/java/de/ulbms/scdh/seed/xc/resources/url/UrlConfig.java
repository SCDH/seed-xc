package de.ulbms.scdh.seed.xc.resources.url;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UrlConfig {

	private final String allowedProtocols;

	private final String domainWhiteList;

	private final String domainBlackList;

	private final String allowedFilePath;

	private final int connectTimeout;

	private final int readTimeout;

	private final long resourceMaxSize;

	public UrlConfig(
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
		this.allowedProtocols = allowedProtocols;
		this.domainWhiteList = domainWhiteList;
		this.domainBlackList = domainBlackList;
		this.allowedFilePath = allowedFilePath;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.resourceMaxSize = resourceMaxSize;
	}

	public long getResourceMaxSize() {
		return resourceMaxSize;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public String getAllowedFilePath() {
		return allowedFilePath;
	}

	public String getDomainBlackList() {
		return domainBlackList;
	}

	public String getDomainWhiteList() {
		return domainWhiteList;
	}

	public String getAllowedProtocols() {
		return allowedProtocols;
	}
}
