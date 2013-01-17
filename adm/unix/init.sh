#!/usr/bin/bash
#
# init.sh
#
#   Initializes the sedona environment for a UNIX host.
#
# Author: Matthew Giannini
# Creation: 10 Dec 08
#

# Setup sedona_home. This is the directory that contains adm/, src/, bin/, etc...
# This assumes you have set up a symbolic link in your home directory called
# "sedonadev" that points to the repository you want to be working with.
# You can always change this to be the explicit path.This

export sedona_home=~/sedonadev

# path
export PATH=$PATH:$sedona_home/bin:$sedona_home/adm:$sedona_home/adm/unix

# python environment
export PYTHONPATH=$PYTHONPATH:$sedona_home/adm

# java environment - use JAVA_HOME from environment if set, otherwise hard code it
java_home=$JAVA_HOME
[ -z "$java_home" ] && java_home=/usr/lib/jvm/java-6-sun
export java_home

# check to make sure that programs we need are in the path
for p in jikes gcc python
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
alias makeunixdev='makeunixdev.py'
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
  jikes +E -classpath $rt $sedona_home/adm/SCodeGen.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "java" $sedona_home/adm/scode.txt $sedona_home/adm/scode.java $sedona_home/src/sedonac/src/sedonac/scode/SCode.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "h" $sedona_home/adm/scode.txt $sedona_home/adm/scode.h $sedona_home/src/vm/scode.h
  rm -rf $sedona_home/adm/*.class
}
