# SEED XC - Composable XML REST Services

This project provides high-level components for XML-processing REST
services and different services built from them.

Services

- [SEED XML Transformer](transformer): A RESTful webservice for
  transforming XML (and even HTML tag soup) with XSLT, XQuery, etc.
- [DTS](dts): A RESTful webservice implementing a subset of the
  endpoints of Distributed Text Services
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
- [XSLT](xslt): a plugin to the SEED XC Transformation Map that
  provides transformation by XSLT

Planned components:

- XQuery: a plugin similar to the XSLT plugin, but for XQuery
- Source plugins: Plugins for accessing source documents from
  different storage types by ID: URL, URN, DOI, local filesystem,
  Solr, OpenSearch, DBMSs


## Getting Started

Build:

```shell
./mvnw generate-sources package
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
