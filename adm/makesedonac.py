#! /usr/bin/env python
#
# makesedonac.py
# 
#    Compile sedonac.jar
#
# Author:    Brian Frank
# Creation:  7 Dec 07
# 

import os
import env
import compilejar          

depends = [env.sedonaJar]
srcDir  = os.path.join(env.src, "sedonac", "src")
jarFile = env.sedonacJar
packages = [ 
  "sedonac",
  "sedonac.analysis",
  "sedonac.asm",
  "sedonac.ast",
  "sedonac.gen",
  "sedonac.ir",
  "sedonac.namespace",
  "sedonac.parser",
  "sedonac.platform",
  "sedonac.scode",
  "sedonac.steps",
  "sedonac.test",
  "sedonac.translate",
  "sedonac.util",
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
