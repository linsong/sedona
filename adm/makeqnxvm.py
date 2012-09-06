#! /usr/bin/env python
#
# makeqnxvm.py
#    Author: Clif Turman        6/5/2012
#
#    Compile specific svm for QNX, big-endian, power PC
#
#    USAGE: makeqnxvm [-v VERSION] [-4 | -6]
#              options:  -v VERSION - sets the compiler macro PLAT_BUILD_VERSION, which in turn determines the
#                           "Platform Version" reported in the sedona platform service.  If not specified, defaults
#                           to "buildVersion" in sedona.properties
#                        -4 | -6 - build for ipv6 or ipv4.  If not specified, defaults to ipv4
#    EXAMPLE: makeqnxvm -v 1.2.24 -6    (builds a vm with ipv6 stack, build version identifier 1.2.24
#    EXAMPLE: makeqnxvm -6              (builds a vm with ipv6 stack, build version specified by current sedona.properties
#
#    NOTES:
#       1) Valid installation of QNX dev environment is assumed.
#       2) This script first copies all source files into the <sedona.home>/temp/qnx folder.  This is
#          normally and currently "D:/sedona/baseline/pub/temp/qnx" folder.
#       3) Top-level QNX makefiles are included/checked in the platform source code folder: 
#          "\pub\platforms\src\generic\qnx\ppc\native"
#          In order to compile, two args must be passed to the top-level Makefile:
#          a)the version string, and b) the ipv4 or ipv6 variant.
#          This script does this automatically by looking at the -v and -4/-6 args to this script (makeqnxvm.py) 
#          For example, "makeqnxvm -v 1.2.24 -6" results in the following "make" command line:
#
#          make -C "D:\sedona\baseline\pub\temp\qnx" CCFLAGS+=-DPLAT_BUILD_VERSION="1.2.24" CCFLAGS+=-DSOCKET_FAMILY_INET
#
#       4) Momentics IDE and/or QNX make utility can used to make the svm by adding the CCFLAGS definitions
#          on project compiler preferences (Momentics IDE), or on the command line as shown in note 3 above.

import os
import sys
import env
import argparse
import fileutil
import compilekit
import platArchive                  


platFile = ""
stageDir = ""

# initParser
def initParser():
  global parser
  parser = argparse.ArgumentParser(description='Make Sedona VM for QNX')

  parser.add_argument('-v', '--ver', action='store', default=env.buildVersion(), 
                             help='Set SVM version string to VERSION', 
                             metavar="VERSION")
  
  parser.add_argument('-s', '--stageOnly', action='store_true', default=False,
                             help='Stage the files but do not compile the svm')

  ipgroup = parser.add_mutually_exclusive_group()
  ipgroup.add_argument('-4', '--ipv4', action='store_true', default=False,  
                             help='Use IPv4 protocol (default)')
  ipgroup.add_argument('-6', '--ipv6', action='store_true', default=False, 
                             help='Use IPv6 protocol')


# Make
def compile(platFile, stageDir):  
  try:
    compilekit.compile(platFile,["-outDir ", stageDir])
  except env.BuildError:
    print "**"
    print "** FAILED [cannot compile " + platFile + "]"
    print "**"
    exit()

  
def main(argv=[]):
  if not os.environ.get("QNX_HOST") or not os.environ.get("QNX_TARGET"):
    print " ERROR: QNX dev environment not found"
    exit();

  platFile = os.path.join(env.platforms, "src","generic","qnx","ppc","generic-qnx-ppc.xml")
  stageDir = os.path.join(env.temp,"qnx")
  global parser

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

  print "platFile = " + platFile
  print "stageDir = " + stageDir
  print

  # Add command line arg to select ipv4 vs. ipv6 socket family
  if (options.ipv6):
    print " Building Sedona VM to use IPv6 protocol"
  else:    # Defaults to IPv4
    print " Building Sedona VM to use IPv4 protocol"

  # Add cmd line arg to set version string
  if options.ver:
    print " Building Sedona VM version " + options.ver
  else:
    print " Building Sedona VM version " + env.buildVersion()

  #compile the platform definition file
  compile(platFile, stageDir)  

  #create the qnx specific hardware variant folders for ppc
  if not os.path.exists(os.path.join(stageDir,"ppc")):
    print "creating ppc folder"
    os.mkdir(os.path.join(stageDir,"ppc"))
    
  #create the "standard" qnx recursive makefile
  if not os.path.exists(os.path.join(stageDir,"ppc","Makefile")):
    print "create a Makefile in the ppc folder"
    ppcmakeFile = open(os.path.join(stageDir,"ppc","Makefile"),"w")
    ppcmakeFile.write("LIST=VARIANT\n")
    ppcmakeFile.write("ifndef QRECURSE\n")
    ppcmakeFile.write("QRECURSE=recurse.mk\n")
    ppcmakeFile.write("ifdef QCONFIG\n")
    ppcmakeFile.write("QRDIR=$(dir $(QCONFIG))\n")
    ppcmakeFile.write("endif\n")
    ppcmakeFile.write("endif\n")
    ppcmakeFile.write("include $(QRDIR)$(QRECURSE)\n")
    ppcmakeFile.close()

  #create the big-endian hardware variant folder
  if not os.path.exists(os.path.join(stageDir,"ppc","o-be")):
    print "creating ppc/o-be folder"
    os.mkdir(os.path.join(stageDir,"ppc","o-be"))

  #create the "standard" qnx bottom level makefile
  if not os.path.exists(os.path.join(stageDir,"ppc","o-be","Makefile")):
    print "create a Makefile in the ppc/o-be folder"
    obemakeFile = open(os.path.join(stageDir,"ppc","o-be","Makefile"),"w")
    obemakeFile.write("include ../../common.mk\n")
    obemakeFile.close()

  #don't compile if we are only staging
  if (options.stageOnly):
    print "\nSource files have been staged in " + stageDir
    exit()

  #NOTE: The root level Makefile, common.mk, and .qnx_internal.mk should all exist
  # in the root directory, as they are checked in as part of the platform native source.
  # Now we need to construct a command line to invoke "make" while passing compiler
  # flags for version number and ip stack type
  command = "make"

  #change to the stage directory before compiling
  command += " -C " + r'"' + stageDir + r'"' 

  # Add any other options supplied by caller
  command += " CCFLAGS+=-DPLAT_BUILD_VERSION="
  if options.ver:
    command += r'"' + options.ver + r'"'
  else:
    command += r'"' + env.buildVersion() + r'"'

  if options.ipv6:
    command += " CCFLAGS+=-DSOCKET_FAMILY_INET6"
  else:
    command += " CCFLAGS+=-DSOCKET_FAMILY_INET"

  print command
  status = os.system(command)
  if status:
    raise env.BuildError("FATAL: connot compile qnx vm ")

  if not os.path.exists(os.path.join(stageDir,"ppc","o-be","svm")):
    print "ERROR: svm not found"
    exit()

  fileutil.cpfile(os.path.join(stageDir,"ppc","o-be","svm"),os.path.join(stageDir,".par","svm","svm"),force=0)

  
  platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])

# Main
if __name__ == '__main__':
  main(sys.argv[1:])

