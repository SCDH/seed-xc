#!/bin/sh

DTS_TRANS_HOME=$(realpath $(dirname $0)/../../../selectors/selection-engine/core)

target="$(realpath $(dirname $0))/target/dependencies"

echo "Coping DTS Transformation from $DTS_TRANS_HOME to $target"

mkdir -p $target

for tarball in $DTS_TRANS_HOME/target/selection-engine-core-*-xslt.tar.gz; do tar -zxf $tarball -C $target; done
