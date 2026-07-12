# DTS

SEED DTS Server is a versatile level 1 DTS Server following [DTS
conformance](https://dtsapi.org/specifications/versions/v1.0/#conformance)
rules.


| endpoint   | implementation | URI template                                                          |
|:-----------|:---------------|:----------------------------------------------------------------------|
| entry      | ✅             | `BASE_URL/FRONT/entry`                                                |
| collection | ✅             | `BASE_URL/FRONT/collection/{id}{?nav}`                                |
| navigation | ✅             | `BASE_URL/FRONT/navigation/{resource}{?tree,ref,start,end,down}`      |
| document   | ✅             | `BASE_URL/FRONT/navigation/{resource}{?tree,ref,start,end,mediaType}` |

SEED DTS Server is designed for 1:n deployment, i.e., one service
instance can serve a multitude of projects (entry points). A project
is selected by the [`FRONT`](../doc/dts.md#front) part of the
requested URL. Different document storage systems can be [plugged
in](../plugins/README.md#resource-provider-plugins), e.g. InvenioRDM,
RDBMS, http file servers, lookup per URN, or simply the local file
system. The storage is not integrated into the SEED DTS service, which
is a micro service providing the DTS community API alone. This design
aims at reducing deployment costs on the long run.

[OpenAPI specs](https://github.com/SCDH/dts-openapi/blob/main/facade-openapi.yaml)

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


## Getting started

### Docker

TODO

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
directory. Use `file` as value for the **provider** path parameter,
and `bible` for **location**.

To serve TEI files from an other local directory, use the
`seed-dts.filesystem` property like so, where `PATH` must be an
absolute path.

```shell
./mvnw -Dseed-dts.filesystem=PATH -Pdts quarkus:dev
```

Have a look at
[`src/main/resources/application.properties`](src/main/resources/application.properties)
for more config options.

### Native Executable and Docker Image

A native executable is a build artifact, that can be run without a
Java virtual machine. It is simply a linux executable, compiled from
Java by a special compiler called GraalVM. And, since it is statically
linked for a minimal attack surface, its size is huge: about
170MB. Everything is in there compiled to native machine code: the
Saxon XSLT processor, the Apache Jena SPARQL engine, the web server,
the APIs.

If you want to build the native executable locally on your system: Be
aware, that the compilation takes some amount of time: about 10
Minutes on my Intel i7 with 32GB RAM.

Here is what to do: Docker is required, because the build uses GraalVM
and OpenJDK from a docker container:

```shell
./mvnw -Pdownload-openapi generate-sources
./mvnw -Ddts-native install
```

This produces `seed-dts-VERSION-runner` in `dts/target/`. It has
properties set for operating in a container image. So let's build it:

```shell
docker build -f dts/src/main/docker/Dockerfile.native-micro -t scdh/dts-testing .
```

The service starts up lightning fast. Just call the native executable:

```shell
docker run -i --rm -p 8080:8080 scdh/dts-testing
```

Swagger UI is available at http://localhost:8080/q/swagger-ui

You can mount-bind your own transformations into `/work/resources/`
and your own files into `/work/projects/`.

### Testing with cURL

```shell
curl -X 'GET' \
  'http://localhost:8080/file/bible/entry' \
  -H 'accept: application/ld+json'
```

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
