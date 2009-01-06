#! /usr/bin/env python
#
# makelinuxvm.py
#
#    Compile svm.exe Linux's Sedona virtual machine
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#

import os
import env
import fileutil
import compilelinux
import compilekit

platDir = os.path.join(env.platforms, "generic", "linux")

platFile = os.path.join(platDir, "generic-linux.xml")

exeFile = os.path.join(platDir, "svm")

stageDir = os.path.join(env.temp, "linux")

srcFiles = [ os.path.join(stageDir, "*.c")]

includes = []

libs = []

defs = [("__LINUX__", "1")]


# Make
def compile():
  try:
    compilekit.compile(platFile + " -outDir " + stageDir)
    compilelinux.compile(exeFile, srcFiles, includes, libs, defs)
    fileutil.cpfile(exeFile, env.svmExe)
    os.chmod(env.svmExe, 0755)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"


# Main
if __name__ == '__main__':
  compile()

