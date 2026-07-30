# Development

[OpenAPI specs](https://github.com/SCDH/dts-openapi/blob/main/facade-openapi.yaml)


## Dev Server

Instead of using the official docker image, you can clone the project
and start a development server.

All commands must be run in the root directory of SEED XC, **not**
from the `dts` subfolder.

1. Build:

```shell
./mvnw -Pdownload-openapi generate-sources
./mvnw generate-sources package
```

Whenever you want to get a new version of the OpenAPI specs or XSLT
run `./mvnw clean` first and the above commands then.

2. Run dev server

```shell
./mvnw -Pdts quarkus:dev
```

The dev server will be available on
[http://localhost:8080](http://localhost:8080). (See third-last line
of output about the "Quarkus Main Thread".)

Swagger UI is available under
[http://localhost:8080/q/dev-ui/quarkus-smallrye-openapi/swagger-ui](http://localhost:8080/q/dev-ui/quarkus-smallrye-openapi/swagger-ui).

Per default, the service serves files from the [`samples`](../samples)
directory. Use `file` as value for the **provider** path parameter,
and `bible` for **location**.

To serve TEI files from an other local directory, use the
`seed-dts.filesystem` property like so, where `PATH` must be an
absolute path.

```shell
./mvnw -Dseed-dts.filesystem=PATH -Pdts quarkus:dev
```

Have a look at
[`src/main/resources/application.properties`](src/main/resources/application.properties)
for more config options.

## Native Executable and Docker Image

A native executable is a build artifact, that can be run without a
Java virtual machine. It is simply a linux executable, compiled from
Java by a special compiler called GraalVM. And, since it is statically
linked for a minimal attack surface, its size is huge: about
170MB. Everything is in there compiled to native machine code: the
Saxon XSLT processor, the Apache Jena SPARQL engine, the web server,
the APIs.

If you want to build the native executable locally on your system: Be
aware, that the compilation takes some amount of time: about 10
Minutes on my Intel i7 with 32GB RAM.

Here is what to do: Docker is required, because the build uses GraalVM
and OpenJDK from a docker container:

```shell
./mvnw -Pdownload-openapi generate-sources
./mvnw -Ddts-native install
```

This produces `seed-dts-VERSION-runner` in `dts/target/`. It has
properties set for operating in a container image. So let's build it:

```shell
docker build -f dts/src/main/docker/Dockerfile.native -t scdh/dts-testing .
```

The service starts up lightning fast. Just call the native executable:

```shell
docker run -i --rm -p 8080:8080 scdh/dts-testing
```

Swagger UI is available at http://localhost:8080/q/swagger-ui

You can your own files into `/work/projects/`. Have a look at the docs
for [customizing the
transformations](../doc/dts.md#customizing-transformations).

If you want to look, what's inside the container, do ` docker run -it
scdh/dts-testing bash -c "ls -l /"`.

A container image built this way differs from the official container
image available on
[docker.io](https://hub.docker.com/r/scdh/distributed-test-services):
It's based on a minimal RedHat UBI image with
[Dockerfile.native](src/main/docker/Dockerfile.native), so it has a
package manager (microdnf). The official image is based on a micro
image and does not have a package manager, in order to minimize its
attack surface. This is much more complicated to build, in fact with
[`buildah`](buildah_native_micro.sh) instead of `docker`, which is
done on a Gitlab runner on Münster's IT infrastructure.

## Testing with cURL

```shell
curl -X 'GET' \
  'http://localhost:8080/file/bible/entry' \
  -H 'accept: application/ld+json'
```
