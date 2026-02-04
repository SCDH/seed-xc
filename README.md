# SEED XC - XSLT/XQuery Compilation Service

[OpenAPI specification](api/src/main/resources/openapi/seed-xc-openapi.yaml)


## Getting Started

Build:

```shell
./mvnw package
```

Run dev server:

```shell
./mvnw quarkus:dev
```

Dev-Server will listen on http://localhost:8080


## Exporting compiled XSLT Stylesheets

The service can return compiled XSLT stylesheets in SEF format,
provided, that the instance of the service has a Saxon Enterprise
Edition License.

Copy this license to `/etc/licenses/saxon-license.lic`.

Then tell the instance to use the EE Saxon configuration that uses
this license at startup.

dev server:

```shell
./mvnw -Dde.ulbms.scdh.seed.xc.xslt.ConfiguredProcessor.saxonConfigLocations=$(realpath service/src/main/resources/saxon-config-ee.xml) quarkus:dev
```


curl example with sample zip from this project:

```shell
curl -X 'POST' \
	 'http://localhost:8080/xslc/zip/xsl%2Fid.xsl' \
	 -H 'accept: */*' \
	 -H 'Content-Type: application/zip' \
	 --data-binary '@harden/src/test/resources/xsl.zip' \
	 -i
```
