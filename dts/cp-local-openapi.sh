#!/bin/sh

OPENAPI_HOME=$(realpath $(dirname $0)/../../../apis/dts-openapi-specs)

target="$(realpath $(dirname $0))/target"

echo "Coping DTS Transformation from $OPENAPI_HOME to $target"

mkdir -p $target

cp -v $OPENAPI_HOME/standalone/facade-openapi.yaml $target/
