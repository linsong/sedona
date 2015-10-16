#!/bin/bash
# Credits
set -e -x -u

BASE_SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

export DOCKER_ENV_VERSION="0.1"
export BASE_IMAGE_NAME="sedona/base:${DOCKER_ENV_VERSION}"

pushd ${BASE_SCRIPT_DIR}
docker build --rm=true -t ${BASE_IMAGE_NAME} .
popd
