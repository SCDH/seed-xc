# SEED XC Plugins

## Resource Provider Plugins

| Storage     | State   | Provider ID | Location Example                                   | Module                                        | important config                                               |
|:------------|---------|:------------|:---------------------------------------------------|-----------------------------------------------|:---------------------------------------------------------------|
| file system | ✅      | `file`      | `projects%20hsde`                                  | [seed-resource-providers](resource-providers) | base path for file system protection                           |
| web via URL | ✅      | `url`       | `https%3A%2F%2Fscdh.github.io%2Fsample-edition%2F` | [seed-resource-providers](resource-providers) | black lists, white lists, allowed protocols, allowed file path |
| web via URN | planned | `urn:RES`   |                                                    |                                               |                                                                |
| InvenioRDM  | planned | `doi`       |                                                    |                                               |                                                                |
| RDBMS       | planned |             |                                                    |                                               |                                                                |

## Transformation Plugins

| Technology       | State   | type               | Module                         | input      | output   |
|:-----------------|:--------|:-------------------|:-------------------------------|------------|----------|
| XSLT             | ✅      | `xslt`             | [seed-xc-saxon](saxon)         | XML, HTML¹ | *        |
| XQuery           | ✅      | `xquery`           | [seed-xc-saxon](saxon)         | XML, HTML¹ | *        |
| SPARQL construct | ✅      | `sparql-construct` | [seed-xc-jena](seed-xc-sparql) | any RDF²   | any RDF² |
| identity         | planned | `identity`         |                                | *          | *        |

Notes:

1. [Xerces](https://xerces.apache.org/xerces2-j/) is the default
   parser for XML input, but the [HTML tagsoup
   parser](https://about.validator.nu/htmlparser/) can be selected
   instead. Xerces can be configured to parse XInclude aware.
2. As input and output, any RDF serialization format known by [Apache
   Jena](https://jena.apache.org/documentation/io/) can be processed,
   including JSON-LD 1.1 via [Titanium JSON
   LD](https://github.com/filip26/titanium-json-ld).
