# Security

## Security Issues

If you find security issues, please don't use the issue tracker, but
send a mail to `scdh@uni-muenster.de` or one of the main developers.

## Security Risks

Running XSLT and XQuery on a web service requires extra security
measures, because of the risks that these languages entail.

These languages have features, that allow security exploits, if no
counter measures are taken.

### `<xsl:result-document>`

Writing to the local file system with `<xsl:result-document>` is
strictly denied. In Saxon, it's not possible to disable
`<xsl:result-document>`. But the goal is achieved by setting a denying
resolver for the XSL transformer.

### No access to Java

Calling Java functions from XSLT or XQuery is denied.

### Controlling Access to the Local Filesystem

The transformation resources must be present on the local file
system. Thus, access to the local file system cannot be absolutely
denied. However, it is controlled, which paths are accessible.

There's also a distinction for access control during compile time at
service startup and during request processing during later runtime.

The compiler accesses the file system access through the
`RestrictedResourceResolver` (see [architecture](architecture.md),
which allows access to a certain path and disallows the rest. The
resolver does not delegate request it cannot resolve.

Since accessing static assets (e.g. CSS or JS files, i18n translation
vocabularies, ...) during runtime must be possible, the compiled
transformation has access through the resource provider *and* through
the `RestrictedResourceResolver`. Thus, it's theoretically possible to
read out the transformation resources. We do not consider this a
security issue, because we use publicly available transformation
resources (XSLT stylesheets etc.) only and transformation
configurations are accessible through the API.

### `<xsl:evaluate>`

The `<xsl:evaluate>` element allows to evaluate arbitrary XPath
expressions. This has some risks:

1. Access to the local file system through XPath functions like
   `fn:doc(...)` or `fn:unparsed-text(...)`: Since the above measures
   for controlling access also contains these functions, this issue is
   already solved.
2. Arbitrary calculations like crypto mining: This can denied by
   setting time limits for transformations.

`<xsl:evaluate>` can be disabled globally for the whole service by
setting the Saxon configuration file.

## General Security Measures

### Compilation to Native Executable

The Quarkus service is compiled to a native Linux executable with
GraalVM. So, no Java Virtual Machine is required to run the
service. This feature drastically reduces the attack vectors.

### Micro Images

Containerization of the native executable based on a micro Linux image
is the best setup you can get. Not even a package manager is in it.
