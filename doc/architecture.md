# Architecture

## Dependency Inversion

SEED XC follows the dependency inversion principle (DIP) and thus has
a clean architectural boundary between high level and low level
components. (Cf. Martin 2018, pp.87)

![UML](architecture.svg)

There are two very fundamental types, `ResourceProvider` and
`Transformation`. The first allows us to get a resource from some kind
of storage or persistence location, while the latter is some process
that takes the resource as input and returns some kind of transformed
output.  These two types are abstract and defined as interfaces. They
don't know about file system, database or web as storage and don't
know about XSLT, XQuery, etc. as transformations. All these details
are implementation details, which may be realized in a plugin: There
is a `FileSystemResourceProvider` and a `WWWResourceProvider` and
resource provider implementations that provider access to an RDMS or
to Invenio RDM; there may be transformations for processing with XSLT,
XQuery or SPARQL. None of the components above the gray architectural
boundary depends on these plugins, but the plugins depend on the
high-level types. That's dependency inversion. It allows, to add
plugins for new types of transformations or new resource providers for
persistence layers, while not re-compiling the high level components.

## High Level


## Low Level
