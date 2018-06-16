#! /usr/bin/env python
#
# makedist.py
#
#    This script is used to package up the open source zip distribution file.
#
# Author:    Brian Frank
# Creation:  9 Jul 08
#

import os
import sys
import env
import fileutil
import makesedona
import makesedonac
import makewinvm
import makeunixvm
import compilekit

# exact copies; tuple is (dirName, exclude regex)
xcopies  = [
  ("adm",               [".DS_Store", ".*\.pyc"]),
  ("apps",              [".DS_Store", ".*\.sab"]),
  ("bin",               [".DS_Store", ".*\.exe"]),
  ("lib",               [".DS_Store"]),
  ("build/doc",         [".DS_Store"]),
  ("build/kits",        [".DS_Store"]),
  ("build/manifests",   [".DS_Store"]),
  ("platforms",         [".DS_Store", "tridium"]),
  ("scode",             [".DS_Store", ".*\.scode"]),
  ("src",               [".DS_Store", ".*\.iml", ".*\.class"]),
]

# directories
stageDir = os.path.join(env.build, "stage")

################################################################
# Main
################################################################

def main():
  nuke()      # Nuke directories for that fresh feeling
  compile()   # Compile everything
  stage()     # Stage the files we want to distribute
  zip()       # Zip it up into one file

################################################################
# Nuke
################################################################

def nuke():
  print "====== makedist.nuke ======"
  fileutil.rmdir(env.build)

################################################################
# Compile
################################################################

def compile():
  print "====== makedist.compile ======"
  # make sedona.jar
  makesedona.compile()

  # make sedonac.jar
  makesedonac.compile()

  # make all kits
  compilekit.compile(env.kits, ["-doc", "-outDir", env.build])

  # make docs
  docOut = os.path.join(env.build, "doc")
  fileutil.cpdir(env.doc, docOut)
  compilekit.compile(os.path.join(docOut, "toc.xml"), ["-doc"])

  # Make Sedona VM (svm)
  if os.name == "posix": # unix, OSX
    makeunixvm.main([])
  else: # win32
    makewinvm.compile()

################################################################
# Stage
################################################################

def stage():
  fileutil.rmdir(stageDir)
  fileutil.mkdir(stageDir)
  for x in xcopies:
    dirName = x[0]
    exclude = x[1]
    srcDir = os.path.join(env.home, dirName)
    if os.path.exists(srcDir):
      fileutil.cpdir(srcDir, os.path.join(stageDir, dirName), exclude)

################################################################
# Zipo
################################################################

def zip():
  zipprefix = "sedona_community-" + env.buildVersion()
  zipname = zipprefix + ".zip"
  zippath  = os.path.join(env.home, zipname)
  fileutil.zip(zippath, stageDir, zipprefix+'/')

################################################################
# Main
################################################################

if __name__ == '__main__':
  main()
