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

# override sedona_home to based off the script location
home    = os.path.dirname(os.path.dirname(os.path.abspath(sys.argv[0])))
root    = os.path.dirname(home)
pub     = os.path.join(root, "pub")  
os.environ["sedona_home"] = pub
sys.path.append(os.path.join(pub, "adm"))

import env
import fileutil
import makesedona
import makesedonac
import makewinvm
import compilekit

# exact copies; tuple is (dirName, exclude regex)
xcopies  = [ 
  ("adm",       [".*\.pyc"]), 
  ("apps",      [".*\.sab"]),
  ("bin",       []),
  ("doc",       []),
  ("kits",      []),
  ("lib",       []),
  ("manifests", []),
  ("platforms", ["tridium"]),
  ("scode",     [".*\.scode"]),
  ("src",       [".*\.class"]),
]  

# directories
stageDir = os.path.join(env.home, "stage")

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
  fileutil.rmdir(env.kits)
  fileutil.rmdir(env.manifests)
  fileutil.rmsubdirs(env.doc)
  fileutil.rmdir(env.temp)
  os.path.join(env.home, "test")
  fileutil.rmdir(stageDir)

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
  compilekit.compile(env.src, ["-doc"])

  # make docs
  compilekit.compile(os.path.join(env.doc, "toc.xml"))

  # make windows svm
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
  zipname = "sedona-" + env.buildVersion() + ".zip"
  zippath  = os.path.join(env.home, zipname)
  fileutil.zip(zippath, stageDir)    

################################################################
# Main
################################################################

if __name__ == '__main__':   
  main()
    
