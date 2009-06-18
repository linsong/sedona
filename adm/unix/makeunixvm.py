#! /usr/bin/env python
#
# makeunixvm.py
#
#    Compile svm 
#
# Author:    Matthew Giannini
# Creation:  10 Dec 08
#

import sys
import getopt
import os
import re
import glob
import env
import fileutil
import compileunix
import compilekit

compiler = "gcc"
platDir = os.environ.get("SVM_PLATFORM")

includes = []

libs = []

defs = [("__UNIX__", "1")]


# usage
def usage():
# print "12345678902234567890323456789042345678905234567890623456789072345678908234567890"
  print " makeunixvm.py [opts]"
  print " Compiles the svm for a UNIX platform."
  print ""
  print " Options:"
  print "   -c, --compiler      The compiler to use. Defaults to 'gcc'."
  print "   -p, --platform      The path that contains the sedonaPlatform XML file. If"
  print "                       the path is relative, it is assumed to be rooted at"
  print "                       $sedona_home/platforms. If this option is omitted, then"
  print "                       the environment variable $SVM_PLATFORM is used."
  print "   -l                  List the supported compilers."   
  print "   -h, --help          Show this usage"

# list compilers
def listCompilers():
  compilers = [e for e in dir(compileunix) if callable(getattr(compileunix,e))]# and not e.startsWith("__")]
  if len(compilers) == 0:
    print "No compilers supported for building the SVM"
  else:
    print "Compiler(s) supported for building the SVM:"
    for c in compilers:
      print " " + c

# Make
def compile():
  global platDir
  try:
    # init all variables
    platDir = platDir if os.path.isdir(platDir) else os.path.join(env.platforms, platDir)
    platFile = glob.glob(os.path.join(platDir, "*.xml"))[0]
    stageDir = os.path.join(env.temp, re.sub("\.xml$", "", os.path.split(platFile)[1]))
    srcFiles = [ os.path.join(stageDir, "*.c") ]

    fileutil.rmdir(stageDir)
    compilekit.compile(platFile + " -outDir " + stageDir)
    getattr(compileunix, compiler)(env.svmExe, srcFiles, includes, libs, defs)
    os.chmod(env.svmExe, 0755)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"

def verifyOpts():
  if not platDir:
    print "Error: Sedona platform is not specified (--platform), or $SVM_PLATFORM is not set."
    sys.exit(1)
  elif not getattr(compileunix, compiler, None):
    print "Error: Compiler '" + compiler + "' is not supported."
    sys.exit(1)
  
# main
def main(argv):
  global compiler, platDir
  try:
    opts, args = getopt.getopt(argv, "c:hlp:", ["compiler=", "platform="])
    for opt, arg in opts:
      if opt in ("-l"):
        listCompilers()
        sys.exit()
      elif opt in ("-h", "--help"):
        usage()
        sys.exit()
      elif opt in ("-c", "--compiler"):
        compiler = arg
      elif opt in ("-p", "--platform"):
        platDir = arg

    verifyOpts()
    compile()

  except getopt.GetoptError:
    usage()
    sys.exit(1)

# __main__
if __name__ == '__main__':
  main(sys.argv[1:])
  sys.exit()

