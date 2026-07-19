# DTS Records

This chapter described how to create a record for publication through DTS.

## Requirements

1. Your TEI-XML documents
1. a `collection.json` metadata file listing your documents as DTS
   resources and describing the collection structure they appear in.

#### Example

Assume this little edition encompassing 8 versions of forth book of
Ezra, an apocrypha by the prophet Ezra. And there's `collection.json`.

```txt
├── 4Esra-et-de.tei.xml
├── 4Esra-et.tei.xml
├── 4 Esra.xpr
├── 4Ezr-hy-en.tei.xml
├── 4Ezr-hy.tei.xml
├── 4Ezr-la-de.tei.xml
├── 4Ezr-la.tei.xml
├── 4Ezr-sy-de.tei.xml
├── 4Ezr-sy.tei.xml
├── alignment.csv
└── collection.json
```


### TEI-XML documents

You are free to do whatever you do in TEI-XML.

Add you should add `citeStructure` declarations to describe how
citable units are shaped.

### `collection.json`

This file is similar to the collection endpoint's responses, but

1. everything in one file
2. many properties are calculated by the service and can be dropped
3. must contain a `seed:location` property for each DTS Resource,
   which points to a location, where the service find it.
   

#### Example

```json
{
    "@graph": [ ⑩
    {
        "@id": "general", ❷
        "@type": "Collection",
        "title": "Sample Collection of DTS Transformations",
        "dublinCore": { ❸
			"title": [
				{
					"lang": "en",
					"value": "Sample Collection of DTS Transformations"
				}
			]
        },
        "member": [ ❹
            { "@id": "4Ezr-la" },
            { "@id": "4Ezr-la-de" },
            { "@id": "4Ezr-et" },
            { "@id": "4Ezr-et-de" },
            { "@id": "4Ezr-hy" },
            { "@id": "4Ezr-hy-en" },
            { "@id": "4Ezr-sy" },
            { "@id": "4Ezr-sy-de" }
        ]
    },
    {
        "@id": "4Ezr-la", ❺
        "@type": "Resource", ❻
        "title": "4 Book of Ezra, Latin",
        "seed:location": { ❼
			"@type": "Path", ❾
			"path": "4Ezr-la.tei.xml" ❿
        },
        "dublinCore": {
        "title": [
            {
            "lang": "en",
            "value": "4 Book of Ezra, Latin"
            }
        ],
        "identifier": [
            {
            "lang": "en",
            "value": "4Ezr-la"
            }
        ]
        }   
    },
    {
        "@id": "4Ezr-la-de",
        "@type": "Resource",
        "title": "4 Book of Ezra, German translation of Latin text",
        "seed:location": {
			"@type": "Path",
			"path": "4Ezr-la.tei.xml"
        },
        "dublinCore": {
			"title": [
				{
					"lang": "en",
					"value": "4 Book of Ezra, Latin, Translation to German"
				}
			],
			"identifier": [
				{
					"lang": "en",
					"value": "4Ezr-la-de"
				}
			]
        }   
    },
	....
    ],
    "@context": [ ⓫
    "https://scdh.github.io/dts-transformations/latest/xsl/context/modules/dct-nest.json",
    "https://scdh.github.io/dts-transformations/latest/xsl/context/modules/1.0rc1.json",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/seed.json",
    {
        "@base": null, ⓬
        "extensions": "@nest"
    }
    ]
}
```

1. The collections and resources go into the default graph of a
   JSON-LD file. It's an array containing objects with `@id`s, that
   make up the records DTS collection and DTS resource objects.
