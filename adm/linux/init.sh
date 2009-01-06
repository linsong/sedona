#!/usr/bin/bash
#
# init.sh
#
#   Initializes the sedona environment for Linux
#
# Author: Matthew Giannini
# Creation: 10 Dec 08
#

# Sedona directories
# Sedona root is the directory that contains pub/, tridium/ and www/
export sedona_root=/usr/local/sedona

export sedona_home=$sedona_root/pub
export sedona_tridium=$sedona_root/tridium
export sedona_www=$sedona_root/www

# path
export PATH=$PATH:$sedona_home/bin:$sedona_home/adm:$sedona_tridium/adm:$sedona_www/bin
export PATH=$PATH:$sedona_home/adm/linux

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

# aliases
alias makelinuxvm='makelinuxvm.py'
alias makesedona='makesedona.py'
alias makesedonac='makesedonac.py'
alias sedonac='sedonac.sh'

# functions

function scodegen
{
  rt=$java_home/jre/lib/rt.jar
  jikes +E -classpath $rt $sedona_home/adm/SCodeGen.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "java" $sedona_home/adm/scode.txt $sedona_home/adm/scode.java $sedona_home/src/sedonac/src/sedonac/scode/SCode.java
  $java_home/bin/java -cp $sedona_home/adm SCodeGen "h" $sedona_home/adm/scode.txt $sedona_home/adm/scode.h $sedona_home/src/vm/scode.h
  rm -rf $sedona_home/adm/*.class
}

#function debugs()
#{
#  $java_home/bin/java -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Dsedona.home=$sedona_home -cp "$sedona_home/lib/*" sedonac/Main "$@"
#}
#

#function cleanSedona()
#{
#  # TODO make this a python script
#  echo "Cleaning c files..."
#  rm -rfv $sedona_home/temp/linux/*.[ch]
#
#  echo "Cleaning kits..."
#  rm -rfv $sedona_home/kits/*
#
#  echo "Removing sedona.jar and sedonac.jar..."
#  rm -rfv $sedona_home/lib/*.jar
#
#  echo "Removing svm..."
#  rm -rfv $sedona_home/bin/svm
#
#  echo "Done."
#  
#}
#
#function buildSedona()
#{
#  cleanSedona
#  python $sedona_home/adm/makesedona.py
#  python $sedona_home/adm/makesedonac.py
#  for i in sys inet
#  do
#    sedonac $sedona_home/src/$i
#  done
#  makevm
#}
