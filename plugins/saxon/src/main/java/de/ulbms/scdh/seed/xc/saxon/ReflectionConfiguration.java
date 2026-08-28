package de.ulbms.scdh.seed.xc.saxon;

import com.ibm.icu.text.UnicodeSet;
import io.quarkus.runtime.annotations.RegisterForReflection;
import net.sf.saxon.Configuration;
import nu.validator.htmlparser.extra.ChardetSniffer;
import nu.validator.htmlparser.sax.HtmlParser;
import org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.xmlresolver.loaders.XmlLoader;

@RegisterForReflection(
		targets = {
			SaxonXslTransformation.class,
			SaxonXQueryTransformation.class,
			XslTransformationExceptionParser.class,
			Configuration.class,
			XmlLoader.class,
			XIncludeAwareParserConfiguration.class,
			DTDDVFactoryImpl.class,
			HtmlParser.class,
			UnicodeSet.class,
			ChardetSniffer.class
		})
public class ReflectionConfiguration {}
