#! /usr/bin/env python
#
# fileutil.py
#
#   File management utilities.
#
# Author:    Brian Frank
# Creation:  15 Mar 02
# 

import os
import zipfile
import re
import shutil

def ext(file):
  """ Return file extension or empty string """
  names = os.path.basename(file).split('.')
  if len(names) == 0:
    return ""
  else:
    return names[len(names)-1]  

def outofdate(frompath, topath):
  """ Return if the frompath has a later timestamp than topath """
  # automatic true if to doesn't exist
  if not os.path.exists(topath): return 1
  # check modified time
  fromtime = os.stat(frompath).st_mtime 
  totime = os.stat(topath).st_mtime
  return fromtime > totime

def mkdir(dirpath):
  """ Insure the specified directory exists """
  if not os.path.exists(dirpath): 
    os.makedirs(dirpath)

def excludematch(exclude, file):
  """ Check if file matches any excludes """
  for pattern in exclude:
    length = re.match(pattern, file)
    if length > 0: return 1
  return 0  

def cpfile(frompath, topath, force=0):
  """ Copy file contents """
  # bail if not out of date, unless the force flag is true
  if not force and not outofdate(frompath, topath): return
  # log
  print '    Copy "' + topath + '"'
  # get file handles
  fromfile = open(frompath, 'rb')
  tofile = open(topath, 'wb')
  # copy bytes
  while 1:
    buf = fromfile.read(4096)
    if not buf: break
    tofile.write(buf)
    
def cpdir(fromdir, todir, exclude=[], recurse=1, force=0):
  """ Recursively copy a directory - excludes are regular expressions """
  print '  Directory "' + todir + '"'
  if not os.path.exists(todir): os.makedirs(todir)
  for file in os.listdir(fromdir):  
    if excludematch(exclude, file): continue
    frompath = os.path.join(fromdir, file)
    topath   = os.path.join(todir, file)
    if os.path.isdir(frompath):
      if recurse: cpdir(frompath, topath, exclude, recurse, force)
    else:
      cpfile(frompath, topath, force)

def rmdir(path, exclude = [], log=1):
  """ Recursively remove a directory - excludes are regular expressions """
  if not os.path.exists(path): return
  if log: print '  Removing "' + path  + '"'
  files = os.listdir(path)
  for file in files:
    if excludematch(exclude, file): continue
    filepath = os.path.join(path, file)
    if os.path.isdir(filepath):
      rmdir(filepath, [], log)
    else:
      os.remove(filepath)
  if len(exclude) == 0: shutil.rmtree(path, True)

def rmsubdirs(path, exclude = [], log=1):
  """ Recursively remove a sub directories - excludes are regular expressions """
  if not os.path.exists(path): return
  files = os.listdir(path)
  for file in files:
    if excludematch(exclude, file): continue
    filepath = os.path.join(path, file)
    if os.path.isdir(filepath):
      rmdir(filepath, [], log)

def zip(zippath, sourcedir, prefixPath = ""):
  """ Zip the specified source dir to zipfile """
  # insure parent directory exists
  mkdir(os.path.dirname(zippath))
  # create compressed zip file
  zip = zipfile.ZipFile(zippath, "w", zipfile.ZIP_DEFLATED)
  # recursively add the source directory
  for file in os.listdir(sourcedir):
    zipadd(zip, os.path.join(sourcedir, file), prefixPath + file)
  # all done
  zip.close()
  
def zipadd(zip, path, archivepath):
  """ Add the specified dir or file to the zipfile """
  if os.path.isfile(path):
    print "  Zip " + archivepath
    zip.write(path, archivepath)
  else:
    for file in os.listdir(path):
      zipadd(zip, os.path.join(path, file), archivepath + "/" + file)
    
  
      
    
