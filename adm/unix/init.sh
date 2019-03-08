#! /usr/bin/env bash
#
# init.sh
#
#   Initializes the sedona environment for a UNIX host.
#
# Author: Matthew Giannini
# Creation: 10 Dec 08
#

# Setup sedona_home. This is the directory that contains adm/, src/, bin/, etc...
# use the sedona source code directory as the home 

DIR=$(dirname "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )")
export sedona_home=$(dirname $DIR)

# path
export PATH=$PATH:$sedona_home/bin:$sedona_home/adm:$sedona_home/adm/unix

# python environment
export PYTHONPATH=$PYTHONPATH:$sedona_home/adm:$sedona_home/adm/unix

# java environment - use JAVA_HOME from environment if set, otherwise hard code it
java_home=$JAVA_HOME
[ -z "$java_home" ] && java_home=/usr/lib/jvm/$(ls /usr/lib/jvm | head -n1)
export java_home

# set up SVM_PLATFORM for Darwin platform 
platform=$(python3 -c 'import sys; print(sys.platform)')
[ "$platform" == 'darwin' ] && export SVM_PLATFORM=$sedona_home/platforms/src/generic/unix/generic-darwin.xml && echo "set SVM_PLATFORM to $SVM_PLATFORM"

# check to make sure that programs we need are in the path
for p in gcc python3
do
  if ! which $p > /dev/null
  then
    echo "Sedona Linux Env ERROR: $p is not in the PATH"
  fi
done

# Ensure permissions are correct for adm python scripts
find $sedona_home/adm -name "*.py" -exec chmod 755 '{}' \; 2> /dev/null
find $sedona_home/adm -name "*.sh" -exec chmod 755 '{}' \; 2> /dev/null
find $sedona_home/bin -name "*.sh" -exec chmod 755 '{}' \; 2> /dev/null

# aliases
alias makeunixvm='makeunixvm.py'
alias makewinvm='makewinvm.py'
alias makedev='makedev.py'
alias makedocs='makedocs.py'
alias makedist='makedist.py'
alias makesedona='makesedona.py'
alias makesedonac='makesedonac.py'
alias makesedonacert='makesedonacert.py'
alias platArchive='platArchive.py'
alias platformdb='platformdb.py'
alias sedonac='sedonac.sh'
alias sedonacert='sedonacert.sh'
alias jsvm='jsvm.sh'

# functions

function scodegen
{
  rt=$java_home/jre/lib/rt.jar
  jikes -classpath $rt $sedona_home/adm/SCodeGen.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "java" $sedona_home/adm/scode.txt $sedona_home/adm/scode.java $sedona_home/src/sedonac/src/sedonac/scode/SCode.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "h" $sedona_home/adm/scode.txt $sedona_home/adm/scode.h $sedona_home/src/vm/scode.h
  rm -rf $sedona_home/adm/*.class
}


function runapp
{
  appname=${1:-platUnix}
  svm scode/$appname.scode apps/$appname.sab
}

function makeapp
{
  appname=${1:-platUnix}
  sedonac apps/$appname.sax && sedonac scode/$appname.xml && echo "Done"
}
