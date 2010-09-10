#! /usr/bin/env python
#
# platArchive.py
#
#    Archive staged platforms into a zip file for upload to sedonadev.org.
#    Optionally, it can add the platform to the platform database.
#
# Author:    Matthew Giannini
# Creation:  05 June 09
#

import os
import getopt
import sys
import shutil
import xml.dom.minidom
import zipfile
import subprocess
import env

# Command-line Options
stageDir=""
outFile=""
doDb=False
doSvm=False

# usage
def usage():
  print("""
  platArchive [<opts>]
  
  Options:
  
  -s, --stage <stageDir> : 
    The directory that contains all the files for zipping into an archive.
    The default <stageDir> is <sedona_home>/temp/.par
    
    It should be organized into the following structure; all other
    files and directories will be ignored.
    
    <stageDir>/platformManifest.xml
              /svm/<svm executable> [requires --svm option]
              [/lib/*] [Optional - Future]
                          
  -o <outFile> : 
    Explicitly set the output file. The default output archive will be placed
    in <stageDir>/out/<platformId>.par
    
  --svm : Include the /svm directory in the archive
    
  --db : Add the platform archive into the local platform database.
    
  -h: Show this usage
""")
  
def fail(msg, showUsage=False, code=1):
  print
  print "Error: ", msg
  if showUsage: usage()
  sys.exit(code)
  
# parseOpts
def parseOpts(argv):
  global stageDir, outFile, doDb, doSvm
  stageDir = outFile = ""
  doDb = doSvm = False
  try:
    opts, args = getopt.getopt(argv, "hs:o:", ["stage=", "db", "svm"])
    for opt, arg in opts:
      if opt in ("-s", "--stage"):
        stageDir = str(arg)
      elif opt in ("-o"):
        outFile = str(arg)
      elif opt in ("--db"):
        doDb = True
      elif opt in ("--svm"):
        doSvm = True
      elif opt in ("-h"):
        usage()
        sys.exit(0)
    
    if not stageDir: stageDir = os.path.join(env.temp, ".par")
    if not os.path.isdir(stageDir): fail(stageDir + " is not a directory", True)
    
  except getopt.GetoptError, err:
    print str(err)
    usage()
    sys.exit(1)
  
def archive():
  global outFile, platformId
  
  manifest = os.path.join(stageDir, "platformManifest.xml")
  if not os.path.isfile(manifest): fail("Could not find platform manifest: " + manifest)
  svmDir = os.path.join(stageDir, "svm")
  
  xplatform = filter(lambda x:x.nodeName=="platformManifest", xml.dom.minidom.parse(manifest).childNodes)[0]
  platformId = xplatform.getAttribute("platformId")
  
  if not outFile:
    outFile = os.path.join(stageDir, "out", platformId+".par")
  outDir = os.path.dirname(outFile)
  if os.path.isdir(outDir): shutil.rmtree(outDir)
  os.makedirs(outDir)
  
  zip = zipfile.ZipFile(outFile, "w", zipfile.ZIP_DEFLATED)
  zip.write(manifest, "platformManifest.xml")
  if doSvm and os.path.isdir(svmDir) and len(os.listdir(svmDir)) == 1:
    svmFile = os.listdir(svmDir)[0]
    zip.write(os.path.join(os.path.join(svmDir, svmFile)), "svm/"+svmFile)
  zip.close()
  
  if (doDb): addToPlatformDb(platformId)

def addToPlatformDb(platformId):
  platDir = os.path.join(env.platforms, "db")
  for d in platformId.split("-"):
    platDir = os.path.join(platDir, d)
  platDir = os.path.join(platDir, ".par")
  cmd = "platformdb.py -i " + outFile
  if subprocess.call(cmd, shell=True, env=os.environ.copy()):
    raise Exception, "call failed: " + cmd
  
def main(argv=[]):
  parseOpts(argv)
  archive()
  
# Main
if __name__ == '__main__':
  main(sys.argv[1:])
  print "\nSuccess: ", outFile
