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
import argparse

# Default app & scode files, assumed to be in <sedona>/apps and <sedona>/scode
defaultapp   = "test.sax"
defaultscode = "x86-test.xml"


# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Build and run Sedona VM for Windows')

  parser.add_argument('-v', '--version', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VER', metavar="VER")
  parser.add_argument('-t', '--test', action='append', 
                             help='Run specified test (default is all)',
                             default=None, choices=['sedonac', 'svm', 'all', 'none'], 
                             metavar="TEST")
  parser.add_argument('-r', '--run', action='store_true', default=False,
                             help='Run an app after building')
  parser.add_argument('-a', '--app', action='store', 
                             help='Specify app SAX to run (default is %s)' % defaultapp,
                             default=os.path.join(env.apps, defaultapp), metavar="SAX")
  parser.add_argument('-s', '--scode', action='store', 
                             help='Specify scode XML to run (default is %s)' % defaultscode,
                             default=os.path.join(env.scode, defaultscode), 
                             metavar="XML")


# Main
if __name__ == '__main__':
  global parser

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

  # Print options
  print 'options.version = ', options.version
  print 'options.test    = ', options.test
  print 'options.run     = ', options.run
  print 'options.scode   = ', options.scode
  print 'options.app     = ', options.app

  scodefile = os.path.splitext(options.scode)[0] + '.scode'
  sabfile   = os.path.splitext(options.app)[0]   + '.sab'

  # If no test specified, default to 'all'
  if not options.test: options.test = ['all']

  # Figure out which tests to run
  run_sc = options.test.count('sedonac') > 0 or options.test.count('all') > 0 
  run_sv = options.test.count('svm') > 0     or options.test.count('all') > 0 

  # Specifying 'none' overrides any other selections
  if options.test.count('none') > 0:
    run_sc = False
    run_sv = False

  if not run_sc: print '  Skipping sedonac tests'
  if not run_sv: print '  Skipping svm tests'


  # Make sure OS is Windows
  if os.name != "nt":
    raise env.BuildError("FATAL: makedev.py can only run on windows.")
     
  # Make sedona.jar  
  status = makesedona.compile()
  if status:
    raise env.BuildError("FATAL: makesedona failed")   
  
  # Make sedonac.jar  
  status = makesedonac.compile()
  if status:
    raise env.BuildError("FATAL: makesedonac failed")   

  # Make sedonacert.jar  
  status = makesedonacert.compile()
  if status:
    raise env.BuildError("FATAL: makesedonacert failed")   
  
  # Make all kits
  compilekit.compile(env.src)

  # Make windows test scode (or scode specified on cmd line)
  compilekit.compile(options.scode)
  
  # Make windows test scode
  if options.app:
    compilekit.compile(options.app)
  
  # Make windows SVM
  status = makewinvm.compile()
  if status:
    raise env.BuildError("FATAL: makewinvm failed")   
  
  # Run sedonac tests
  if run_sc:
    print "\n\n"
    print "  ---------------------------------------------------------"
    print "  -------------------- Testing sedonac --------------------"
    print "  ---------------------------------------------------------"
    print "\n\n"
    status = os.system(env.sedonacExe + " -test")
    if status:
      raise env.BuildError("FATAL: sedonac tests failed")   

  # Run SVM tests
  if run_sv:
    print "\n\n"
    print "  ---------------------------------------------------------"
    print "  -------------------- Testing svm ------------------------"
    print "  ---------------------------------------------------------"
    print "  scode =", scodefile
    print "\n\n"
    status = os.system(env.svmExe + " " + scodefile + " -test")
    if status:
      raise env.BuildError("FATAL: svm tests failed")      


  # Run SVM with specified app
  if options.run:
    status = os.system(env.sedonacExe + " " + options.app)
    if status:
      raise env.BuildError("FATAL: app failed to build")      

    print "\n\n"
    print "  ---------------------------------------------------------"
    print "  -------------------- Running svm ------------------------"
    print "  ---------------------------------------------------------"
    print "  scode =", scodefile
    print "  app   =", sabfile
    print "\n\n"

    status = os.system(env.svmExe + " " + scodefile + " " + sabfile)
    if status:
      raise env.BuildError("FATAL: svm failed to run")      



