<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    7 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Setup

## Overview

The root directory of your Sedona Framework installation or development
environment is referred to as _Sedona home_.  
Relative paths in this documentation always start from the Sedona home directory.
Sedona home is organized into the following sub directories:

- **adm**: administration scripts used to build and test
- **apps**: Sedona application files (.sab and .sax formats)
- **bin**: Win32 binaries and some Unix shell scripts
- **doc**: documentation
- **kits**: directory for kit database
- **lib**: Java jar files and properties files
- **manifests**: directory for manifest database
- **platforms**: directory for platform database; also contains source files for Sedona Framework platforms
- **scode**: scode images, and linker files for creating them
- **src**: directory tree for Java, C, and Sedona source code

## Sedonac
Sedona Framework development primarily centers around the tool `sedonac`.
It functions as a compiler for Sedona applications as well as Sedona kits (library
modules); its behavior depends on the input file it is processing.

### Windows
For Windows you will be using the <code>sedonac.exe</code> launcher executable,
located in the <code>bin</code> directory.  
If you are developing at the command line, make sure that <code>bin</code>
is in your path.

The compiler requires a Java Runtime of 1.4 or greater.  
<code>sedonac</code> will look in the registry to find
the path to your current Java VM.  For more details about how
<code>sedonac</code> starts up, use the <code>--v</code> command
line switch (that's two dashes before the 'v'), which
will trace the registry access and jvm.dll load.

To verify <code>sedonac</code> is correctly installed, run with the `-ver` switch:

```shell
D:\sedona>sedonac -ver
Sedona Compiler 1.2.28
Copyright (c) 2007-2013 Tridium, Inc.
sedona.version = 1.2.28
sedona.home    = D:\sedona
java.home      = C:\Program Files (x86)\Java\jre7
java.version   = 1.7.0_13
```

### Unix

There is a bash shell script in <code>adm/unix</code> called <code>init.sh</code>
that initializes your Sedona Framework development environment.
In order to use this script, you must first


1. export JAVA_HOME in your login script
2. create a symbolic link in your home directory called `sedonadev` that
points to your actual _Sedona home_ directory. For example,
`~/sedonadev > ~/repos/sedona-1.0/pub`
If you don't want to use this symbolic link, edit <code>init.sh</code> to explicitly set
the <code>sedona_home</code> environment variable

After doing the above configuration, change your login script to run <code>init.sh</code>.
This script will make sure some key programs are in your path, check
that all the python scripts have executable permissions, and create
some useful aliases.

There are two Unix specific commands that you should use for building in a
Unix environment:

- **makeunixvm.py**: Compiles the Sedona VM (SVM) for Unix using gcc.
Run <code>makeunixvm -h</code> for more details

- **makeunixdev.py**: Builds sedona.jar, sedonac.jar, all opensource kits, and makes the Unix SVM

### Java Command Line

If you need to invoke Sedonac directly you can launch it directly
with Java:

- Put <code>lib/sedona.jar</code> and <code>lib/sedonac.jar</code> in the classpath
- Pass the installation directory to the "sedona.home" system property
- Run the "sedonac.Main" class with the desired arguments

For example:

```shell
java -cp {lib}sedona.jar:{lib}sedonac.jar -Dsedona.home={home} sedonac.Main src/sox
```
## Environment

You can rebuild from source using the python scripts in the <code>adm</code> directory (or <code>adm/unix</code>). These scripts are used to rebuild the compiler itself and the SVM. In order to use the python scripts, you will need to install Python (version 2.7 or better).

Also the following environment variables must be defined:

- **sedona_home**: directory of your Sedona Framework installation (the parent directory of adm, bin, lib, etc)

- **java_home**: directory of the Java JDK (1.4 or later).  
Note: the scripts use <code>adm/jikes.exe</code> to compile Java code,
but still require the JDK for <code>bin/jar.exe</code> and <code>jre/lib/rt.jar</code>

- **win_sdk**: if you wish to compile the VM using the Visual Studio C compiler then <code>win_sdk</code> should reference your Windows development kit.  Also make sure you have run the <code>vcvars32.bat</code> script included in your Visual Studio installation.  Many users can skip this step, though, and just use the pre-built Win32 <code>svm.exe</code> provided in the <code>bin</code> directory

To verify your environment you can run the <code>adm/env.py</code> script, which will print all the files and directories being used with your current setup.

## Common commands

The following commands are commonly used when building and running the Sedona Framework:

| Command | Outcome |
|---------|---------|
|<b>makesedona.py</b> | compile sedona runtime Java source into sedona.jar |
|<b>makesedonac.py</b>| compile sedonac Java source into sedonac.jar (depends on sedona.jar) |
|<b>makewinvm.py</b> | recompile svm.exe for Win32 using Visual Studio compiler |
|`sedonac src\zoo\kit.xml` | compile Sedona source for kit <code>zoo</code> into a kit file (you can also specify just <b>src\zoo</b>) |
|`sedonac scode\zdevice.xml` | link Sedona kits specified by zdevice.xml into zdevice.scode image file |
|`sedonac apps\bar.sax` | compile a SAX (application XML) file into a SAB (binary) file |
|`svm scode\zdevice.scode apps\bar.sab` | run Win32 SVM with the specified scode and app file |
|`svm scode\zdevice.scode -test` | run test suite with the Win32 SVM and the specified scode file |
| `svm --plat` | For the open source Win32 and Unix SVM implementations, this allows you to run the SVM in <i>platform mode</i>.  Running in platform mode allows the SVM to handle restart commands, and it will handle loading staged scode and sab files. The SVM must be running in platform mode to pass the [certification test suite](/platforms/platCertified/)
| **makedev.py** | recompile all the commonly used targets and run tests with the Win32 SVM |

!!! Note
    Any command that builds the SVM will need to have the appropriate target toolchain set up first.
