# DTS

## Data layout

### Collection Metadata

The Service requires a file named `collection.json` at the base location of a project. It is called the **collection metadata file**. It holds collection metadata served by the collection endpoint.

This file is very similar to the metadata returned by the collection endpoint. But all metadata is merged into a single JSON-LD file, members are not embedded but linked by ID.

Examples are present in [`samples/collection.json`](../samples/collection.json) and––in an ever current state––in the [tests of the dts module](../dts/src/test/resources).

For each resource (document), there must be a property (`seed:location`) that points to a file. The service uses this property to get the file location.

The location is **independent** of the value of DTS' `{resource}` or `{id}` parameters. Providing the `seed:location` property for a resource is the key to this abstraction.

IRIs of collections and resources in the collection metadata file can be relative or absolute. Use `"@base": null` in the `@context` to make them relative. See JSON-LD 1.1 specs, sec. [4.1.3](https://www.w3.org/TR/json-ld11/#base-iri):

> Setting @base to null will prevent relative IRI references from being expanded to IRIs.

The examples linked above use this standards-conformant feature.

As a result, relative IRIs will be used by through `{resource}` and `{id}` in the URI templates. The relative IRIs will be resolved on the base of the request URL in LOD result bodies. This makes them IRIs which can be dereferenced. See [sections](#url-structure) below.

Absolute IRIs in the collection metadata file are simply processed untouched.


## URL structure

### URI templates

The URI templates define the identifiers of collections and resources as a path parameter, while the other parameters are passed in as query parameters:

#### collection

```
BASE/FRONT/collection/{id}{?nav,page}
```

#### navigation

```
BASE/FRONT/navigation/{resource}{?tree,ref,start,end,down,page}
```

#### document

```
BASE/FRONT/document/{resource}{?tree,ref,start,end,mediaType}
```

### `FRONT`

**state: not yet implemented**, see issue #32.

Since this implementation of DTS is a microservice which does not store documents in a persistence layer, but fetches them from a remote location, the URL also has to contain information about this remote location.

So, `FRONT` is 2 path segments in the path part of the URL. The first segment, `PROVIDER` is the type of the location, the second the location. `PROVIDER is internally used to select a resource provider that is able to dereference the location. The location MUST be an url encoded string.

```
BASE/PROVIDER/LOCATION/document/{resource}{?tree,ref,start,end,mediaType}
```

Example, with URN `urn:ulbms:asdf:jklö`.

```
BASE/urn/urn53Aulbms%3Aasdf%3Ajklö/document/{resource}{?tree,ref,start,end,mediaType}
```

```
FRONT         := PROVIDER '/' LOCATION
PROVIDER      := PROVIDER_TYPE [ SEP OPTIONS ]
PROVIDER_TYPE := 'url' | 'urn' | 'doi' | 'file'
SEP           := ':'
```


## Linked Open Data

Using DTS URLs as IRIs in a knowledge graph is considered by the DTS specification as well as the URL design of the SEED DTS implementation.

Passing the `resource` parameter as a path parameter and the other parameters as query parameters is the result of the consideration, as well as making the location information (FRONT) a part of the path part of the URL.

This structure makes it easy to identify the overall resource and stripping away parts and representation as media type (content type). It also allows to resolve relative IRIs. 

### URL Abstraction Through URNs

Important to know in the LOD context: URNs with a [q-component](https://datatracker.ietf.org/doc/html/rfc8141#section-2.3.2) offer an abstraction layer with the possibility to pass on query parameters to the DTS service.

We could resolve URNs to DTS URLs and pass the query parameters from the endpoints' URI templates through as the q-component of URNs. E.g.

```
urn:ulbms:dts:logic:v2:PortRoyal?=tree=chap&ref=sec3.2
```
could resolve to

```
https://dts.scdh.uni-muenster.de/urn/urn&3Aulbms%3Acorpora%3Alogic%3Av2/document/PortRoyal?tree=chap&ref=sec3.2
```

So, having such URNs with q-component that resolve to a DTS instance is a good idea to have stable IRIs in a knowledge graph with knowledge about texts.
