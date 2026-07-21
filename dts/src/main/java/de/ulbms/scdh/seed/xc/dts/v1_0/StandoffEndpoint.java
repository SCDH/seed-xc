package de.ulbms.scdh.seed.xc.dts.v1_0;

import de.ulbms.scdh.seed.xc.dts.endpoints.StandoffApi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import java.io.InputStream;
import java.net.URI;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

@RequestScoped
public class StandoffEndpoint implements StandoffApi {

	public static final String MEDIATYPE_ANNOTATIONS =
			"application/ld+json;text/turtle;application/rdf+xml;application/n-triples;text/trig;application/n-quads;application/trix+xml;application/rdf+thrift;application/rdf+protobuf";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<String> standoffBackward(
			URI resource,
			URI provider,
			URI location,
			@RestForm @PartType(MEDIATYPE_ANNOTATIONS) InputStream annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			InputStream frame) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uni<String> standoffForward(
			URI resource,
			URI provider,
			URI location,
			@RestForm @PartType(MEDIATYPE_ANNOTATIONS) InputStream annotations,
			String ref,
			String start,
			String end,
			String tree,
			String mediaType,
			InputStream frame) {
		return null;
	}
}
