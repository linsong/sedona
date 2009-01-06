#! /usr/bin/env python
#
# compilelinux.py
# 
#    Standard code used to compile a Linux executable
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#                 

import os
import env
import fileutil

#
# Compile C source to a Linux executable
#   exeFile:  absolute path of output executable
#   srcFiles: list of absolute source files
#   includes: list of absolute directories
#   libs:     list of filenames
#   defs:     list of name/value tuples
#
def compile(exeFile, srcFiles, includes, libs, defs):  
  print "Compile [" + os.path.basename(exeFile) + "]"

  # standard includes                                                                   
  cmd = "gcc"
  for include in includes:
    cmd += " -I\"" + include + "\""
    
  # defines (tuples)
  for d in defs:
    cmd += " -D" + d[0] + "=" + d[1]      

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
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: compilelinux " + exeFile)     

  print "  Success [" + exeFile + "]"
    
