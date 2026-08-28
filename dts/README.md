# DTS

SEED DTS Server is a versatile level 1 DTS Server following [DTS
conformance](https://dtsapi.org/specifications/versions/v1.0/#conformance)
rules.


| endpoint   | implementation | URI template                                                          |
|:-----------|:---------------|:----------------------------------------------------------------------|
| entry      | ✅             | `BASE_URL/FRONT/entry`                                                |
| collection | ✅             | `BASE_URL/FRONT/collection/{id}{?nav,page}`                           |
| navigation | ✅             | `BASE_URL/FRONT/navigation/{resource}{?tree,ref,start,end,down,page}` |
| document   | ✅             | `BASE_URL/FRONT/document/{resource}{?tree,ref,start,end,mediaType}`   |

SEED DTS Server is designed for 1:n deployment, i.e., one service
instance can serve a multitude of projects (entry points). A project
is selected by the `FRONT` (see [below](#parts-of-the-url)) part of
the requested URL. Different document storage systems can be [plugged
in](../plugins/README.md#resource-provider-plugins), e.g. InvenioRDM,
RDBMS, http file servers, lookup per URN, or simply the local file
system. The storage is not integrated into the SEED DTS service, which
is a micro service providing the DTS community API alone. This design
aims at reducing deployment costs on the long run.

The creation of the endpoints' response bodies is done through
transformations. The default setup offers advanced XSLT processing of
TEI-XML documents, based on [DTS
Transformations](https://github.com/scdh/dts-transformations). These
transformations conform to the DTS and TEI specs and are extensible
for getting content types (media types) other than
`application/tei+xml`. However, users can replace the transformations
completely with their own transformations using one of the [supported
transformations of SEED
XC](../plugins/README.md#transformation-plugins): XSLT,
XQuery, SPARQL. Thus, it is possible to serve any kind of documents
with SEED DTS, not only TEI-XML.

## Extensions

The purpose of the two endpoints based on [Selene Selection
Engine](https://github.com/scdh/selene) is to transform the selectors
of [web annotations]() to target the different representations
provided by the `document` endpoint. A selector transformation is done
using the same transformation as is used for generating the resource
representation.

| endpoint                 | Verb | URI template                                                           | POST parameters                 |
|:-------------------------|:-----|:-----------------------------------------------------------------------|:--------------------------------|
| forward web annotations  | POST | `BASE_URL/FRONT/oa/forward/{resource}{?tree,ref,start,end,mediaType}`  | `annotations`, `frame`, `xpath` |
| backward web annotations | POST | `BASE_URL/FRONT/oa/backward/{resource}{?tree,ref,start,end,mediaType}` | `annotations`, `frame`          |

**forward**: Rewrites web `annotations` on `resource` to annotations
on representation provided by document endpoint. **backward**:
Rewrites web `annotations` on representation provided by document
endpoint to annotations on original `resource`. See also [OpenAPI
specs](#openapi-specs).


## Getting started

The official docker image is on [docker.io](https://hub.docker.com/r/scdh/distributed-test-services).

```shell
docker pull scdh/distributed-test-services:latest
```

Start the service:

```shell
docker run -i --rm -p 8080:8080 scdh/distributed-test-services
```

The instance comes with Swagger UI under
http://localhost:8080/q/swagger-ui .

This container image is highly optimized for deployment on cloud
infrastructure, e.g. a kubernetes cluster. Its startup time is far
under a second. With the [DTS
Transformations](https://github.com/scdh/dts-transformations), which
are configured per default, the startup time is in fact less than a
tenth of a second.

### Sample Dataset

There's a sample project
[online](https://scdh.zivgitlabpages.uni-muenster.de/doering-4esra/edition-4esra/collection.json). For
serving it through your local instance, use the following parameters
on the endpoints of the DTS instance, which make up the `FRONT` part
of the URL:

- `provider`: `url`
- `location`: `https://scdh.zivgitlabpages.uni-muenster.de/doering-4esra/edition-4esra/`

Note, that the **trailing slash on the location URL** is important
([right now](https://github.com/SCDH/seed-xc/issues/59)). Also note,
that for curl and any other client, the value of **`location` must be
URL-encoded** (which is done by Swagger UI behind the scenes based on
the datatype declared for `location`):

```shell
curl -X 'GET' 'http://localhost:8080/url/https%3A%2F%2Fscdh.zivgitlabpages.uni-muenster.de%2Fdoering-4esra%2Fedition-4esra%2F/entry'
```

If you want to inspect indented JSON output, pipe it through `yq` like so:

```shell
curl -X 'GET'   'http://localhost:8080/url/https%3A%2F%2Fscdh.zivgitlabpages.uni-muenster.de%2Fdoering-4esra%2Fedition-4esra%2F/collection' | yq
```



### Parts of the URL

Based on the example dataset, let's have a look at the parts of the DTS URL:

```txt
http://localhost:8080/url/https%3A%2F%2Fscdh.zivgitlabpages.uni-muenster.de%2Fdoering-4esra%2Fedition-4esra%2F/collection
<--   BASE_URL    -->/<--                 FRONT                                                            -->/<-- ENDPOINT'S URI TEMPLATE -->
```

So `FRONT` has these two parts, which are URL parameters: `provider`
(which has value `url` in the example), and `location` (which is the
URL-encoded base URL
`https://scdh.zivgitlabpages.uni-muenster.de/doering-4esra/edition-4esra/`
in the example). `FRONT` is a constant for every project. So do not
take it as an extension of DTS URI templates!

For more details see [docs](../doc/dts.md#front).

### OpenAPI Specs

The OpenAPI
[spec](https://github.com/SCDH/dts-openapi/blob/main/facade-openapi.yaml)
is in an extra repo. The spec can best be explored via
[petstore.swagger.io](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/SCDH/dts-openapi/refs/tags/0.6.0/standalone/facade-openapi.yaml).



### Preparing your own Edition

Read the [documentation](../doc/dts-records.md) on how to serve your own
content and on how to mount it into the instance for testing.

## Development

To build the service and get into the details of the implementation or
the container image, start with reading [dev.md](dev.md)!

## FAQ

#### Where do the Documents live?

Everywhere and nowhere! The service connects to a persistence layer by
a [resource provider
plugin](../plugins/README.md#resource-provider-plugins). You can
connect every store you like, ranging from local filesystem, to RDBMS,
the web, XML-database, Invenio RDM (Zenodo), etc.

#### Can I use my own XSLT?

Yes. The endpoint can freely be bound to a transformation. You just
have to provide stylesheets, which get compiled at startup of the
service and are then used to process all subsequent request.

#### Can I use XQuery instead of XSLT!

Yes. XSLT comes just as a low level plugin amongst other. So you're
not forced to it. There are [multiple types of
transformations](../plugins/).

#### I don't have TEI-XML. Can I use this DTS implementation nevertheless?

Yes. XML processing is just a detail of the currently existing low
level [transformation plugins](../plugins). Transformers for other
data formats may be plugged in as well. XML is not part of the
high-level components of SEED XC (albeit the X in XC).

#### Why SPARQL for the Collections Endpoint?

It's impossible to write generic XQuery for delivering collection data
directly from TEI. But it's possible from extracted graph data
following FRBRoo, LRMoo or DCterms. So we recommend extracting RDF
triples ahead-of-time, e.g. in a CI pipeline, and deposit them at an
accessible location. At Münster, we're using [XTriples
Micro](https://github.com/scdh/xtriples-micro) for this extraction.

However, it's quite simple to change the transformation type. SPARQL
is just the default. You can bind a [transformation of any other
type](../plugins).

#### Nice! Can I add my own endpoint to the service?

Yes. That's the point of this project's approach!

Have a look at the existing endpoints. Their code is just for passing
over to the plugin system. Very straight forward. They implement
interfaces generated from OpenAPI specifications. Have a look into the
`pom.xml` file and watch out for the OpenAPI-Generator plugin.

#### What's your Plans?

An endpoint that recalculates [Web Annotation
Selectors](https://www.w3.org/TR/annotation-model/#selectors) into
parts of parts of a documents to selectors into the base document (and
vice versa) and from media type to the base format (and vice
versa). It will be based on
[Selene](https://github.com/scdh/selene). That's why we go along with
XSLT for DTS processing and this whole SEED XC thing. We currently
need the declarative paradigm of XSLT for providing this at a generic
level.
