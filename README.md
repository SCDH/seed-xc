# SEED XC - XSLT/XQuery Compilation Service

[OpenAPI specification](api/src/main/resources/openapi/seed-xc-openapi.yaml)


## Getting Started

The following maven command is required in the first place and after
each call of the `clean` target. It will download thefor the service.

```shell
./mvnw -Popenapi generate-sources
```

Build:

```shell
./mvnw package
```

Run dev server:

```shell
./mvnw quarkus:dev
```

Dev-Server will listen on http://localhost:8080


