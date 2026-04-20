# SEED XC - XML Processing with Composable REST Services

[![Tests](https://github.com/SCDH/seed-xc/actions/workflows/test.yaml/badge.svg)](https://github.com/SCDH/seed-xc/actions/workflows/test.yaml)
[![Formatting](https://github.com/SCDH/seed-xc/actions/workflows/formatting.yaml/badge.svg)](https://github.com/SCDH/seed-xc/actions/workflows/formatting.yaml)

This project provides micro services for XML processing and high-level
components for building them.

Services

- [SEED XML Transformer](transformer): A RESTful webservice for
  transforming XML (and even HTML tag soup) with XSLT, XQuery, etc.
- [DTS](dts): A RESTful webservice implementing a subset of the
  endpoints of [Distributed Text
  Services](https://github.com/distributed-text-services/specifications)
- [SEED XSLT Compiler](compiler): Compiles XSLT to
  [SEF](https://www.saxonica.com/saxonjs/documentation3/index.html)
  (needs a license for the Saxon entreprise edition)

Components

- [SEED API](api): [OpenAPI
  specs](api/src/main/resources/openapi/seed-xc-openapi.yaml) and
  derived data models and service interfaces, common supporting
  classes
- [SEED XC Transformation Map](transformations): compile
  transformations once ahead of time and make them available for
  processing subsequent transformation request instantly
- [XSLT](plugins/saxon): a plugin to the SEED XC Transformation Map
  that provides transformation by XSLT
- [XQuery
  (branch)](https://github.com/SCDH/seed-xc/blob/xquery/plugins/saxon/src/main/java/de/ulbms/scdh/seed/xc/saxon/SaxonXQueryTransformation.java):
  a plugin similar to the XSLT plugin, but for XQuery . SPARQL
  (planned): a plugin similar to XSLT, but for running a SPARQL query
  against a serialized RDF graph
- [ResourceProviders](plugins/resource-providers): Plugins for
  accessing source documents from different storage types by ID: URL,
  URN, DOI, local filesystem, Solr, OpenSearch, DBMSs


This project is part of the SEED, which is--choose one--either a
recursive acronym for SEED *E*lectronic *Ed*itions or a word build
from the alphabet D S E (the acronym for digital scholarly editions).

## Getting Started

For DTS see [`dts` directory](dts)!

Build:

Once after cloning and after each cleanup:

```shell
 ./mvnw -Pdownload-openapi generate-sources
```

Then building all components:

```shell
./mvnw package
```

To run one of the services in dev mode, use `-P` switch to select a
service profile.

XSLT Compiler:

```shell
./mvnw -Pcompiler quarkus:dev
```

XML Transformer:

```shell
./mvnw -Ptransformer quarkus:dev
```

Dev-Server will listen on http://localhost:8080


## Contributing

See [contributing guide](contributing.md)!
