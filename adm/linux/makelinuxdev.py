#! /usr/bin/env python
#
# makelinux.py
# 
#    This script is used to rebuild and test what a developer typically
#    needs to get going.  It rebuilds most everything from source, 
#    then runs the sedonac and linux svm test suite as a fairly quick 
#    sanity check.
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#                 

import os
import env
import fileutil
import makesedona
import makesedonac
import makelinuxvm
import compilekit

# Main
if __name__ == '__main__':
  # make sedona.jar  
  makesedona.compile()
  
  # make sedonac.jar  
  makesedonac.compile()
  
  # make all kits
  compilekit.compile(env.src)

  # make linux svm
  makelinuxvm.compile()
  
  # make windows test scode
  #compilekit.compile(os.path.join(env.scode, "x86-test.xml"))  
  
  # run sedonac tests
  #status = os.system(env.sedonacExe + " -test")
  #if status:
  #  raise env.BuildError("FATAL: sedonac tests failed")   

  # run svm tests
#  status = os.system(env.svmExe + " " + os.path.join(env.scode, "x86-test.scode") + " -test")
#  if status:
#    raise env.BuildError("FATAL: sedonac tests failed")      
