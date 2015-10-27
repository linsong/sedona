# Sedona [![Build Status](https://travis-ci.org/linsong/sedona.svg?branch=master)](https://travis-ci.org/linsong/sedona)

The Sedona Framework is designed to make it easy to build smart, networked embedded devices. 

This is a git fork of Sedona Framework that is hosted using Mercurial, our goal is to enhance sedona platform to be more powerful and easy to be used.

More details about the Sedona Framework at http://www.sedonadev.org.

***

## Major Improvements/Features
* Remove jikes dependency for POSIX OS platforms
* Wireshark protocol dissector for Sox/Dasp [More Info](./tools/README.md)
* Linux Docker image [More Info](./tools/README.md)
* Fix build issues under Mac OS X

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

### Linux
We build a shell script that uses docker to build sedona under linux, read [here](./tools/README.md) for more details.

