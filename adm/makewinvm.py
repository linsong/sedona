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
import env
import argparse
import fileutil
import compilewin
import compilekit
import platArchive

platFile = os.path.join(env.platforms, "src", "generic", "win32", "generic-win32.xml")

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


# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Make Sedona VM for Windows')

  parser.add_argument('-v', '--ver', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VERSION', 
                             metavar="VERSION")

  ipgroup = parser.add_mutually_exclusive_group()
  ipgroup.add_argument('-4', '--ipv4', action='store_true', default=False,  
                             help='Use IPv4 protocol (default)')
  ipgroup.add_argument('-6', '--ipv6', action='store_true', default=False, 
                             help='Use IPv6 protocol')

# compile
def compile(cdefs=defs):
  try:
    fileutil.rmdir(stageDir)
    compilekit.compile(platFile + " -outDir " + stageDir)
    #compilewin.compile(exeFile, srcFiles, includes, libs, cdefs, ["/P"])  # preproc output
    compilewin.compile(exeFile, srcFiles, includes, libs, cdefs)

  except env.BuildError:
    print "**"
    print "** FAILED [" + exeFile + "]"
    print "**"
    return 1



# Main
if __name__ == '__main__':
  global parser
  config = defs

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

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

  # Compile Sedona VM
  compile(config)

  # Create platform archive and install in platform DB
  platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])


