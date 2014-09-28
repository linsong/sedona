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
import platArchive
import xml.dom.minidom

compiler = "gcc"
platFile = ""
stageDir = ""
includes = []
libs = []
defs = [ ("__UNIX__", "1"), ("SOCKET_FAMILY_INET", "1") ]

# usage
def usage():
# print "12345678902234567890323456789042345678905234567890623456789072345678908234567890"
  print " makeunixvm.py [opts]"
  print " Compiles the svm for a UNIX platform."
  print ""
  print " Options:"
  print "   -c, --compiler      The compiler to use. Defaults to 'gcc'."
  print "   -p, --platform      The path to the sedonaPlatform XML file. If this option"
  print "                       is omitted, then environment variable $SVM_PLATFORM will"
  print "                       be checked. If that variable is not set, then"
  print "                       $sedona_home/platforms/src/generic/unix/generic-unix.xml"
  print "                       will be used."
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
  global platFile, stageDir
  try:
    # init all variables
    if xml.dom.minidom.parse(platFile).documentElement.nodeName != "sedonaPlatform":
      raise env.BuildError("" + platFile + " is not a sedonaPlatform XML file")

    stageDir = os.path.join(env.temp, re.sub("\.xml$", "", os.path.split(platFile)[1]))
    srcFiles = [ os.path.join(stageDir, "*.c") ]
    fileutil.rmdir(stageDir)
    compilekit.compile(platFile + " -outDir " + stageDir)
    getattr(compileunix, compiler)(env.svmExe, srcFiles, includes, libs, defs)
    os.chmod(env.svmExe, 0755)

  except env.BuildError, err:
    print "**"
    print "** " + str(err) 
    print "** FAILED [" + env.svmExe + "]"
    print "**"

def verifyOpts():
  global platFile
  if not platFile:
    platFile = os.environ.get("SVM_PLATFORM")
    if not platFile:
      platFile = os.path.join(env.platforms,"src","generic","unix","generic-unix.xml")
    
  if not getattr(compileunix, compiler, None):
    print "Error: Compiler '" + compiler + "' is not supported."
    sys.exit(1)
  
# main
def main(argv):
  global compiler, platFile, stageDir
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
        platFile  = arg

    verifyOpts()
    compile()
    platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])

  except getopt.GetoptError:
    usage()
    sys.exit(1)

# __main__
if __name__ == '__main__':
  main(sys.argv[1:])
  sys.exit()

