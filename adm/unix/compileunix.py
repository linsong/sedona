#! /usr/bin/env python
#
# compileunix.py
#
#    Standard code used to compile a UNIX executable
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#

import os
import sys
import env
import fileutil

def is64bit():
  return sys.maxsize > 2**32

#
# Compile C source to a unix executable
#   exeFile:  absolute path of output executable
#   srcFiles: list of absolute source files
#   includes: list of absolute directories
#   libs:     list of filenames
#   defs:     list of name/value tuples
#
#def compile(exeFile, srcFiles, includes, libs, defs):
def gcc(exeFile, srcFiles, includes, libs, defs):
  # standard includes
  cmd = "gcc"
  if is64bit():
      cmd += " -m32" # always compile in 32bit mode

  for include in includes:
    cmd += " -I" + include

  # defines (tuples)
  for d in defs:
    cmd += " -D" + d[0]
    if d[1] is not None:
      cmd += "=" + d[1]

  cmd += " -DPLAT_BUILD_VERSION=" + '\\"' + env.buildVersion() + '\\"'

  # src
  for src in srcFiles:
    cmd += " " + src

  # libs
  for lib in libs:
    cmd += " -l" + lib

  # remaining options
  cmd += " -O2"
  cmd += " -o " + exeFile

  # compile away
  print cmd
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: compileunix " + exeFile)

  print "  Success [" + exeFile + "]"

