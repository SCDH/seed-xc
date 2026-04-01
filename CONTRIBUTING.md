# Contributing

## Code formatting

This project uses [`spotless`](https://github.com/diffplug/spotless)
and the [Palantir Java
Format](https://github.com/palantir/palantir-java-format) for
formatting Java code and Maven POMs.

Command line:

Check formatting:

```shell
./mvnw spotless:check
```

Apply formatting rules:

```shell
./mvnw spotless:apply
```
