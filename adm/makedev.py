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
  status = makesedona.compile()
  if status:
    raise env.BuildError("FATAL: makesedona failed")   
  
  # make sedonac.jar  
  status = makesedonac.compile()
  if status:
    raise env.BuildError("FATAL: makesedonac failed")   

  # make sedonacert.jar  
  status = makesedonacert.compile()
  if status:
    raise env.BuildError("FATAL: makesedonacert failed")   
  
  # make all kits
  compilekit.compile(env.src)

  # make windows svm
  status = makewinvm.compile()
  if status:
    raise env.BuildError("FATAL: makewinvm failed")   
  
  # make windows test scode
  compilekit.compile(os.path.join(env.scode, "x86-test.xml"))  
  
  # run sedonac tests
  print "\n\n"
  print "  ---------------------------------------------------------"
  print "  -------------------- Testing sedonac --------------------"
  print "  ---------------------------------------------------------"
  print "\n\n"
  status = os.system(env.sedonacExe + " -test")
  if status:
    raise env.BuildError("FATAL: sedonac tests failed")   

  # run svm tests
  print "\n\n"
  print "  ---------------------------------------------------------"
  print "  -------------------- Testing svm ------------------------"
  print "  ---------------------------------------------------------"
  print "\n\n"
  status = os.system(env.svmExe + " " + os.path.join(env.scode, "x86-test.scode") + " -test")
  if status:
    raise env.BuildError("FATAL: svm tests failed")      

  # run jsvm tests
  #print "\n\n"
  #print "  ---------------------------------------------------------"
  #print "  -------------------- Testing jsvm -----------------------"
  #print "  ---------------------------------------------------------"
  #print "\n\n"
  #status = os.system(env.jsvmExe + " -test")
  #if status:
  #  raise env.BuildError("FATAL: jsvm tests failed")      
  
    
