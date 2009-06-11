#! /usr/bin/env python
#
# compilejar.py
# 
#    Standard code used to compile a set of packages into a jar.
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#                 

import os
import env
import fileutil

#
# Compile java to jar
#   srcDir:   directory containing package directories
#   depends:  absolute paths to dependent jars (rt.jar is implied)
#   packages: list of dotted names
#   jarFile:  absolte path of resulting jar file to build 
#   func:     function called with tempDir after compile but before jar
#
def compile(srcDir, depends, packages, jarFile, func=None):  
  print "Compile [" + os.path.basename(jarFile) + "]"
  # init jarTemp dir
  temp = os.path.join(env.home, "tempJar")  
  fileutil.rmdir(temp, [], 0)
  fileutil.mkdir(temp)
  
  # compile using jikes.exe
  print "  Javac [" + srcDir + "]"
  cmd = env.jikes
  cmd += " +E +Pno-shadow"
  cmd += " -d " + temp
  cmd += " -classpath " + env.jreRt + env.cpSep + srcDir
  for depend in depends:
    cmd += env.cpSep + depend
  for package in packages:
    cmd += " " + os.path.join(srcDir, package.replace(".", os.path.sep), "*.java")    
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: makejar " + jarFile)
    
  # if we have a function call it
  if (func != None):
    func(temp) 
    func(srcDir)
  
  # jar up using jar.exe
  cmd = env.jar + " cf " + jarFile + " -C " + temp + " ."
  status = os.system(cmd)
  if status:
    raise env.BuildError("FATAL: makejar " + jarFile)  
  
  # cleanup
  fileutil.rmdir(temp, [], 0)
  
  # success
  print "  Jar [" + jarFile + "]"

