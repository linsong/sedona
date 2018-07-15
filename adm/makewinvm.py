#! /usr/bin/env python
#
# makewinvm.py
#
#    Compile svm.exe Window's Sedona virtual machines
#
# Author:    Brian Frank
# Creation:  7 Dec 07
#

import os
import sys
import env
import argparse
import fileutil
import compilewin
import compilekit
import platArchive
import xml.dom.minidom

compiler = "msvc"
# compiler = "mingw32"

platFile = ""

exeFile = os.path.join(env.bin, "svm.exe")

stageDir = os.path.join(env.temp, "win32")

srcFiles = [ os.path.join(stageDir, "*.c")]

includes = [ stageDir ]

libs = [ "ws2_32.lib",  "uuid.lib", "kernel32.lib"]


#
# Dictionary of compiler args - may be modified by cmd line
#
defs = { 'WIN32':None, '_WIN32':None, 'SOCKET_FAMILY_INET':None,
         'PLAT_BUILD_VERSION':'\\"' + env.buildVersion() + '\\"' }


def platformType(string):
  if xml.dom.minidom.parse(string).documentElement.nodeName != "sedonaPlatform":
    msg = "" + string + " is not a sedonaPlatform XML file"
    raise argparse.ArgumentTypeError(msg)
  global platFile
  platFile = string


def compilerType(string):
  if not getattr(compilewin, string, None):
    msg =  "Compiler '" + string + "' is not supported."
    raise argparse.ArgumentTypeError(msg)
  global compiler
  compiler = string

# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Make Sedona VM for Windows')

  parser.add_argument('-v', '--ver', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VERSION', 
                             metavar="VERSION")

  parser.add_argument('-l', '--list-compilers', action='store_true',
                             help='List the supported compilers')

  parser.add_argument('-p', '--platform', type=platformType, action='store',
                             help='The path to the sedonaPlatform XML file. \nEnvironment variable SVM_PLATFORM can be used to set the platform path.',
                             metavar="SVM_PLATFORM")
  parser.add_argument('-c', '--compiler', type=compilerType, action='store',
                             help="The compiler to use. Defaults to 'msvc'", metavar="COMPILER")

  ipgroup = parser.add_mutually_exclusive_group()
  ipgroup.add_argument('-4', '--ipv4', action='store_true', default=False,  
                             help='Use IPv4 protocol (default)')
  ipgroup.add_argument('-6', '--ipv6', action='store_true', default=False, 
                             help='Use IPv6 protocol')

# list compilers
def listCompilers():
  compilers = [e for e in dir(compilewin) if callable(getattr(compilewin,e))]# and not e.startsWith("__")]
  if len(compilers) == 0:
    print "No compilers supported for building the SVM"
  else:
    print "Compiler(s) supported for building the SVM:"
    for c in compilers:
      print " " + c


# compile
def compile(cdefs=defs):
  try:
    if xml.dom.minidom.parse(platFile).documentElement.nodeName != "sedonaPlatform":
      raise env.BuildError("" + platFile + " is not a sedonaPlatform XML file")

    fileutil.rmdir(stageDir)
    compilekit.compile(platFile + " -outDir " + stageDir)
    getattr(compilewin, compiler)(exeFile, srcFiles, includes, libs, cdefs)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"
    sys.exit(1)


def verifyOpts():
  global platFile
  if not platFile:
    platFile = os.environ.get("SVM_PLATFORM")
    if not platFile:
      platFile = os.path.join(env.platforms, "src", "generic", "win32", "generic-win32.xml")


# Main
if __name__ == '__main__':
  global parser
  config = defs

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

  if (options.list_compilers):
    listCompilers()
    sys.exit(0)

  if options.compiler:
    compiler = options.compiler

  if options.platform:
    platFile = options.platform

  # Add command line arg to select ipv4 vs. ipv6 socket family
  if (options.ipv6):
    defs.pop("SOCKET_FAMILY_INET", None)    # Remove ipv4 defn
    defs["SOCKET_FAMILY_INET6"] = None
    print " Building Sedona VM to use IPv6 protocol.\n"

  else:    # Defaults to IPv4
    defs.pop("SOCKET_FAMILY_INET6", None)    # Remove ipv6 defn
    defs["SOCKET_FAMILY_INET"] = None
    print " Building Sedona VM to use IPv4 protocol.\n"

  # Add cmd line arg to set version string
  if options.ver:
    defs["PLAT_BUILD_VERSION"] = '\\"' + options.ver + '\\"'

  verifyOpts()

  # Compile Sedona VM
  compile(config)

  # Create platform archive and install in platform DB
  platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])


