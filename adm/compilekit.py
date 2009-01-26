#! /usr/bin/env python
#
# compilekit.py
# 
#    Standard code used to compile a kit (or kits).
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#                 

import os
import env

#
# Compile kit file
#   srcDir:   directory containing the kit.xml file
#
def compile(srcDir, args = []):  
  cmd = env.sedonacExe + " " + srcDir
  for arg in args:
    cmd += " " + arg         
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: compilekit " + srcDir)   
