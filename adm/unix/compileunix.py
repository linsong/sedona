#! /usr/bin/env python3
#
# compileunix.py
#
#    Standard code used to compile a UNIX executable
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#

from __future__ import print_function
import os
import platform
import sys
import env
import fileutil

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
  compile_prefix=os.environ.get("SVM_CROSS_COMPILE")
  cmd = "gcc"
  if compile_prefix is not None:
    cmd = compile_prefix + cmd

  if (platform.machine() == 'x86_64') and (compile_prefix is None):
    cmd += " -m32"  # always compile in 32bit mode

  sysroot = os.environ.get("SVM_SYSROOT")
  for include in includes:
    if sysroot is not None and include.startswith("/"):
      include = sysroot + os.sep + include
    cmd += " -I" + include

  # defines (tuples)
  for d in defs:
    if not isinstance(d, tuple):
      d = (d, )
    cmd += " -D" + d[0]
    if len(d)>1 and d[1] is not None:
      cmd += "=" + d[1]

  cmd += " -DPLAT_BUILD_VERSION=" + '\\"' + env.buildVersion() + '\\"'

  # add user CFLAGS
  cflags = os.environ.get("SVM_CFLAGS")
  if cflags is not None:
    cmd += " " + cflags

  # src
  for src in srcFiles:
    cmd += " " + src

  # libs
  for lib in libs:
    cmd += " -l" + lib

  # add user LDFLAGS
  ldflags = os.environ.get("SVM_LDFLAGS")
  if ldflags is not None:
    cmd += " " + ldflags

  # remaining options
  cmd += " -O2"
  cmd += " -o " + exeFile

  # compile away
  print(cmd)
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: compileunix " + exeFile)

  print("  Success [" + exeFile + "]")

