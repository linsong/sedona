#! /usr/bin/env python
#
# makedev.py
# 
#    This script is used to rebuild and test what a developer typically
#    needs to get going.  It rebuilds most everything from source, 
#    then runs the sedonac and win32 svm test suite as a fairly quick 
#    sanity check.
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#                 

import os
import env
import fileutil
import makesedona
import makesedonac
import makesedonacert
import makewinvm
import compilekit

# Main
if __name__ == '__main__':
  if os.name != "nt":
    raise env.BuildError("FATAL: makedev.py can only run on windows.")
     
  # make sedona.jar  
  makesedona.compile()
  
  # make sedonac.jar  
  makesedonac.compile()

  # make sedonacert.jar  
  makesedonacert.compile()
  
  # make all kits
  compilekit.compile(env.src)

  # make windows svm
  makewinvm.compile()
  
  # make windows test scode
  compilekit.compile(os.path.join(env.scode, "x86-test.xml"))  
  
  # run sedonac tests
  status = os.system(env.sedonacExe + " -test")
  if status:
    raise env.BuildError("FATAL: sedonac tests failed")   

  # run svm tests
  status = os.system(env.svmExe + " " + os.path.join(env.scode, "x86-test.scode") + " -test")
  if status:
    raise env.BuildError("FATAL: svm tests failed")      

  # run jsvm tests
  status = os.system(env.jsvmExe + " -test")
  if status:
    raise env.BuildError("FATAL: jsvm tests failed")      
  
    
