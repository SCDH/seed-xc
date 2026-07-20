# Configuration

A SEED XC service, be it DTS or the XML Transformer, can run any
number of transformations. Since XSLT, or XQuery transformations are
programs in a Turing complete language, there's a [security](security)
risk, when they wouldn't be controlled by the operators of the
service. The current mode of control: The transformations have to be
present at service startup and are mounted into the container.

The mounted resources have to encompass the actual transformation
scripts (e.g. XSLT stylesheets) and a YAML configuration file that
tells the service, how to compile these scripts to transformations.

Both, the transformation scripts and the YAML config file have to be
bundled in a gzipped tar-ball which has to be base64 encoded after
compression.

It has to be mounted into `/tmp/config.tar.gz.b64` of the
container. The service will
[check](../dts/src/main/docker/entrypoint.sh), if this file is present
at start up and unpack it to `/work/resources/` before it fires the
service.

The base 64 encoding makes it possible to use a kubernetes config map
for passing in the resource bundle.

Here's how to create the required file:

```shell
tar -czf config.tar.gz PATH_TO_TRANSFORMATIONS_AND_CONFIG
base64 config.tar.gz > config.tar.gz.b64
```

Here's how you mount the file into the container using docker:

```shell
docker run \
	   --mount type=bind,source=$(realpath config.tar.gz.b64),target=/tmp/config.tar.gz.b64 \
	   scdh/distributed-text-services
```

This uses `$(realpath ...)` since the source path must be an absolute
path. Run `realpath config.tar.gz.b64` to understand what is
happening.


## YAML Config File

This configuring transformations must be passed in as a YAML
configuration file. YAML was choosen, because it can simply be written
by hand or be generated automatically. And configurations can be
simple merged with low level tools like `cat`.

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
