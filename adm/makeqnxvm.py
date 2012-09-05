#! /usr/bin/env python
#
# makeqnxvm.py
#
#    Compile specific svm for QNX, big-endian, power PC
#

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
    print " Must execute from a QNX dev environment - \n"
    print " for example, first run D:\emb\qnx630\scripts\qnx641rc_26.bat"
    exit();

  #platFile = os.path.join(env.platforms, "src", "generic", "platQnxPpc", "tridium-jace-qnx-ppc.xml")
  platFile = os.path.join(env.platforms, "src","generic","qnx","ppc","tridium-jace-qnx-ppc.xml")
  stageDir = os.path.join(env.temp,"qnx")
  global parser

  # Parse command line arguments
  initParser()
  options = parser.parse_args()

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

  print "\nplatFile = " + platFile
  print "stageDir = " + stageDir
  
  compile(platFile, stageDir)

  if not os.path.exists(os.path.join(stageDir,"ppc")):
    print "creating ppc folder"
    os.mkdir(os.path.join(stageDir,"ppc"))
    
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

  if not os.path.exists(os.path.join(stageDir,"ppc","o-be")):
    print "creating ppc/o-be folder"
    os.mkdir(os.path.join(stageDir,"ppc","o-be"))

  if not os.path.exists(os.path.join(stageDir,"ppc","o-be","Makefile")):
    print "create a Makefile in the ppc/o-be folder"
    obemakeFile = open(os.path.join(stageDir,"ppc","o-be","Makefile"),"w")
    obemakeFile.write("include ../../common.mk\n")
    obemakeFile.close()

  #NOTE: root level Makefile, common.mk, and .qnx_internal.mk should all exist
  # in the root directory.
  #Now we need to construct a command line to invoke "make" while passing compiler
  #flags for version number and ip stack type
  command = "make"
  #change to the stage directory before compiling
  #command += " -C " + r'"' + "D:/sedona/baseline/pub/temp/platQnxPpc" + r'"' 
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
    print "svm not found"
    exit()

  #shutil.copy2('/dir/file.ext', '/new/dir') 
  fileutil.cpfile(os.path.join(stageDir,"ppc","o-be","svm"),os.path.join(stageDir,".par","svm","svm"),force=0)

  #print ""
  #print "!!!!! MANUAL STEP REQUIRED TO COMPLETE PROCESS !!!!!"
  #print ""
  #print "1) Use QNX IDE to build svm"
  #print "2) Copy svm to d:\\sedona\\baseline\\pub\\temp\\jace-qnx\\.par"
  #print "3) Invoke D:\\sedona\\baseline\\tridium\\adm>platArchive --db --svm --stage d:\\sedona\\baseline\\pub\\temp\\jace-qnx\\.par"    
  
  platArchive.main(["--db", "--stage", os.path.join(stageDir, ".par")])

# Main
if __name__ == '__main__':
  main(sys.argv[1:])

