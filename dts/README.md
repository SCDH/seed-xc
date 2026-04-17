# DTS

This is an implementation of the API endpoints specified
for [distributed text services](https://dtsapi.org/specifications/)
(DTS):

- ✅ navigation 
- ✅ document
- 🚧 collection
- ❗entry: [Must be placed somewhere else!](Entry)

[OpenAPI specs](https://zivgitlab.uni-muenster.de/SCDH/apis/dts-openapi-specs/-/blob/main/facade-openapi.yaml?ref_type=heads)

The DTS business logic is implemented in XSLT. It can be found on
SCDH's github space: [DTS
Transformations](https://github.com/scdh/dts-transformations). These
packages are used by default, but can be replaced by other
transformations.

## Getting started

### Dev Server

All commands must be run in the root directory of SEED XC, **not**
from the `dts` subfolder.

1. Build:

```shell
./mvnw -Pdownload-openapi generate-sources
./mvnw generate-sources package
```

Whenever you want to get a new version of the OpenAPI specs or XSLT
run `./mvnw clean` first and the above commands then.

2. Run dev server

```shell
./mvnw -Pdts quarkus:dev
```

The dev server will be available on
[http://localhost:8080](http://localhost:8080). (See third-last line
of output about the "Quarkus Main Thread".)

### Testing with cURL

```shell
curl -X 'GET' \
  'http://localhost:8080/navigation?resource=hello.xml' \
  -H 'accept: application/ld+json'
```

## FAQ

#### What Happend to the Entry Endpoint?

The entry endpoint is the killer feature of DTS. It's a service
registry with links to other services! These links come as URI
templates, which is  the other killer feature of DTS.

These two features give the freedom to distribute DTS endpoints to
different base URLs.

So: Don't look for the entry endpoint in the SEED DTS implementation,
but rather put your entry endpoint as static asset on some site and
link it to endpoints of an instance of the SEED DTS service.

#### Where do the Documents live?

Everywhere and nowhere! The service connects to a persistence layer by
a [resource provider plugin](../plugins/resource-providers). You can
connect every store you like, ranging from local filesystem, to RDBMS,
the web, XML-database, Invenio RDM (Zenodo), etc.

#### How is the Resource Provider Plugins configured?

You can determine the resource provider plugin by a bean
mechanism. The `cr` parameters can be used to pass information through
URLs. Everything else is up to the plugin. See [Wiki](../../wiki).

#### Why do you break the API with these `c*` Parameters?

No, these parameters do not break the specs. The specs gives you the
freedom to define URI templates with a set of certain
parameters. There may be other query and path parameters in the URL.

#### How to get Follow Up Links?

You can use the `cf` parameters for passing information to your
transformation that runs the endpoint. User for passing information
directly or indirectly into the transformation. You will need a
specialized module to evaluate it.

#### What about `mediaType`?

The `mediaType` parameter of the document endpoint will soon be
supported. Selecting a `mediaType` will select a different
transformation. DTS Transformations offers some static
[`media-type-*` stylesheet
parameters](https://github.com/SCDH/dts-transformations/blob/main/xsl/document.xsl)
that can be used to get different compiled transformations on top of
the `document.xsl` machinery.

#### Can I use my own XSLT?

Yes. The endpoint can freely be bound to a transformation. You just
have to provide stylesheets, which get compiled at startup of the
service and are then used to process all subsequent request.

#### Can I use XQuery instead of XSLT!

Yes. XSLT comes just as a low level plugin amongst other. So you're
not forced to it. There are [multiple types of
transformations](../plugins/).

#### Why SPARQL for the Collections Endpoint?

That's just a default. You can bind a [transformation of any other
type](../plugins). The reason behind it is the diversity of metadata
in TEI but the relative stability of metadata in RDF-based models like
LRMoo, FRBRoo or even DCterms. And the endpoint delivers linked open
data.––It's much easier to write a generic SPARQL query than a generic
XQuery that goes for metadata.

#### Nice! Can I add my own endpoint to the service?

Yes. That's the point of this project's approach!

#### What's your Plans?

An endpoint that recalculates Web Annotation Selectors into parts of
parts of a documents to selectors into the base document (and vice
versa) and from media type to the base format (and vice versa). It
will be based on [Selene](https://github.com/scdh/selene). That's why
we go along with XSLT for DTS processing and this whole SEED XC thing.
