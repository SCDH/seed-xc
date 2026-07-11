package de.ulbms.scdh.seed.xc.resources.url;

import de.ulbms.scdh.seed.xc.api.ResourceException;
import de.ulbms.scdh.seed.xc.api.ResourceNotFoundException;
import de.ulbms.scdh.seed.xc.api.ResourceProviderConfigurationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class UrlValidator {

	private static final Logger LOG = LoggerFactory.getLogger(UrlValidator.class);

	private String domainWhiteList;

	private Set<String> allowedProtocolSet;

	private List<Pattern> domainWhiteListPatterns;

	private List<Pattern> domainBlackListPatterns;

	private URI allowedFileUri;

	private boolean isConfigured = false;

	protected void configure(UrlConfig config) throws ResourceProviderConfigurationException {
		if (!isConfigured) {
			allowedProtocolSet = new HashSet<>(Arrays.asList(config.allowedProtocols.split(",")));

			domainWhiteList = config.domainWhiteList;
			List<Pattern> patterns = new ArrayList<>();
			for (String domain : config.domainWhiteList.split(",")) {
				patterns.add(Pattern.compile(domain));
			}
			domainWhiteListPatterns = List.copyOf(patterns); // unmodifiable

			patterns = new ArrayList<>();
			for (String domain : config.domainBlackList.split(",")) {
				patterns.add(Pattern.compile(domain));
			}
			domainBlackListPatterns = List.copyOf(patterns); // unmodifiable

			try {
				allowedFileUri = new URI(config.allowedFilePath);
			} catch (URISyntaxException e) {
				LOG.error("configuration error for allowed file path: {}", e.getMessage());
				throw new ResourceProviderConfigurationException(e);
			}

			isConfigured = true;
		}
	}

	public void check(URI base) throws ResourceException, ResourceNotFoundException {
		// enforce protocol constraints
		if (base.getScheme() == null || !allowedProtocolSet.contains(base.getScheme())) {
			LOG.error("rejecting protocol {} in {}", base.getScheme(), base);
			throw new ResourceException("bad protocol: " + base);
		}
		if (base.getScheme().equals("file")) {
			// protect against access to disallowed paths of the file system
			if (!base.normalize()
					.getPath()
					.startsWith(allowedFileUri.normalize().getPath())) {
				LOG.warn("rejecting access to file system: {}", base);
				throw new ResourceNotFoundException("not found");
			}
		} else {
			// match against white and black list
			String domain = base.getHost();
			if (!(inPatterns(domainWhiteListPatterns, domain) || domainWhiteList.isEmpty())
					|| inPatterns(domainBlackListPatterns, domain)) {
				LOG.warn("domain not allowed: {}", domain);
				throw new ResourceException("domain not allowed: " + domain);
			}
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
