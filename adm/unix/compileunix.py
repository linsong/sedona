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
  cmd = "gcc"
  for include in includes:
    cmd += " -I\"" + include + "\""
    
  # defines (tuples)
  for d in defs:
    cmd += " -D" + d[0] + "=" + d[1]      

  cmd += " -DPLAT_BUILD_VERSION=" + '\\"' + env.buildVersion() + '\\"'

  # libs     
  for lib in libs:
    cmd += " -L\"" + lib + "\""

  # src     
  for src in srcFiles:
    cmd += " " + src
  
  # remaining options  
  cmd += " -O2"
  cmd += " -o " + exeFile

  # compile away
  print cmd
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: compileunix " + exeFile)     

  print "  Success [" + exeFile + "]"
    
