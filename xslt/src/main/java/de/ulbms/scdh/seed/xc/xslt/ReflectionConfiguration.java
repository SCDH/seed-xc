package de.ulbms.scdh.seed.xc.xslt;

import io.quarkus.runtime.annotations.RegisterForReflection;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.StandardURIResolver;
import net.sf.saxon.lib.StandardUnparsedTextResolver;
import org.xmlresolver.loaders.XmlLoader;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl;

@RegisterForReflection(targets = {
	Configuration.class,
	StandardURIResolver.class,
	StandardUnparsedTextResolver.class,
	XmlLoader.class,
	XIncludeAwareParserConfiguration.class,
	DTDDVFactoryImpl.class
    })
public class ReflectionConfiguration {}
