# DTS

This is an implementation of the API endpoints specified
for [distributed text services](https://dtsapi.org/specifications/)
(DTS):

- ❗entry: [Must be placed somewhere else!](#what-happend-to-the-entry-endpoint)
- 🚧 collection
- ✅ navigation 
- ✅ document

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

Swagger UI is available under
[http://localhost:8080/q/dev-ui/quarkus-smallrye-openapi/swagger-ui](http://localhost:8080/q/dev-ui/quarkus-smallrye-openapi/swagger-ui).

Per default, the service serves files from the [`samples`](../samples)
directory, which currently has only a single document, `john.xml`.

To serve TEI files from an other local directory, use the
`seed-dts.filesystem` property like so, where `PATH` must be an
absolute path.

```shell
./mvnw -Dseed-dts.filesystem=PATH -Pdts quarkus:dev

```

Have a look at
[`src/main/resources/application.properties`](src/main/resources/application.properties)
for more config options.


### Testing with cURL

```shell
curl -X 'GET' \
  'http://localhost:8080/navigation?resource=john.xml' \
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
