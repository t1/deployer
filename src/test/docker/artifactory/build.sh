#!/usr/bin/env bash

cd "$(dirname "$0")"

mvn clean install

docker build --tag=artifactory-mock .
