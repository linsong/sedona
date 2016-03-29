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
[ -z "$java_home" ] && java_home=/usr/lib/jvm/java-6-sun
export java_home

# check to make sure that programs we need are in the path
for p in gcc python
do
  if ! which $p > /dev/null
  then
    echo "Sedona Linux Env ERROR: $p is not in the PATH"
  fi
done

# Ensure permissions are correct for adm python scripts
find $sedona_home/adm -name "*.py" -exec chmod 755 '{}' \; 2> /dev/null
find $sedona_home/adm/unix -name "*.py" -exec chmod 755 '{}' \; 2> /dev/null
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
alias soxClient='soxClient.sh'
alias sCodeGen='scodegen.sh'
