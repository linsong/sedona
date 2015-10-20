The Sedona Framework is designed to make it easy to build smart, networked embedded devices. 

This is a git fork of Sedona Framework that is hosted using Mercurial. 

More details about the Sedona Framework at http://www.sedonadev.org.

[![Build Status](https://travis-ci.org/linsong/sedona.svg?branch=develop)](https://travis-ci.org/linsong/sedona.svg?branch=develop)

## How To Build
### Mac OS X 
1. make sure Xcode command line tools installed. you can run following command in terminal to install it:
```
 $ xcode-select --install
```
2. in terminal window, go to sedona's folder and initialize the build environment:
```
 $ cd /path/to/sedona/src && source adm/unix/init.sh 
```
3. in terminal window, start to build sedona zipball:
```
 $ python adm/makedist.py
```
