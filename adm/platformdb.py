#! /usr/bin/env python
#
# platformdb.py
# 
#  Wrapper to call sedona.platform.PlatformDb main in sedona.jar.
#
# Author:    Matthew Giannini
# Creation:  17 June 09
#

import sys
import os
import subprocess
import env

# Main
if __name__ == '__main__': 
  cmd = '"' + os.path.join(env.javaBin, "java") + '"'
  cmd += " -Dsedona.home=" + env.envHome
  cmd += " -cp " + env.sedonaJar
  cmd += " sedona.platform.PlatformDb "
  cmd += " ".join(sys.argv[1:])
  subprocess.call(cmd, shell=True)
