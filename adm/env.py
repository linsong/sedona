#! /usr/bin/env python
#
# env.py
# 
#    Provides a set of global variables and functions
#    for the current Sedona build environment.
#
# Author:    Brian Frank
# Creation:  6 Dec 02
# 

import sys
import os           
import env
import props

# Environment Variables
javaHome = os.environ.get("java_home")
if not javaHome or not os.path.exists(javaHome):
  print ""
  print "WARNING: java_home environment variable not setup correctly"
  print ""                 

# Home Directory: if the "sedona_home" environment variable
# we use that; otherwise we assume this script is {home}\adm\env.py
home = os.path.dirname(os.path.dirname(sys.argv[0]))
envHome = os.environ.get("sedona_home")
if envHome and os.path.exists(envHome):
  home = os.path.abspath(envHome)
  
# Sedona Environment
adm          = os.path.join(home, "adm")
bin          = os.path.join(home, "bin")
doc          = os.path.join(home, "doc")
kits         = os.path.join(home, "kits")
lib          = os.path.join(home, "lib")
manifests    = os.path.join(home, "manifests")
platforms    = os.path.join(home, "platforms")
scode        = os.path.join(home, "scode")
src          = os.path.join(home, "src")
apps         = os.path.join(home, "apps")
temp         = os.path.join(home, "temp")         
sedonacExe   = os.path.join(bin, "sedonac.exe")
svmExe       = os.path.join(bin, "svm.exe")
jsvmExe      = os.path.join(bin, "jsvm.exe")
sedonaJar    = os.path.join(lib, "sedona.jar")
sedonacJar   = os.path.join(lib, "sedonac.jar")
sedonacertJar= os.path.join(lib, "sedonacert.jar")
jikes        = os.path.join(adm, "jikes.exe")

# Build Environment
buildHome   = "d:\\sedona\\build"

# Java Environment  
javaBin      = os.path.join(javaHome, "bin")
jar          = os.path.join(javaBin,  "jar.exe")
jreHome      = os.path.join(javaHome, "jre")
jreLib       = os.path.join(jreHome,  "lib")
jreRt        = os.path.join(jreLib,   "rt.jar")
cpSep        = ";" # classpath separator char for javac

# Platform (os) dependent configuration/re-configuration
if os.name == "posix":
  cpSep       = ":"
  jikes       = "jikes"   # assume in the PATH
  jar         = os.path.join(javaBin, "jar")
  sedonacExe  = os.path.join(bin, "sedonac.sh")
  svmExe      = os.path.join(bin, "svm")
  jsvmExe     = os.path.join(bin, "jsvm")
  svmPlatform = os.environ.get("SVM_PLATFORM")
  
# Get the buildVersion defined in /lib/sedona.properties
def buildVersion():
  return props.load(os.path.join(lib, "sedona.properties"))["buildVersion"]

# BuildError
class BuildError(Exception):
  def __init__(self, value):
    self.value = value
  def __str__(self):
    return repr(self.value)

# Dump the environment setting
def dump():           
  print "------- env ------"
  keys = env.__dict__.keys()
  keys.sort()
  for slot in keys:
    if not slot.startswith("__"):
      print "%-20s %20s" % (slot + ":", getattr(env, slot))
    
# Main
if __name__ == '__main__':     
  dump()

