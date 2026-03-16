# DTS

This is an implementation of the some of the API endpoints specified
for [distributed text services](https://dtsapi.org/specifications/)
(DTS):

- navigation
- document

[OpenAPI specs](https://zivgitlab.uni-muenster.de/SCDH/apis/dts-openapi-specs/-/blob/main/facade-openapi.yaml?ref_type=heads)

The DTS business logic is implemented in XSLT an can be found on
SCDH's github: [DTS
Transformations](https://github.com/scdh/dts-transformations). These
packages are used by the service at hand.

## Dev Server

All commands must be run in the root directory of SEED XC, **not**
from the `dts` subfolder.

1. Build:

```shell
./mvnw generate-sources package
```

2. Run dev server

```shell
./mvnw -Pdts quarkus:dev
```
