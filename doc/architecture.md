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
don't know about the file system, a database or the web as storage for
resources and they don't know about XSLT, XQuery, etc. as
transformations. Handling such things is implementation details, which
may be realized in a plugin: There is a `FileSystemResourceProvider`
for allowing access to parts of the local file system or a
`WWWResourceProvider` with white and blacklisted URL patterns, or
plugins that provide access to an RDBMS or to Invenio RDM; there may
be transformations for processing with XSLT, XQuery or SPARQL. None of
the components above the gray architectural boundary depends on such
details, but the plugins depend on the high-level types as they
implement them. That's dependency inversion. It allows, to add plugins
for new types of transformations or new resource providers for
persistence layers, while not re-compiling the high level components.

## High Level

### Resource Provider

### Transformation


## Low Level
