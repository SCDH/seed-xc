package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class UrlValidator {

	private static final Logger LOG = LoggerFactory.getLogger(UrlValidator.class);

	protected Set<String> allowedProtocolSet;

	protected List<Pattern> domainWhiteListPatterns;

	protected List<Pattern> domainBlackListPatterns;

	@ConfigProperty(name = "resources-url-allowed-protocols", defaultValue = "http,https")
	protected String allowedProtocols;

	@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = ".*")
	protected String domainWhiteList;

	@ConfigProperty(name = "resources-url-domain-whitelist", defaultValue = "drive-by-download")
	protected String domainBlackList;

	private boolean isConfigured = false;

	protected void configure() {
		if (!isConfigured) {
			allowedProtocolSet = new HashSet<>(Arrays.asList(allowedProtocols.split(",")));

			List<Pattern> patterns = new ArrayList<>();
			for (String domain : domainWhiteList.split(",")) {
				patterns.add(Pattern.compile(domain));
			}
			domainWhiteListPatterns = List.copyOf(patterns); // unmodifiable

			patterns = new ArrayList<>();
			for (String domain : domainBlackList.split(",")) {
				patterns.add(Pattern.compile(domain));
			}
			domainBlackListPatterns = List.copyOf(patterns); // unmodifiable

			isConfigured = true;
		}
	}

	public void check(URI base) throws ResourceException {
		// enforce protocol constraints
		if (base.getScheme() == null || !allowedProtocolSet.contains(base.getScheme())) {
			LOG.error("rejecting protocol {} in {}", base.getScheme(), base);
			throw new ResourceException("bad protocol: " + String.valueOf(base.getScheme()));
		}
		// match against white and black list
		String domain = base.getHost();
		if (domain == null) {
		} else if (!(inPatterns(domainWhiteListPatterns, domain) || domainWhiteList.isEmpty())
				|| inPatterns(domainBlackListPatterns, domain)) {
			LOG.warn("domain not allowed: {}", domain);
			throw new ResourceException("domain not allowed: " + domain);
		}
	}

	protected static boolean inPatterns(List<Pattern> patterns, String s) {
		boolean matches = false;
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(s);
			if (matcher.matches()) {
				matches = true;
				break;
			}
		}
		return matches;
	}
}
