#!/bin/bash
#
#   sedonac.sh
#
#   Script to run sedonac java program on Unix.
#
# Author: Matthew Giannini
# Creation: 12 Dec 08
#

which java > /dev/null
if [[ $? != 0 ]]
then
  echo "java is not in the PATH"
  return 1
fi

# Get full path to sedonac script
sedonac_path=`dirname $(cd ${0%/*} && echo $PWD/${0##*/})`

# Determine sedona home by pulling off trailing /bin
sedona_home=${sedonac_path%/bin}

java -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedonac/Main "$@"

