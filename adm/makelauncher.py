#! /usr/bin/env python
#
# makelauncher.py
# 
#    Compile sedonac.exe launcher file
#
# Author:    Brian Frank
# Creation:  7 Dec 07
# 

import os
import env
import compilewin          


exeFileA = os.path.join(env.bin, "sedonac.exe")
exeFileD = os.path.join(env.bin, "sedonacert.exe")

srcFiles = [
  os.path.join(env.src, "launcher", "launcher.cpp"),
]

includes = [
  os.path.join(env.javaHome, "include"),
  os.path.join(env.javaHome, "include", "win32"),
]

libs = [
  "uuid.lib",
  "advapi32.lib",
]


#
# Dictionaries of compiler args - may be modified by cmd line
#
defsA = { 'LAUNCHER_MAIN':'\\"sedonac/Main\\"' }
defsD = { 'LAUNCHER_MAIN':'\\"sedonacert/Main\\"' }


# Make
def compile():
  try:
    compilewin.compile(exeFileA, srcFiles, includes, libs, defsA)
    compilewin.compile(exeFileD, srcFiles, includes, libs, defsD)
  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"
    
    
# Main
if __name__ == '__main__': 
  compile()

