package de.ulbms.scdh.seed.xc.resources.url;

public class UrlConfig {

	private final String allowedProtocols;

	private final String domainWhiteList;

	private final String domainBlackList;

	private final String allowedFilePath;

	private final int connectTimeout;

	private final int readTimeout;

	private final long resourceMaxSize;

	public UrlConfig(
			String allowedProtocols,
			String domainWhiteList,
			String domainBlackList,
			String allowedFilePath,
			int connectTimeout,
			int readTimeout,
			long resourceMaxSize) {
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
