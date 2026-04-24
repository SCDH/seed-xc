# SPARQL Plugins

This module provides transformation plugins.

## Transformation Types

- `sparql-construct`

## Content Negotiation

- If `TransformationInfo.mediaType` is present, then and only then it determines the returned format.
- If there's no content type on the transformation level and content negotiation by the request's `Accept` is used.
- In case no content type is defined on the transformation level nor one is requested, the content tpye of input resource will be used
- As a fallback, a default content type is used.

## Parameters

Request parameters are passed to the query with a mechanism by
[Apache Jena](https://jena.apache.org/documentation/query/parameterized-sparql-strings.html).
It replaces all occurrences of a SPARQL variable `?X` with a literal.

Parameter types should be declared with the parameter descripter as `xs:...` types.
`xs:string` is used as default.