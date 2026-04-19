# Configuration

The records for configuring transformations must be passed in as a
YAML configuration file. YAML was choosen, because it can simply be
written by hand or be generated automatically. And configurations can
be simple merged with low level tools like `cat`.

The schema is defined in an OpenAPI specification. Look for
`transformationInfo` in
[api/src/main/resources/openapi/seed-xc-openapi.yaml](../api/src/main/resources/openapi/seed-xc-openapi.yaml).

## Generation from XSLT

There's an [XSLT stylesheet](../utils/transformation-info.xsl) for
generating an `TransformationInfo` record for an XSLT stylesheet or
package. Its output is JSON, which can easily be converted to YAML.

Usage examples can be found on other projects:

- [DTS Transformations](https://github.com/scdh/dts-transformations):
  `xsl/navigation.xsl` and `xsl/document.xsl` are transformed with
  with the transformation to JSON. Maven plugins are then used to
  convert JSON to YAML and then the YAML files are merged into a
  config that is added to the SEED artifact. See `pom.xml` for
  details.
- [SEED TEI
  Transformations](https://github.com/scdh/seed-tei-transformations):
  dito
