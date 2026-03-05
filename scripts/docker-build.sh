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

pwd

./gradlew build
cp ./app/build/libs/app-0.0.1-SNAPSHOT.jar ./app.jar
IMAGE_NAME="mikeyfennelly/ise-y2-b3-project-web-app:dev-$(date "+%Y%m%d%H%M%S")"
docker build -t "${IMAGE_NAME}" .

docker login
docker push "${IMAGE_NAME}"
