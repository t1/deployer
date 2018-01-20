#!/usr/bin/env bash

cd "$(dirname "$0")"
set -e

export DOCKER_HUB_TAG=rdohna/artifactory-mock

echo "------------------- build jar"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/1.8.x/Contents/Home"
mvn clean install

echo "------------------- build image"
docker build --tag=${DOCKER_HUB_TAG} .

echo "------------------- push image"
docker push ${DOCKER_HUB_TAG}
