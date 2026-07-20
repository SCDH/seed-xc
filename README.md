# SEED XC - Reactive Micro Services for XML and RDF Processing

[![Tests](https://github.com/SCDH/seed-xc/actions/workflows/test.yaml/badge.svg)](https://github.com/SCDH/seed-xc/actions/workflows/test.yaml)
[![Formatting](https://github.com/SCDH/seed-xc/actions/workflows/formatting.yaml/badge.svg)](https://github.com/SCDH/seed-xc/actions/workflows/formatting.yaml)

This project provides RESTful micro services for XML processing and
high-level components for building them. It is based on
[Quarkus](https://quarkus.io/about/), a stack for writing supersonic
Java applications, and
[Mutiny](https://smallrye.io/smallrye-mutiny/latest/), an event-driven
reactive programming API: Together they are the basis for
[non-blocking, responsive, elastic and
resilient](https://quarkus.io/guides/quarkus-reactive-architecture)
services, that can serve thousands of requests per second.

**Services:**

- [DTS](dts): A level 1 implementation of [Distributed Text
  Services](https://github.com/distributed-text-services/specifications)
- [SEED XML Transformer](transformer): A RESTful webservice for
  transforming XML (and even HTML tag soup) with XSLT, XQuery, etc.
- [SEED XSLT Compiler](compiler): Compiles XSLT to
  [SEF](https://www.saxonica.com/saxonjs/documentation3/index.html)
  (needs a license for the Saxon entreprise edition)

**Components:**

- [SEED API](api): [OpenAPI
  specs](api/src/main/resources/openapi/seed-xc-openapi.yaml) and
  derived data models and service interfaces, common supporting
  classes
- [SEED XC Transformation Map](transformations): compile
  transformations once ahead of time and make them available for
  processing subsequent transformation request instantly
- Transformations: Plugins for the SEED XC Transformation Map
  generating output representations from input streams
  - [XSLT](plugins/saxon): XML Stylsheet Language Transformtions
    driven by [Saxon HE](https://www.saxonica.com/)
  - [XQuery](plugins/saxon): same as the XSLT plugin, but for the XMM
    Query language
  - [SPARQL](plugins/seed-xc-sparql): a [Apache
    Jena](https://jena.apache.org/)-based plugin for running a SPARQL
    query against a serialized RDF graph. For generating JSON-LD
    output, it can run JSON-LD framing based on [Titanium JSON
    LD](https://github.com/filip26/titanium-json-ld). So this plugin
    is perfect for serving nice JSON APIs from RDF knowledge graphs.
- ResourceProviders: Plugins for accessing resources (source
  documents) from different storage types
  - [local filesystem](plugins/resource-providers): activated per default in tests
  - [URL](plugins/resource-providers): activated per default
  - URN (planned)
  - InvenioRDM by DOI (planned)
  - RDBMS (planned)
- [transformation-info.xsl](utils): XSLT stylesheet for generating
  configuration files for an XSLT stylesheet or package that is used
  in an transformation. See [documentation](doc/configuration.md).


This project is part of the SEED, which is--choose one--either a
recursive acronym for SEED *E*lectronic *Ed*itions or a word build
from the alphabet D S E (the acronym for digital scholarly editions).

## Getting Started

This project provides container images on [docker
hub](https://hub.docker.com/r/scdh/) for each service, as soon as
possible.

**DTS**: See [`dts` directory](dts)

**XML Transformer**: See [`transformer` directory](transformer)

**XSLT Compiler**: See [`compiler` directory](compiler)

Don't forget to read the [docs](doc).

For the container images, the services are compiled to a native Linux
executable and are then for [security](doc/security.md) reasons build
into a RedHat UBI micro image. Images are small, and have a fantastic
startup time.

You can also run dev services. Instructions are provided under the
links above. But notice, that you cannot deploy a Quarkus application
on Tomcat, TomEE, WildFly etc.


## Contributing

See [contributing guide](contributing.md)!

## License

MIT
