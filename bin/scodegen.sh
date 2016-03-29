#!/bin/bash
#
#   sedonac.sh
#
#   Script to run scodegen java program on POSIX.
#
# Author: Nitin Lamba
# Creation: 29 Oct 2015
#

which java > /dev/null
if [[ $? != 0 ]]
then
  echo "java is not in the PATH"
  return 1
fi

# Get full path to scodegen script
scode_path=`dirname $(cd ${0%/*} && echo $PWD/${0##*/})`

# Determine sedona home by pulling off trailing /bin
sedona_home=${scode_path%/bin}

# Go to adm/scode
pushd ${sedona_home}/adm/scode

echo "Generating SCodes for Java..."
java -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedonac.util.SCodeGen \
  "java" scode.txt scode.java $sedona_home/src/sedonac/src/sedonac/scode/SCode.java
echo "Generating SCodes header for Native..."
java -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedonac.util.SCodeGen \
  "h" scode.txt scode.h $sedona_home/src/vm/scode.h

popd
