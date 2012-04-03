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

includes = []

libs = [ "ws2_32.lib",  "uuid.lib", "kernel32.lib"]


#
# Default compiler args - may be overridden by cmd line
#
defs     = [ ("PLAT_BUILD_VERSION",'\\"' + env.buildVersion() + '\\"'), 
             ("SOCKET_FAMILY_INET","0")]            # un-comment for IPv4 support
#             ("SOCKET_FAMILY_INET6","0")]           # un-comment for IPv6 support



# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Make Sedona VM for Windows')
  ipgroup = parser.add_mutually_exclusive_group()
  ipgroup.add_argument('-4', '--ipv4', action='store_true', default=True,  
                             help='Use IPv4 protocol (default)')
  ipgroup.add_argument('-6', '--ipv6', action='store_true', default=False, 
                             help='Use IPv6 protocol')
  ipgroup.add_argument('-v', '--ver', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VERSION', 
                             metavar="VERSION")


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



# Main
if __name__ == '__main__':
  global parser
  config = []

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

  # Add command line arg to select ipv4 vs. ipv6 socket family
  if (options.ipv4):
    config.append(("SOCKET_FAMILY_INET","0"))               # value doesn't matter
    print " Building Sedona VM to use IPv4 protocol.\n"
  elif (options.ipv6):
    config.append(("SOCKET_FAMILY_INET6","0"))              # value doesn't matter
    print " Building Sedona VM to use IPv6 protocol.\n"
  #else:  
    # Defaults to IPv4

  # Add cmd line arg to set version string
  if options.ver:
    verstr = '\\"' + options.ver + '\\"'
    config.append(("PLAT_BUILD_VERSION",verstr))

  # Compile Sedona VM
  compile(config)

  # Create platform archive and install in platform DB
  platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])


