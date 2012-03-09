#! /usr/bin/env python
#
# props.py
#
#   Utilities for Java properties files
#
# Author:    Brian Frank
# Creation:  15 Mar 02
# 

def load(filename):
  """ Read a Java properties file into a Dictionary """
  props = {}
  file = open(filename, 'r')
  for line in file.readlines():
    line = line.strip()
    if (len(line) == 0 or line[0] == "#"): continue
    pair = line.split('=')
    if len(pair) < 2: continue  # hack until I support \ line continues
    props[pair[0]] = pair[1]
  return props

