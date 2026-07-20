# SEED XML Transformer

Once after cloning and after each cleanup:

```shell
./mvnw -Pdownload-openapi generate-sources
```

Then building all components:

```shell
./mvnw package
```

To run one of the services in dev mode, use `-P` switch to select a
service profile.

XML Transformer:

```shell
./mvnw -Ptransformer quarkus:dev
```

Dev-Server will listen on http://localhost:8080

