#! /usr/bin/env python
#
# makesedona.py
# 
#    Compile sedonac.jar
#
# Author:    Brian Frank
# Creation:  7 Dec 07
# 

import os
import env
import compilejar          

depends = []
srcDir  = os.path.join(env.src, "sedona", "src")
jarFile = env.sedonaJar
packages = [ 
  "sedona", 
  "sedona.kit", 
  "sedona.manifest",
  "sedona.platform",
  "sedona.offline",
  "sedona.dasp",
  "sedona.sox",
  "sedona.upload",
  "sedona.util",
  "sedona.web",
  "sedona.xml",
  "sedona.vm",
  "sedona.vm.sys",
  "sedona.vm.inet",
]

# Make
def compile():
  try:
    compilejar.compile(srcDir, depends, packages, jarFile)
  except env.BuildError:
    print "**"
    print "** FAILED [" + jarFile + "]"
    print "**"
    
# Main
if __name__ == '__main__': 
  compile()

