#!/usr/bin/env bash

# Note: The ID of the working container is stored to ./buildah-container-id

# USAGE EXAMPLE:
# ./buildah_native_micro.sh
# ctr=$(< buildah-container-id)
# echo "making image from container $ctr"
# img=$(buildah commit $ctr $CI_REGISTRY_IMAGE:$IMAGE_TAG)
# buildah login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
# buildah push $img docker://$CI_REGISTRY_IMAGE:$IMAGE_TAG
# buildah logout $CI_REGISTRY


FROM_IMAGE=quay.io/quarkus/quarkus-micro-image:2.0

UBI_VERSION=8

APPLICATION='target/*-runner'

TRNSFORMATIONS='target/dependencies/'

# create a new working container based on FROM_IMAGE
ctr=$(buildah from $FROM_IMAGE)
mnt=$(buildah mount $ctr)

# install tar
# see https://www.redhat.com/en/blog/introduction-ubi-micro
#
# version in --releasever must match UBI base image of FROM_IMAGE
yum install --installroot $mnt --releasever $UBI_VERSION --setopt install_weak_deps=false --nodocs -y tar gzip

# cleanup after using yum package manager
yum clean all --installroot $mnt


# setup for quarkus
# create /work and make it the working directory for the container
buildah config --workingdir='/work' $ctr

# create required folder structure
buildah run $ctr -- mkdir -p /work/resources
buildah run $ctr -- mkdir -p /work/projects

# set owner and permissions of workdir
buildah run $ctr -- chown -R 1001:root /work
buildah run $ctr -- chmod -R g+rwX /work

# copy native executable to /work/application
buildah add --chown 1001:root $ctr $APPLICATION '/work/application'

# copy default transformations
buildah add --chown 1001:root $ctr $TRNSFORMATIONS '/work/resources/'

# expose port
buildah config --port 8080 $ctr

# set user ID
buildah config --user 1001 $ctr


# configure container to start application

# 1. copy script for advanced entry point
buildah add --chown 1001:root --chmod 555 $ctr 'src/main/docker/entrypoint.sh' '/work/entrypoint.sh'

# 2. set entrypoint and cmd
# array format allowed but undocumented,
# see https://github.com/containers/buildah/commit/154417fe74a3be9c8cf82ed4b076384d208df678
buildah config --entrypoint '[ "/work/entrypoint.sh" ]' $ctr
buildah config --cmd '[ "-Dquarkus.http.host=0.0.0.0" ]' $ctr


# mount not needed for further steps
buildah umount $ctr


# pass container ID to subsequent consumers
echo $ctr > buildah-container-id