2. The default (starting) collection has the `@id` `general`.
12. IDs can be absolute or relative IRIs. To make them relative like
	in the example above (e.g. ❷, ❺), set the `@base` URI to null like
	in ⓬. This conforms the JSON-LD 1.1 standard; see specs, sec
	[4.1.3](https://www.w3.org/TR/json-ld11/#base-iri).
3. DCTerms metadata
4. A collection lists its members by identifiers. This allows members
   to appear in multiple collections without redundancy.
5. Resources have identifiers, that are completely independent of file
   names.
6. They must be of type `Resource`.
7. For Resources, there will be a file lookup base on the
   `seed:location` property. The location is future-proof object with a
8. type and a
9. `path` in case it is a `Path` location.
10. The path is a URI. Relative URIs will be resolved relative against
   the URI of the `collection.json` file, aka the base location of the
   record. Links into a zip container will be supported in future.
11. The JSON-LD context makes this a representation of an RDF
    graph. The `collection.json` will be parsed as a RDF graph on the
    server and processed with RDF tools like SPARQL.
	
## `@id`

The identifiers in `collection.json` determine the IRIs of the linked
open data (LOD) objects, that are returned by the DTS endpoints. They
will appear in the URLs (`{id}` or `{resource}`) of the endpoints and
as values of the `@id` properties of the response objects.

So, the identifiers must be understood as URIs or
[IRI](https://www.w3.org/TR/rdf11-concepts/#section-IRIs)s in the LOD
graph, that your documents will referenced in via DTS.

### Absolute IRIs

Absolute IRIs make it as they are into the `{id}` or `{resource}` slot
of the URI templates. The `@id` property will contain the request URI
containing them.

You can use the `"@base": "http://my.project.example.com/asdf/"`
property in the `@context` to shorten them.

### Relative IRIs

Relative IRIs also make it to the `{id}` or `{resource}` slot of the
URI templates, and the `@id` property will contain the request URI
containing them. That means, that the are made absolute by resolving
them against the base URL of the DTS record.

#### Example

From above:

```json
"@id": "4Ezr-la", ❺
```

Will make up these accessible IRIs:

```
BASE/FRONT/collection/4Ezra
BASE/FRONT/navigation/4Ezra
BASE/FRONT/document/4Ezra
```

Note: It was a clear decision to pass `{id}` or `{resource}` as path
parameters in the URI templates and to use query parameters for the
rest of the arguments. Reason: The rules of resolving relative IRIs
strip the query part altogether.

### URL Encoding

Identifiers have to be understood as IRIs, i.e. URIs. URIs in URLs
must be URL-encoded. Otherwise, the URL cannot be parsed cleanly.

That means, that e.g. `/` has to be written as `%2F`. So, if you want

```json
"@id": "Ezr/4Ezr-la",
```

use

```json
"@id": "Ezr%2F4Ezr-la",
```

instead.

If you encounter problems with URL encoding identifiers, do not
hesitate to file an issue report or start a discussion.

### Recommendation

Generally, I recommend to use relative IRIs in the `collection.json`
metadata file and to avoid characters, that need URL encoding.


## Metadata

### Blank Node

### Nesting

### IRI

```json
{
	"@id": "../../document/Ezr-la",
	"title": "4 Book of Ezra. Latin",
	...
}
```

will expand to 

```json
{
	"@id": "BASE/FRONT/document/Ezr-la",
	"title": "4 Book of Ezra. Latin",
	...
}
```

so the metadata is stated to be about the document.

## Configuration on a Per-Record Basis

In addition to collection and resource metadata, the `collection.json`
file can also contain configuration options, that adapt the
transformations run on the DTS service.

These options are contained in `/configuration` property.

For the configuration to work, the LOD context definition has to
include a binding for the `configuration` property. In the follow
example, this is simply done by adding
`https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/seed.json`
❶ to the context array. If you want a monolithic context, have a look
at
[seed.json](../dts/main/resources/META-INF/resources/context/seed.json)
and copy the definitions in there.


#### Example

```json
{
	"@graph": [ ... ],
	"@context": [
		"https://scdh.github.io/dts-transformations/latest/xsl/context/modules/dct-nest.json",
		"https://scdh.github.io/dts-transformations/latest/xsl/context/modules/1.0rc1.json",
		"https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/seed.json", ❶
        {
			"@base": null,
	    	"extensions": "@nest"
		}
    ],
	"configuration": { 
		"frames": {
			"collection": {
				"document": { ❷
					"member": {
						"@embed": "@always",
						"@omitDefault": true
					},
					"@context": [
						"https://scdh.github.io/dts-transformations/latest/xsl/context/modules/dct-obj.json",
						"https://scdh.github.io/dts-transformations/latest/xsl/context/modules/1.0rc1.json",
						{
							"extensions": "@nest",
							"Document": { "@id":  "dts:Document" }
						}
					],
					"dts:requested": {}
				}
			},
			"navigation": { ❸
				"location": "https://dtsapi.org/specifications/context/2.0.json"
			}
		}		
	}
```

In this example, the record rewrites the JSON-LD context (frames) for
the `collection` and for the `navigation` endpoints. The context for
the `navigation` endpoint ❸ is set to a simple URL
(`https://dtsapi.org/specifications/context/2.0.json`). This value is
passed as a runtime parameter into the transformation and as a
`Config` object (which is ignored by the XSLT plugin).

For the collection endpoint ❷, the frame is set not to a URL by
`location` but to a complex JSON object, contained under
`document`. (`"member"` is part of the LSON-LD frame, and `@context`
is the LOD context.) This information is, too, passed as runtime
parameters and in a Config object. The SPARQL plugin uses the Config
object and the actual SPARQL query drops the runtime parameters.

See below for list of configuration options.

### Runtime Parameters Supplied to Transformation Calls

The following parameters are supplied to transformations on top of the
parameters that result from request parameters.

| Parameter          | Service | Endpoint   | JSON Path in `collection.json`                    | Type                 | Purpose                                                |
|:-------------------|:--------|:-----------|:--------------------------------------------------|:---------------------|:-------------------------------------------------------|
| `context-url`      | DTS     | collection | `/configuration/frames/(all|collection)/location` | string               | configure `@context` on a per-record basis             |
| `context-document` | DTS     | collection | `/configuration/frames/(all|collection)/document` | stringified JSON doc | dito for complex contexts, read with `fn:parse-json()` |
| `context-url`      | DTS     | navigation | `/configuration/frames/(all|navigation)/location` | string               | configure `@context` on a per-record basis             |
| `context-document` | DTS     | navigation | `/configuration/frames/(all|navigation)/document` | stringified JSON doc | dito for complex contexts, read with `fn:parse-json()` |

### Config properties Supplied to Transformation Calls

See [Config](../api/src/main/resources/openapi/seed-xc-openapi.yaml#/schemas/config).

| Config property           | Service | Endpoint   | JSON Path in `collection.json`                    | Type                 | Purpose                                    |
|:--------------------------|:--------|:-----------|:--------------------------------------------------|:---------------------|:-------------------------------------------|
| `config.context.location` | DTS     | collection | `/configuration/frames/(all|collection)/location` | string parsed as URI | configure frame on a per-record basis      |
| `config.context.document` | DTS     | collection | `/configuration/frames/(all|collection)/document` | JsonLD Frame         | dito for complex frames                    |
| `comfig.context.location` | DTS     | navigation | `/configuration/frames/(all|navigation)/location` | string parsed as URI | configure `@context` on a per-record basis |
| `config.context.document` | DTS     | navigation | `/configuration/frames/(all|navigation)/document` | JsonLD Context       | dito for complex contexts                  |

