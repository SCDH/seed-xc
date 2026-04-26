# SPARQL Plugins

This module provides transformation plugins. It is based on
[Apache Jena](https://jena.apache.org/) and uses
[Titanium JSON-LD](https://github.com/filip26/titanium-json-ld)
for JSON-LD framing.

For JSON-LD contexts used in framing, this plugin allows outbound
URL connections passing by the `ResourceProvider`.

## Transformation Types

- `sparql-construct`

## Content Negotiation

- If `TransformationInfo.mediaType` is present, then and only then it determines the returned format.
- If there's no content type on the transformation level and content negotiation by the request's `Accept` is used.
- In case no content type is defined on the transformation level nor one is requested, the content tpye of input resource will be used
- As a fallback, a default content type is used.

## Transformation Parameters

Request parameters are passed to the query with a mechanism by
[Apache Jena](https://jena.apache.org/documentation/query/parameterized-sparql-strings.html).
It replaces all occurrences of a SPARQL variable `?X` with a literal.

Parameter types should be declared with the parameter descripter as `xs:...` types.
`xs:string` is used as default.

## Application Properties

- `url-connect-timeout`: time limit for establishing a connection to a remote JSON-LD context URL for framing, defaults to 10s
- `url-read-timeout`: time limit for fetching a remote JSON-LD context for framing, defaults to 10s
- `context-max-size`: size limit of JSON-LD context for framing, defaults to 1MB
