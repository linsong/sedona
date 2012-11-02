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
import time          

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
  "sedona.util",
  "sedona.util.sedonadev",
  "sedona.web",
  "sedona.xml",
]

# Compile
def compile():
  try:                   
    compilejar.compile(srcDir, depends, packages, jarFile, lambda d: writeVerFile(d))
  except env.BuildError:
    print "**"
    print "** FAILED [" + jarFile + "]"
    print "**"
    return 1
    
# Write Version File
def writeVerFile(dir):  
  f = open(os.path.join(dir, "version.txt"), "w")
  f.write("version=" + env.buildVersion() + "\n")
  f.write("time=" + time.strftime("%Y-%m-%d %H:%M") + "\n")
  f.close()  
    
# Main
if __name__ == '__main__': 
  compile()

