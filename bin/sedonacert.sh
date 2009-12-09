#!/bin/bash
#
#   sedonacert.sh
#
#   Script to run sedonacert java program on Unix.
#
# Author: Brian Frank
# Creation: 16 Jun 09
#

which java > /dev/null
if [[ $? != 0 ]]
then
  echo "java is not in the PATH"
  return 1
fi

# Get full path to sedonac script
sedonacert_path=`dirname $(cd ${0%/*} && echo $PWD/${0##*/})`

# Determine sedona home by pulling off trailing /bin
sedona_home=${sedonacert_path%/bin}

java -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedonacert/Main "$@"

