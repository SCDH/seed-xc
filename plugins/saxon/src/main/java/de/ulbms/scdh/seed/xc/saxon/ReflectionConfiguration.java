package de.ulbms.scdh.seed.xc.saxon;

import io.quarkus.runtime.annotations.RegisterForReflection;
import net.sf.saxon.Configuration;
import org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.ccil.cowan.tagsoup.Parser;
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
			Parser.class
		})
public class ReflectionConfiguration {}
