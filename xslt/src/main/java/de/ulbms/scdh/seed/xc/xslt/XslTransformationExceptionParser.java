package de.ulbms.scdh.seed.xc.xslt;

import static jakarta.ws.rs.core.Response.Status;

import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationExceptionParser;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.UncheckedXPathException;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class XslTransformationExceptionParser implements TransformationExceptionParser {

	private static final Logger LOG = LoggerFactory.getLogger(XslTransformationExceptionParser.class);

	@Override
	public int parseCode(TransformationException err) {
		LOG.info("parsing TransformationException with message {}", err.getMessage());
		Throwable cause = err.getCause();
		if (cause == null) return Status.INTERNAL_SERVER_ERROR.getStatusCode();
		else if (cause instanceof SaxonApiException) return parseCode((SaxonApiException) cause);
		else if (cause instanceof TransformationException) return parseCode((TransformationException) cause);
		else return parseCode(cause);
	}

	/**
	 * This dives into the traceback, since <code>getMessage()</code> does not return
	 * messages send by xsl:message or xsl:assert.
	 */
	@Override
	public String message(TransformationException err) {
		LOG.info("getting message from TransformationException with message {}", err.getMessage());
		Throwable cause = err.getCause();
		if (cause == null) return err.getMessage();
		else if (cause instanceof SaxonApiException) return message((SaxonApiException) cause);
		else if (cause instanceof TransformationException) return message((TransformationException) cause);
		else return message(cause);
	}

	public int parseCode(SaxonApiException err) {
		LOG.info("parsing SaxonApiException: {}", err.getErrorCode());
		Throwable cause = err.getCause();
		// XPathException is more informative than SaxonApiException.
		// Thus, try to get it first.
		if (err.getErrorCode() == null
				&& !(cause instanceof UncheckedXPathException)
				&& !(cause instanceof XPathException)) {
			LOG.info("no information from SaxonApiException");
			return Status.INTERNAL_SERVER_ERROR.getStatusCode();
		} else if (cause instanceof XPathException) {
			return parseCode(((XPathException) cause));
		} else if (cause instanceof UncheckedXPathException) {
			return parseCode(((UncheckedXPathException) cause).getXPathException());
		} else if (err.getErrorCode().getLocalName().equals("XTMM9000")) {
			return parseXslMessage(err.getMessage());
		} else {
			return Status.INTERNAL_SERVER_ERROR.getStatusCode();
		}
	}

	public String message(SaxonApiException err) {
		Throwable cause = err.getCause();
		// XPathException is more informative than SaxonApiException.
		// Thus, try to get it first.
		if (err.getErrorCode() == null
				&& !(cause instanceof UncheckedXPathException)
				&& !(cause instanceof XPathException)) {
			LOG.info("no information from SaxonApiException");
			return err.getMessage();
		} else if (cause instanceof XPathException) {
			return message(((XPathException) cause));
		} else if (cause instanceof UncheckedXPathException) {
			return message(((UncheckedXPathException) cause).getXPathException());
		} else {
			return err.getMessage();
		}
	}

	public int parseCode(XPathException err) {
		// err.getErrorObject() has the message from xsl:message and xsl:assert
		String msg = null;
		try {
			msg = err.getErrorObject().materialize().getStringValue();
		} catch (XPathException e) {
		}
		LOG.info(
				"parsing XPathException: error object '{}', error code qname '{}', show error code '{}'",
				msg,
				err.getErrorCodeQName(),
				err.showErrorCode());
		if (msg != null) return parseXslMessage(msg);
		return Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}

	public String message(XPathException err) {
		// err.getErrorObject() has the message from xsl:message and xsl:assert
		String msg = null;
		try {
			msg = err.getErrorObject().materialize().getStringValue();
		} catch (XPathException e) {
		}
		if (msg != null) return msg;
		return err.getMessage();
	}

	public int parseCode(Throwable err) {
		LOG.info("parsing throwable");
		return Status.BAD_REQUEST.getStatusCode();
	}

	public String message(Throwable err) {
		LOG.info("parsing throwable");
		return err.getMessage();
	}

	public int parseXslMessage(String msg) {
		LOG.info("parsing xsl:message {}", msg);
		Pattern p = Pattern.compile("([1-9][0-9][0-9])");
		Matcher m = p.matcher(msg);
		if (m.find()) return Integer.parseInt(m.group(1));
		else return Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}
}
