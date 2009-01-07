#! /usr/bin/env python
#
# makeunixvm.py
#
#    Compile svm 
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#

import os
import env
import fileutil
import compileunix
import compilekit

platDir = os.path.join(env.platforms, "generic", "unix")

platFile = os.path.join(platDir, "generic-unix.xml")

exeFile = os.path.join(platDir, "svm")

stageDir = os.path.join(env.temp, "unix")

srcFiles = [ os.path.join(stageDir, "*.c")]

includes = []

libs = []

defs = [("__UNIX__", "1")]


# Make
def compile():
  try:
    compilekit.compile(platFile + " -outDir " + stageDir)
    compileunix.compile(exeFile, srcFiles, includes, libs, defs)
    fileutil.cpfile(exeFile, env.svmExe)
    os.chmod(env.svmExe, 0755)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"


# Main
if __name__ == '__main__':
  compile()

