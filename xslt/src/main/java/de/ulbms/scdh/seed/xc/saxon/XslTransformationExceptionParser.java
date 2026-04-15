package de.ulbms.scdh.seed.xc.saxon;

import static jakarta.ws.rs.core.Response.Status;

import de.ulbms.scdh.seed.xc.api.TransformationException;
import de.ulbms.scdh.seed.xc.api.TransformationExceptionParser;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.UncheckedXPathException;
import net.sf.saxon.trans.XPathException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link TransformationExceptionParser} implementation
 * tries to get the HTTP status code and the error message
 * deeply from the error stack, since {@link SaxonApiException}
 * does not give access to the message sent by
 * <code>xsl:message</code> or <code>xsl:assert</code>. One has
 * to get back to the {@link XPathException} to get more
 * information.<P/>
 *
 * The error code is parsed from the message. Currently, getting
 * it from <code>xsl:assert/@error-code</code> does not work.
 */
@ApplicationScoped
public class XslTransformationExceptionParser implements TransformationExceptionParser {

	private static final Logger LOG = LoggerFactory.getLogger(XslTransformationExceptionParser.class);

	private final boolean head;

	/**
	 * Creates a new {@link XslTransformationExceptionParser} instance.
	 * @param head - Whether the status code should be parsed only from its head or from the full message.
	 */
	public XslTransformationExceptionParser(
			@ConfigProperty(
							name = "de.ulbms.scdh.seed.xc.saxon.XslTransformationExceptionParser.head",
							defaultValue = "true")
					Boolean head) {
		this.head = head;
	}

	@Override
	public int parseCode(TransformationException err) {
		if (err == null) return Status.INTERNAL_SERVER_ERROR.getStatusCode();
		LOG.debug("parsing TransformationException with message {}", err.getMessage());
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
		if (err == null) return "null";
		LOG.debug("getting message from TransformationException with message {}", err.getMessage());
		Throwable cause = err.getCause();
		if (cause == null) return err.getMessage();
		else if (cause instanceof SaxonApiException) return message((SaxonApiException) cause);
		else if (cause instanceof TransformationException) return message((TransformationException) cause);
		else return message(cause);
	}

	public int parseCode(SaxonApiException err) {
		LOG.debug("parsing SaxonApiException: {}", err.getErrorCode());
		Throwable cause = err.getCause();
		// XPathException is more informative than SaxonApiException.
		// Thus, try to get it first.
		if (err.getErrorCode() == null) {
			LOG.debug("no information from SaxonApiException");
			return Status.INTERNAL_SERVER_ERROR.getStatusCode();
		} else if (cause instanceof XPathException) {
			return parseCode(((XPathException) cause));
		} else if (cause instanceof UncheckedXPathException) {
			return parseCode(((UncheckedXPathException) cause).getXPathException());
		} else if (err.getErrorCode() == null) {
			return Status.INTERNAL_SERVER_ERROR.getStatusCode();
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
		if (err.getErrorCode() == null) {
			LOG.debug("no information from SaxonApiException");
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
		LOG.debug(
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
			if (head) {
				msg = err.getErrorObject().head().getStringValue();
			} else {
				msg = err.getErrorObject().materialize().getStringValue();
			}
		} catch (XPathException e) {
		}
		if (msg != null) return msg;
		return err.getMessage();
	}

	public int parseCode(Throwable err) {
		LOG.debug("parsing throwable");
		return Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}

	public String message(Throwable err) {
		LOG.debug("parsing throwable");
		return err.getMessage();
	}

	public int parseXslMessage(String msg) {
		LOG.debug("parsing xsl:message {}", msg);
		Pattern p = Pattern.compile("([1-9][0-9][0-9])");
		Matcher m = p.matcher(msg);
		if (m.find()) return Integer.parseInt(m.group(1));
		else return Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}
}
