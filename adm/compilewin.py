#! /usr/bin/env python
#
# compilewin.py
# 
#    Standard code used to compile a Windows executable
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#                 

import os
import env
import fileutil

#
# Compile C source to a Windows exe file
#   exeFile:  absolute path of output exe
#   srcFiles: list of absolute source files
#   includes: list of absolute directories
#   libs:     list of filenames
#   defs:     list of name/value tuples
#
def compile(exeFile, srcFiles, includes, libs, defs, opts=[]):  
  print "Compile [" + os.path.basename(exeFile) + "]"

  # get environment variables
  vcInstallDir = os.environ.get("VCINSTALLDIR")
  if not vcInstallDir or not os.path.exists(vcInstallDir):
    raise Exception("FATAL: Visual Studio environment not setup [VCINSTALLDIR]")     
  
  # get the PlatformSDK directory
  #  1) first check the win_sdk environment
  #  2) assume it is installed under vc7
  winSdk = os.environ.get("win_sdk")
  if not winSdk:
    winSdk = os.path.join(vcInstallDir, "vc7", "PlatformSDK")
  if not os.path.exists(winSdk):
    raise Exception("FATAL: Cannot find PlatformSDK: " + winSdk)     
  
  # make sure exe output directory exists
  if not os.path.exists(os.path.dirname(exeFile)):
    os.makedirs(os.path.dirname(exeFile))
                                                                  
  # standard includes                                                                   
  cmd = "cl"
  cmd += " /I\"" + os.path.join(winSdk, "Include") + "\""
  for include in includes:
    cmd += " /I\"" + include + "\""
    
  # defines (dict)
  for d, v in defs.items():
    cmd += " /D" + d
    if v: cmd += "=" + v

  # libs     
  for lib in libs:
    cmd += " \"" + os.path.join(winSdk, "Lib", lib) + "\""

  # libs     
  for src in srcFiles:
    cmd += " " + src
  
  # force 32-bit
#  cmd += " /DWIN32 /D_WIN32"
  
  # remaining options  
  cmd += " /nologo"
  cmd += " /Fe" + exeFile

  # Add any other options supplied by caller
  for o in opts:
    cmd += " " + o

  #print "----"
  #print cmd
  #print "----"

  # compile away
  status = os.system(cmd)
  
  # cleanup      
  #os.system("del *.obj")
  #os.system("del *.tlh")   
  for f in os.listdir("."):
    if f.endswith(".obj") or f.endswith(".tlh"):
      #print " removing %s" % f
      os.remove(f)
  
  if status:
    raise env.BuildError("FATAL: compilewin " + exeFile)     
   
  print "  Success [" + exeFile + "]"
    
