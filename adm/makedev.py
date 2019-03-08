#! /usr/bin/env python3
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

from __future__ import print_function
import os
import env
import fileutil
import makesedona
import makesedonac
import makesedonacert
import makewinvm
import makeunixvm
import compilekit
import argparse

# Default app & scode files, assumed to be in <sedona>/apps and <sedona>/scode
defaultapp   = "test.sax"
defaultscode = "x86-test.xml"


# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Build and run Sedona VM')

  parser.add_argument('-v', '--version', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VER', metavar="VER")
  parser.add_argument('-t', '--test', action='append', 
                             help='Run specified test (default is all)',
                             default=None, choices=['sedonac', 'svm', 'all', 'none'], 
                             metavar="TEST")
  parser.add_argument('-k', '--kits', action='store',
                             help='Specify XML to build additional external kits. Default is None',
                             default=None,
                             metavar="KITS")
  parser.add_argument('-r', '--run', action='store_true', default=False,
                             help='Run an app after building')
  parser.add_argument('-a', '--app', action='store', 
                             help='Specify app SAX to run (default is %s)' % defaultapp,
                             default=os.path.join(env.apps, defaultapp), metavar="SAX")
  parser.add_argument('-s', '--scode', action='store', 
                             help='Specify scode XML to run (default is %s)' % defaultscode,
                             default=os.path.join(env.scode, defaultscode), 
                             metavar="XML")

  help_msg = 'The path to the sedonaPlatform XML file. ' + \
             'If this option is omitted, then environment variable $SVM_PLATFORM will be checked. ' + \
             'If that variable is not set, then $sedona_home/platforms/src/generic/unix/generic-unix.xml ' + \
             'will be used.'
  parser.add_argument('-p', '--platform', action='store',
                             help=help_msg,
                             default=None,
                             metavar="PLATFORM")

  parser.add_argument('-c', '--compiler', action='store',
                             help='Compiler to use. Defaults to gcc',
                             default=None,
                             metavar="CC")

  parser.add_argument('--sys', action='store',
                             help='Force to use build scripts for specific platform (posix,nt)',
                             default=os.name)

# Main
if __name__ == '__main__':
  global parser

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

  # Print options
  print('options.version = ', options.version)
  print('options.test    = ', options.test)
  print('options.kits    = ', options.kits)
  print('options.run     = ', options.run)
  print('options.scode   = ', options.scode)
  print('options.app     = ', options.app)
  print('options.sys     = ', options.sys)

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

  if not run_sc: print('  Skipping sedonac tests')
  if not run_sv: print('  Skipping svm tests')

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
  compilekit.compile(env.kits)

  # Make additional external kits
  if options.kits is not None:
    print("Build additional external kits")
    compilekit.compile(options.kits)

  # Make windows test scode (or scode specified on cmd line)
  compilekit.compile(options.scode)
  
  # Make windows test scode
  if options.app:
    compilekit.compile(options.app)

  # Make Sedona VM (svm)
  if options.sys == "posix": # unix, OSX
    make = makeunixvm
  else: # win32
    make = makewinvm

  args = []
  if options.compiler is not None:
    args += ['-c', options.compiler ]

  if options.platform is not None:
    args += ['-p', options.platform ]

  status = make.main(args)
  if status:
    raise env.BuildError("FATAL: make svm failed")

  # Run sedonac tests
  if run_sc:
    print("\n\n")
    print("  ---------------------------------------------------------")
    print("  -------------------- Testing sedonac --------------------")
    print("  ---------------------------------------------------------")
    print("\n\n")
    status = os.system(env.sedonacExe + " -test")
    if status:
      raise env.BuildError("FATAL: sedonac tests failed")   

  # Run SVM tests
  if run_sv:
    print("\n\n")
    print("  ---------------------------------------------------------")
    print("  -------------------- Testing svm ------------------------")
    print("  ---------------------------------------------------------")
    print("  scode =", scodefile)
    print("\n\n")
    status = os.system(env.svmExe + " " + scodefile + " -test")
    if status:
      raise env.BuildError("FATAL: svm tests failed")      


  # Run SVM with specified app
  if options.run:
    status = os.system(env.sedonacExe + " " + options.app)
    if status:
      raise env.BuildError("FATAL: app failed to build")      

    print("\n\n")
    print("  ---------------------------------------------------------")
    print("  -------------------- Running svm ------------------------")
    print("  ---------------------------------------------------------")
    print("  scode =", scodefile)
    print("  app   =", sabfile)
    print("\n\n")

    status = os.system(env.svmExe + " " + scodefile + " " + sabfile)
    if status:
      raise env.BuildError("FATAL: svm failed to run")      
