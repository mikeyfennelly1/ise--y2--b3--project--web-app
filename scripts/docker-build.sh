#!/bin/bash

################################################
# SETUP
################################################
OS=$(uname)
if [[ "$OS" == "Darwin" ]]; then
	# OSX uses BSD readlink
	BASEDIR="$(dirname "$0")"
else
	BASEDIR=$(readlink -e "$(dirname "$0")/")
fi
cd "${BASEDIR}/.."

set -eou pipefail

pwd

./gradlew build
cp ./app/build/libs/app-0.0.1-SNAPSHOT.jar ./app.jar
IMAGE_NAME="mikeyfennelly/cotcsubscriber:latest"
docker build -t "${IMAGE_NAME}" .

docker login
docker push "${IMAGE_NAME}"
