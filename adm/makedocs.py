#! /usr/bin/env python3
#
# makedocs.py
#
#    Runs the Sedona VM in current directory with supplied options (or defaults).
#
# Author:    Elizabeth McKenney
# Creation:  27 Oct 2011
#

from __future__ import print_function
import sys
import os
import subprocess
import optparse
import shutil
import fileutil
import env


#
# initParser
#   Initializes the options parser
#
def initParser():
  global parser

  parser = optparse.OptionParser(add_help_option=False, usage="""
  makedocs [opts]
""")

  parser.add_option("-h", "--help", action="help",
                    help="Show this help message and exit")


#
# usage
#  Print usage (e.g. on error condition)
#
def usage():
  global parser

  # Just print the help generated by the parser for --help
  print("")
  parser.parse_args( ["--help"] )



#
# Main
#
def main():
  global parser, options

  (options, args) = parser.parse_args()

  buildsrcdocs = env.sedonacExe + " -md -outDir " + env.build + " " + env.kits
  docOut = os.path.join(env.build, "doc")
  docAPI = os.path.join(env.build, "api")
  docAPIOut = os.path.join(docOut, "api")
  # copy static content in build/doc
  fileutil.cpdir(env.doc, docOut)
  buildpubdocs = env.sedonacExe + " -doc " + os.path.join(docOut, "toc.xml")

  #print "\n\n   Executing cmd = { " + cmd + " }\n\n"

  # Generate source (API) documentation
  if subprocess.call(buildsrcdocs, shell=True):
    raise Exception("\n *** Failed:\n" + buildsrcdocs)
  # move api content in build/doc/api
  fileutil.cpdir(docAPI, docAPIOut)
  shutil.rmtree(docAPI)
  # Generate static documentation (old-stype HTML pages)
  if subprocess.call(buildpubdocs, shell=True):
    raise Exception("\n *** Failed:\n" + buildpubdocs)
  # move api index in build/doc/api
  shutil.move(os.path.join(docOut, "api.html"), os.path.join(docAPIOut, "api.html"))
  shutil.move(os.path.join(docOut, "api.md"), os.path.join(docAPIOut, "api.md"))


#
# Main
#
if __name__ == '__main__':

  # Initialize the options parser
  initParser()

  # Call the main function
  main()
