#!/bin/bash
set -o errexit

CUSTOM_TRANSFORMATIONS=/tmp/config.tar.gz.b64

case "$1" in
    sh|bash)
	# if sh first in CMD, run following commands in shell
        set -- "$@"
    ;;
    *)
        if [ -e "$CUSTOM_TRANSFORMATIONS"]; then
	        # unpack the transformation resources which are mounted as configMap
	        base64 -d $CUSTOM_TRANSFORMATIONS | tar -C /work/resources -zx --skip-old-files
	      fi
        # run the server with the parameters given as CMD (set $0 to run it)
        set -- /work/application "$@"
    ;;
esac

exec "$@"
