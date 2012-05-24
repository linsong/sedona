#! /usr/bin/env python
#
# makesedonacert.py
# 
#    Compile sedonacert.jar
#
# Author:    Brian Frank
# Creation:  16 Jun 09
# 

import os
import env
import compilejar          

depends = [env.sedonaJar,env.sedonacJar]
srcDir  = os.path.join(env.src, "sedonacert", "src")
jarFile = env.sedonacertJar
packages = [ 
  "sedonacert",
  "sedonacert.prov",
  "sedonacert.sox",
]

# Make
def compile():
  try:
    compilejar.compile(srcDir, depends, packages, jarFile)
  except env.BuildError:
    print "**"
    print "** FAILED [" + jarFile + "]"
    print "**"
    return 1
    
# Main
if __name__ == '__main__': 
  compile()
