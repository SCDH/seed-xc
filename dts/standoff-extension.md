# Standoff Extension

The purpose of the two *standoff* endpoints is to transform selectors
of [web annotations](https://www.w3.org/TR/annotation-model/) so that
they target the different representations of a resource provided by
the `document` endpoint. Selector transformations are implemented by
the [Selene Selection Engine](https://github.com/scdh/selene) and are
done using the exactly same transformation as is used for generating
the resource representation on the `document` endpoint.

| endpoint    | Verb | URI template                                                           | POST parameters                 |
|:------------|:-----|:-----------------------------------------------------------------------|:--------------------------------|
| oa/forward  | POST | `BASE_URL/FRONT/oa/forward/{resource}{?tree,ref,start,end,mediaType}`  | `annotations`, `frame`, `xpath` |
| oa/backward | POST | `BASE_URL/FRONT/oa/backward/{resource}{?tree,ref,start,end,mediaType}` | `annotations`, `frame`          |

**forward**: Rewrites web `annotations` on `resource` to annotations
on a representation provided by document endpoint.

**backward**: Rewrites web `annotations` on a representation provided by
document endpoint to annotations on original `resource`.

## Parameters

The *path* and *query* parameters are exactly the same as for the
`document` endpoint as they shape the representation of resource.

| parameter   | required | content type         | default                                                                                                | description                                                                                                   |
|:------------|:---------|:---------------------|--------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| annotations | Y        | any RDF content type |                                                                                                        | An RDF graph containing web annotation selectors                                                              |
| frame       | N        | application/json     | [anno-frame.json](src/main/resources/META-INF/resources/context/anno-frame.json)                       | JSON-LD [Frame](https://www.w3.org/TR/json-ld11-framing/) used when application/ld+json response is requested |
| xpath       | N        | string               | [`sel:to-element(.)`](https://github.com/SCDH/selene/blob/main/core/src/main/resources/xslt/xpath.xsl) | An XPath expression for generating XPathSelector values                                                       |
## Content Negotiation

Similar to the content type of the `annotations` POST parameter,
the content type of the response is determined by content negotiation
and can be any RDF serialization supported by Apache Jena.

## OpenAPI specs

The OpenAPI
[spec](https://github.com/SCDH/dts-openapi/blob/main/facade-openapi.yaml)
is in an extra repo. The spec can best be explored via
[petstore.swagger.io](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/SCDH/dts-openapi/refs/tags/0.6.0/standalone/facade-openapi.yaml).


## Examples

Data for the following examples is available at `samples/bible`. Annotations are in the `annotations` subfolder.

Mount the `samples/bible` folder into the docker image like so:

```shell
docker run --mount type=bind,src=$(realpath samples/bible),dst=/work/projects/bible -i --rm -p 8080:8080 scdh/distributed-test-services
```


The `document` endpoint gives access to the Book of John under
`http://localhost:8080/file/bible/document/John` and the delivered
TEI-XML looks like this:


```xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-model href="http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/tei_all.rng" type="application/xml" schematypens="http://relaxng.org/ns/structure/1.0"?>
<?xml-model href="http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/tei_all.rng" type="application/xml"
	schematypens="http://purl.oclc.org/dsdl/schematron"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0">
   <teiHeader>
      <fileDesc>
         <titleStmt>
            <title>Title</title>
         </titleStmt>
         <publicationStmt>
            <p>Publication Information</p>
         </publicationStmt>
         <sourceDesc>
            <p>Information about the source</p>
         </sourceDesc>
      </fileDesc>
      <encodingDesc>
         <refsDecl n="biblical2" default="true">
            <citeStructure unit="book" match="//body/lg" use="@n">
               <citeStructure unit="chapter" match="lg" use="@n" delim=":">
                  <citeStructure unit="verse" match="l" use="@n" delim=":"/>
               </citeStructure>
            </citeStructure>
         </refsDecl>
         <refsDecl n="wadm" default="false">
            <!-- see comments in WIKI article about alignment with WADM -->
            <citeStructure unit="book" match="//body/lg" use="@n">
               <citeData use="path(.)" property="https://w3id.org/dts/api#xpath"/>
               <citeStructure unit="chapter" match="lg" use="@n" delim=":">
                  <citeData use="path(.)" property="https://w3id.org/dts/api#xpath"/>
                  <citeStructure unit="verse" match="l" use="@n" delim=":">
                     <citeData use="path(.)" property="https://w3id.org/dts/api#xpath"/>
                  </citeStructure>
               </citeStructure>
            </citeStructure>
         </refsDecl>
         <!-- see comments in WIKI article about <pb/> -->
         <refsDecl n="page-milestones">
            <citeStructure unit="page" match="//body//pb" use="@n" delim="p."/>
         </refsDecl>
         <refsDecl n="page-content-by-intersection">
            <citeStructure unit="page"
               match="for $pb in //pb return (let $next:=$pb/following::pb[1] return outermost(($pb, $pb/following::node() intersect $next/preceding::node())))"
               use="@n" delim="p."/>
         </refsDecl>
         <refsDecl n="page-content-xquery-like">
            <citeStructure unit="page"
               match="for $pb in //pb return element {ab} {(let $next:=$pb/following::pb[1] return outermost(($pb, $pb/following::node() intersect $next/preceding::node())))}"
               use="@n" delim="p."/>
         </refsDecl>
         <refsDecl n="page-content-by-intersection-2">
            <citeStructure unit="page-beginning" match="//body//pb" use="@n" delim="p.">
               <citeStructure unit="page-content"
                  match="let $pb:=self::pb, $next:=$pb/following::pb[1] return (($pb, $pb/following::node() intersect $next/preceding::node()))"
                  use="''" delim=" content"/>
            </citeStructure>
         </refsDecl>
         <refsDecl n="page-level2-start-end">
            <citeStructure unit="page" match="//body//pb" use="@n" delim="p.">
               <citeStructure unit="page-start" match="self::pb"
                  use="''" delim=".start"/>
               <citeStructure unit="page-end" match="self::pb/following::pb[1]/preceding::node()[1]"
                  use="''" delim=".end"/>
            </citeStructure>
         </refsDecl>
         <refsDecl n="page-hateoas">
            <citeStructure unit="page" match="//body//pb" use="@n" delim="p.">
               <citeData use="'p.' || @n || '.start'" property="https://w3id.org/dts/api#startMember"/>
               <citeData use="'p.' || @n || '.end'" property="https://w3id.org/dts/api#endMember"/>
               <citeStructure unit="page-start" match="self::pb"
                  use="''" delim=".start"/>
               <citeStructure unit="page-end" match="(self::pb/following::pb[1]/preceding::node()[1], (//text//text())[last()])[1]"
                  use="''" delim=".end"/>
            </citeStructure>
         </refsDecl>
      </encodingDesc>
   </teiHeader>
   <text>
      <body>
         <pb n="1"/>
         <lg n="John">
            <head>The book of John</head>
            <lg n="1">
               <milestone unit="theme" xml:id="creation-start"/>
               <l n="1">In the beginning was the Word, and the Word was with God, and the Word was
                  God.</l>
               <l n="2">He was with God in the beginning.</l>
               <l n="3">Through him all things were made; without him nothing was made that has been
                  made.</l>
               <l n="4" xml:space="preserve">In him was life, and that life was the light<pb n="2"/> of all mankind.</l>
               <l n="5">The light shines in the darkness, and the darkness has not overcome it.</l>
               <milestone unit="theme" xml:id="creation-end"/>
               <milestone unit="theme" xml:id="john-start"/>
               <l n="6">6 There was a man sent from God whose name was John.</l>
               <milestone unit="theme" xml:id="john-end"/>
               <pb n="3"/>
               <l n="7">bla</l>
            </lg>
         </lg>
      </body>
   </text>
</TEI>
```

### Fraction

Further, the document endpoints delivers the following fractional
representation when called with these parameters:


```
http://localhost:8080/file/bible/document/John&start=John:1:3&end=John:1:5
```


```xml
<?xml version="1.0" encoding="UTF-8"?><TEI xmlns="http://www.tei-c.org/ns/1.0" xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing" xmlns:xs="http://www.w3.org/2001/XMLSchema" xml:base="John"><dts:wrapper xmlns:dts="https://w3id.org/api/dts#"><l n="3">Through him all things were made; without him nothing was made that has been
                  made.</l>
               <l n="4" xml:space="preserve">In him was life, and that life was the light<pb n="2"/> of all mankind.</l>
               <l n="5">The light shines in the darkness, and the darkness has not overcome it.</l></dts:wrapper></TEI>
```

#### forward

Now, consider a web
[annotation](../samples/bible/annotations/John.fw.json) targeting the
whole document, ranging from `rough him ...` in verse 1:3 to `... of`
in verse 1:4:


```json
{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "source": "http://localhost:8080/file/bible/document/John",
    "selector": {
      "type": "RangeSelector",
      "startSelector": {
        "type": "XPathSelector",
        "value": "/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:lg[1]/*:l[3]/text()[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        }
      },
      "endSelector": {
        "type": "XPathSelector",
          "value": "/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:lg[1]/*:l[4]/text()[2]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        }
      }
    }
  }
}
```

The selectors in this annotations target can be rewritten to selectors
targeting the fractional representation using the `oa/forward`
endpoint.

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John?end=John%3A1%3A5&start=John%3A1%3A3' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json'
```

Response:

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{https://w3id.org/api/dts#}wrapper[1]/Q{http://www.tei-c.org/ns/1.0}l[2]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=47"
        }
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{https://w3id.org/api/dts#}wrapper[1]/Q{http://www.tei-c.org/ns/1.0}l[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        }
      }
    },
    "source": "http://localhost:8080/file/bible/document/John?end=John:1:5&start=John:1:3"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```


Note, what has changed:

- The `source` property was changed to the URI of the fractional
  representation.
- The start selector's XPath component points to the first `l` element
  now, since the fractional representation starts with verse 1:3
- The end selector's XPath component points to the second `l` element
  now, since the fractional representation starts with verse 1:3 and
  1:4 is the second `l` here
- The end selector's char scheme component was rewritten, since the
  XPath component does not go down all the way to the text leaf, but
  stops at the deepest element level. This behaviour is due to the
  `xpath` POST which by default constructs a path expression down to
  the deepest element. So the RFC5147 character scheme component,
  which is an inter-character position, was recalculated to 47.
- The start selector's char scheme also went through this
  recalculation, since the path expression also ends at the deepest
  element position. But the new position equals the old position,
  since it was in the first text node.

The changes in the JSON property order is not significant and cannot
be determined.

#### backward

Provided, the last result selecting the same portion of the text in
the fraction 1:3-1:5 was stored as `John.bw.json`.

The `oa/backward` endpoint can be used to transform it back to an
annotation targeting the whole document:


```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/backward/John?end=John%3A1%3A5&start=John%3A1%3A3' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.bw.json;type=application/json'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[4]/text()[2]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        }
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[3]/text()[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        }
      }
    },
    "source": "http://localhost:8080/file/bible/document/John"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

Again, note what has changed:

- the `source` property
- the values of the XPathSelectors
- the values of the refining FragmentSelectors

The path expressions are not the same as the input
`John.fw.json`. They have fully qualified element names using the
Clark or extended QName notation with namespace names in curly braces!

The `oa/backward` cannot be parametrized to change the path
expressions via the REST API, since the output is considered a
project's ground standard. It can only be configured at the project
level in the DTS dataset.

The default is path expressions down to the text node.

#### user XPathSelectors

If you want a different ground notation for the set of annotations on a
DTS project you do not own, you can use the `oa/forward` forwarding
to the base representation (resource as is) and use the `xpath` POST
parameter. E.g., if the path expression should only be
`/TEI[1]/text[1]`, this can be achieved with `path(ancestor::*:text)`:

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json' \
  -F 'xpath=path(ancestor::*:text)'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=426"
        }
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=265"
        }
      }
    },
    "source": "http://localhost:8080/file/bible/document/John"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

The `xpath` parameter value must be an XPath that constructs a path
expression that selects exactly one node. Bad user input results in
error messages added to the RDF model:

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json' \
  -F 'xpath=/TEI/text'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:lg[1]/*:l[4]/text()[2]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        },
        "error": "normalizing XPath '/TEI/text' did not return exactly one item: returned 0 items"
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:lg[1]/*:l[3]/text()[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        },
        "error": "normalizing XPath '/TEI/text' did not return exactly one item: returned 0 items"
      }
    },
    "source": "http://localhost:8080/file/bible/document/John"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

#### blanked selectors

When forwarding to a fraction of a resource, selectors may point to
portions not part of the fraction. Imagine the annotation above
forwarded to only 1:4-1:5: The start selector points outside of the
representation then. In such a case, a selector is assigned the
`BlankedSelector` type (defined in the selene.jsonld
context):

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John?end=John%3A1%3A5&start=John%3A1%3A4' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{https://w3id.org/api/dts#}wrapper[1]/Q{http://www.tei-c.org/ns/1.0}l[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=47"
        }
      },
      "startSelector": {
        "type": [
          "BlankedSelector",
          "XPathSelector"
        ],
        "refinedBy": {
          "type": [
            "BlankedSelector",
            "FragmentSelector"
          ],
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147"
        }
      }
    },
    "source": "http://localhost:8080/file/bible/document/John?end=John:1:5&start=John:1:4"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

### HTML

Transforming annotations so that they target an HTML representation is
similar as above. It is the transformation that generates HTML output
for the document endpoint and also transforms the selectors. The only
difference is, that the selectors have no namespaces (empty
namespace).

### plain text

The document will provides the following plain text output:

```shell
curl -X 'GET' \
  'http://localhost:8080/file/bible/document/John?mediaType=text%2Fplain' \
  -H 'accept: application/tei+xml'
```

```txt
 
      
         
         
The book of John
 
 
 
In the beginning was the Word, and the Word was with God, and the Word was
                  God.
He was with God in the beginning.
Through him all things were made; without him nothing was made that has been
                  made.
In him was life, and that life was the light of all mankind.
The light shines in the darkness, and the darkness has not overcome it.
6 There was a man sent from God whose name was John.
bla
      
   
```

#### forward

With the same `John.fw.json` annotation as above. For

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John?mediaType=text%2Fplain' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json'
```

we get

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "FragmentSelector",
        "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
        "value": "char=328"
      },
      "startSelector": {
        "type": "FragmentSelector",
        "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
        "value": "char=182"
      }
    },
    "source": "http://localhost:8080/file/bible/document/John?mediaType=text/plain"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

What has changed:

- The `source` property hat the URI of the according document.
- The start and end selectors are rewritten to a selector type
  appropriate for targeting plain text: RFC5147 character
  schemes. Their values are calculated to the correct inter-character
  positions. (If you want to count characters: Watch out for white
  space characters!)

#### forward to a plain text fraction

Similar to other content types, selectors are also recalculated to
target plain text fractions. Again, let's make a plain text fraction from 1:3-1:5:

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/forward/John?end=John%3A1%3A5&mediaType=text%2Fplain&start=John%3A1%3A3' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.fw.json;type=application/json'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "FragmentSelector",
        "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
        "value": "char=165"
      },
      "startSelector": {
        "type": "FragmentSelector",
        "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
        "value": "char=3"
      }
    },
    "source": "http://localhost:8080/file/bible/document/John?end=John:1:5&mediaType=text/plain&start=John:1:3"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

#### backward

The `oa/backward` endpoint works for plain text as well.

```shell
curl -X 'POST' \
  'http://localhost:8080/file/bible/oa/backward/John?end=John%3A1%3A5&mediaType=text%2Fplain&start=John%3A1%3A3' \
  -H 'accept: application/ld+json' \
  -H 'Content-Type: multipart/form-data' \
  -F 'annotations=@John.bw.txt.json;type=application/json'
```

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[4]/text()[2]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        }
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[3]/text()[1]",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        }
      }
    },
    "source": "http://localhost:8080/file/bible/document/John"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```

### non-matching `source`

The `source` property of an annotation must equal the URI of the
document endpoint for the resource and in its
representation. Annotations with a non-matching `source` will simply
pass the transformation untouched. This is intended.

It must be considered a feature in the general context of linked open
data.

It's also a practical feature, since it allows to process a big graph
of annotations on several sources with one request, updating only
relevant annotations. `source` is a filter that determines which
annotations to process.


### bad selectors

When a selector cannot be processed, the endpoint does not respond
with a error status code, but writes the it back to the RDF
model. This is a feature, since otherwise a single broken selector
could render a whole graph of annotations non-processable.

```json
{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "source": "http://localhost:8080/file/bible/document/John",
    "selector": {
      "type": "RangeSelector",
      "startSelector": {
        "type": "XPathSelector",
        "value": "/world/sun/earth/football",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        }
      },
      "endSelector": {
        "type": "XPathSelector",
          "value": "/vfl/s04/bvb",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        }
      }
    }
  }
}
```

Will result in:

```json
{
  "id": "https://annotations.example.com/samples/p1.1",
  "type": "Annotation",
  "body": {
    "source": "https://global.scdh.uni-muenster.de/comment/1"
  },
  "target": {
    "selector": {
      "type": "RangeSelector",
      "endSelector": {
        "type": "XPathSelector",
        "value": "/vfl/s04/bvb",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=3"
        },
        "error": "XPath '/vfl/s04/bvb' does not select exactly one node in XdmValueResource"
      },
      "startSelector": {
        "type": "XPathSelector",
        "value": "/world/sun/earth/football",
        "refinedBy": {
          "type": "FragmentSelector",
          "conformsTo": "http://tools.ietf.org/rfc/rfc5147",
          "value": "char=2"
        },
        "error": "XPath '/world/sun/earth/football' does not select exactly one node in XdmValueResource"
      }
    },
    "source": "http://localhost:8080/file/bible/document/John"
  },
  "@context": [
    "https://www.w3.org/ns/anno.jsonld",
    "https://raw.githubusercontent.com/SCDH/seed-xc/refs/heads/main/dts/src/main/resources/META-INF/resources/context/selene.jsonld"
  ]
}
```
