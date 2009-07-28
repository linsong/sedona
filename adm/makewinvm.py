#! /usr/bin/env python
#
# makewinvm.py
#
#    Compile svm.exe Window's Sedona virtual machines
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#

import os
import env
import fileutil
import compilewin
import compilekit

platFile = os.path.join(env.platforms, "src", "generic", "win32", "generic-win32.xml")

exeFile = os.path.join(env.bin, "svm.exe")

stageDir = os.path.join(env.temp, "win32")

srcFiles = [ os.path.join(stageDir, "*.c")]

includes = []

libs = [ "ws2_32.lib",  "uuid.lib", "kernel32.lib"]

defs = []


# Make
def compile():
  try:
    compilekit.compile(platFile + " -outDir " + stageDir)
    compilewin.compile(exeFile, srcFiles, includes, libs, defs)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"


# Main
if __name__ == '__main__':
  compile()

