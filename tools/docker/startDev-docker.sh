#!/bin/bash
#
# Copyright 2015 XXX. All Rights Reserved.
# Licensed under the Academic Free License version 3.0
#
# History:
#    2015 Oct 15   Divisuals   Initial script 
#

set -e -x -u

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source ${SCRIPT_DIR}/makeDev-docker.sh

IMAGE_NAME="sedona/base:${DOCKER_ENV_VERSION}"

if [ "$(uname -s)" == "Linux" ]; then
  USER_NAME=${SUDO_USER:=$USER}
  USER_ID=$(id -u "${USER_NAME}")
  GROUP_ID=$(id -g "${USER_NAME}")
else # boot2docker uid and gid
  USER_NAME=$USER
  USER_ID=1000
  GROUP_ID=50
fi

docker build -t "${IMAGE_NAME}-${USER_NAME}" - <<UserSpecificDocker
FROM ${IMAGE_NAME}
RUN groupadd --non-unique -g ${GROUP_ID} ${USER_NAME} \
 && useradd -g ${GROUP_ID} -u ${USER_ID} -k /root -m ${USER_NAME}
ENV HOME /home/${USER_NAME}
# SOXPORT 1876
# WEBSERVER 8080
EXPOSE  8080 1876/udp
UserSpecificDocker

# Go to root
pushd ${SCRIPT_DIR}/../..

docker run -it \
  --rm=true \
  -p 8080:8080 \
  -p 1876:1876/udp \
  -w "/home/${USER_NAME}/sedonadev" \
  -u "${USER_NAME}" \
  -v "$PWD:/home/${USER_NAME}/sedonadev" \
  -v "${SCRIPT_DIR}/.bashrc:/home/${USER_NAME}/.bashrc" \
  --name sedonaDev \
  ${IMAGE_NAME}-${USER_NAME} \
  bash

popd
