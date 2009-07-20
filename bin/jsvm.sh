#!/bin/bash
#
#   jsvm.sh
#
#   Script to run java vm in UNIX.
#
# Author: Matthew Giannini
# Creation: 21 Apr 09
#

which java > /dev/null
if [[ $? != 0 ]]
then
  echo "java is not in the PATH"
  return 1
fi

# Get full path to jsvm script
jsvm_path=`dirname $(cd ${0%/*} && echo $PWD/${0##*/})`

# Determine sedona home by pulling off trailing /bin
sedona_home=${jsvm_path%/bin}

java -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedona/vm/Jsvm "$@"

