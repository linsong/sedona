
<!--
[//]: # (Copyright &#169; 2010 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    07 Jan 2010 - Matthew Giannini creation
) -->
[![Sedona](../logo.png)](/)
# Platform Tutorial

## Introduction
A Sedona Framework platform is simply any device running an SVM. The
platform is uniquely identified by the ***platformId*** property of the
platform service running in the app on that device.

This section presents a high-level, step-by-step guide to creating a
Sedona Framework platform for your own device. We will use the Win32
platform provided in the open source distribution as a case study in
creating a new Sedona Framework platform.

1.  [Step 1:](#create-kit) Create a kit for your platform service
2.  [Step 2:](#implement-native-methods) Implement PlatformService native methods
3.  [Step 3:](#create-platDef-file) Create a platform definition file
4.  [Step 4:](#port-svm) Port the SVM
5.  [Step 5:](#build-svm) Build the SVM
6.  [Step 6:](#create-par-file) Create a PAR file
7.  [Step 7:](#test-platform) SAX and Scode setup for your platform

## Create Kit

Every app must have a `sys::PlatformService` component running in it. A
platform service encapsulates the behavior of that specific OS/Hardware
platform. The `sys::PlatformService` class itself is generic, so some
key methods do nothing. To implement a real Sedona platform, it must be
subclassed so that platform-specific behavior can be implemented.

The first step in porting Sedona to a new platform is the creation of a
PlatformService subclass for the platform. As an example, the win32
platform service is defined in a kit called "platWin32" and the kit
definition file is in `sedona_home/platforms/src/generic/win32/kit.xml`

## Implement Native Methods

The base class `sys::PlatformService` defines three native methods that
must be implemented for every platform. They are called by
`PlatformService` as needed to populate the corresponding properties.

-   `doPlatformId()`: returns platform ID string
-   `getPlatVersion()`: returns platform version string
-   `getNativeMemAvailable()`: returns number of bytes of physical
    memory available

The open source includes win32 natives as an example, see
`sedona_home/src/sys/native/win32`.

### doPlatformId()

One of the most important properties on the platform service is the
`platformId` property. This property uniquely identifies your platform,
and maps to a platform manifest stored in the platform database. The
native method `doPlatformId()` provides a means for Sedona to access
this native property.

The native implementation of `doPlatformId()` for the win32 platform is

    #include "sedona.h"
    #include "sedonaPlatform.h"
    #include <windows.h>

    // Str PlatformService.doPlatformId()
    Cell sys_PlatformService_doPlatformId(SedonaVM* vm, Cell* params)
    {
      Cell result;
      result.aval = PLATFORM_ID;
      return result;
    }

The value of `PLATFORM_ID` is defined in the header file
`sedonaPlatform.h`, which is generated automatically from the platform
definition XML file. So the next step is to create one.

## Create platDef file

Please refer to the section on [platform definition](/platforms/platDef) for
an in depth discussion of this file and all its sections and attributes.

The platform definition for the win32 platform is located in
`sedona_home/platforms/src/generic/win32/generic-win32.xml`. (This file
can be located anywhere, but the convention is to put it under
`sedona_home/platforms/src/`.)

```xml
    <sedonaPlatform vendor="Tridium" id="tridium-generic-win32-${sedona.env.version}" >

      <compile endian="little" blockSize="4" refSize="4" debug="true" test="true">

        <!-- Native Kits -->
        <nativeKit depend="sys 1.0" />
        <nativeKit depend="inet 1.0" />
        <nativeKit depend="datetimeStd 1.0" />
        <nativeKit depend="platWin32 1.0" />

        <!-- Native Sources -->
        <nativeSource path="/src/vm" />
        <nativeSource path="/src/sys/native" />
        <nativeSource path="/src/sys/native/std" />
        <nativeSource path="/src/sys/native/win32" />
        <nativeSource path="/src/inet/native" />
        <nativeSource path="/src/inet/native/std" />
        <nativeSource path="/src/inet/native/sha1" />
        <nativeSource path="/src/datetimeStd/native/std" />
        <nativeSource path="/platforms/src/generic/win32/native" />
      </compile>

    </sedonaPlatform>
```

There are a few things to note about this platform definition.

1.  It specifies the `id` attribute for the platform. Later, when we run
    `sedonac` on this file to stage the source, it will generate the
    header file `sedonaPlatform.h` based on this file. That header file
    will contain the `PLATFORM_ID` definition required by the
    [doPlatformId()](#doplatformid) native code.
2.  The SVM created from this platform definition will only support the
    native methods from the sys, inet, datetimeStd, and platWin32 kits
    because those are the only kits included in `<nativeKit>`
    declarations. In order to support other kits with native methods,
    they will have to be added to the platform definition.

## Port SVM

This process is already described in detail in the
[porting](/development/porting) section. We specified in the platform definition
file where all the native source code is located. If the native source
file locations change at any time during the platform development
process, the platform definition will need to be updated accordingly.

Refer to the win32 platform definition to see where all the native code
for the win32 SVM resides.

## Build SVM

Once the native source code is written and the platform definition file
is correct, we are ready to build the SVM. As described in the
[staging](/development/porting#staging) section, we use `sedonac` to stage all
native source files in one directory. Then we use the appropriate native
tool-chain to actually build the SVM.

The open source distribution includes a `makewinvm.py` script that will
build the win32 platform SVM. It runs `sedonac` on the generic-win32.xml
platform definition file to stage all the source code for the SVM, and
then it compiles the source code into a binary. This is the same
`svm.exe` that appears in the `bin/` directory of the open source
distribution. To run `makewinvm.py` you must first have [set up](/quickstart/setup) your environment for Windows 32-bit development.

You must provide your own native tool-chain to accomplish the same tasks
for a different platform.

## Create PAR file

Refer to the section on [PAR files](/platforms/par) for a more in-depth
discussion on PAR files.

When we staged the native source by running `sedonac` on the platform
definition file, it also created a `.par/` directory containing a valid
platformManifest.xml file for the platform. Therefore, all we need to do
is zip up the contents of that directory and use the platformdb.py
script to install it in our platform database.

The `makewinvm.py` script does the database installation step
automatically for the open source win32 platform. If you issue the
`platformdb.py --list` command you should see output similar to the
following

```shell
    sedona> platformdb.py --list
     tridium-generic-win32-1.0.46
```

This output indicates that the we have successfully installed the win32
platform into the platform database. The toolchain steps you perform for
another platform will need to do the same thing for that platform. You
can use the `platformdb -i <par file>` command to install a PAR file
into the platform database.

!!! Note
    The path within the platform database where the .par folder will be located **must** match the platform ID exactly. For example, the win32 platform definition file defines the platform ID as `tridium-generic-win32-${sedona.env.version}`. When we ran `sedonac` on the platform definition file it substituted an actual version number, so the resulting platform manifest file contains a specific definition, for example `tridium-generic-win32-1.2.29`. Sedona would then expect to find the manifest file for this platform in the platform database under `sedona_home/platforms/db/tridium/generic/win32/1.2.29/.par/`.

## Test Platform

To use your new platform service, you will need to create an application
(SAX) that uses your platform service. There is an example SAX file that
uses the Win32PlatformService at `sedona_home/apps/platWin32.sax`. You
can use this file as a template and make the following modifications to
use it for your platform:

1.  In the `<schema>` section, remove the platWin32 kit and add the kit
    for your platform service.
2.  In the `<app>` section there is a component called "plat". Change
    the type of that component from "platWin32::Win32PlatformService"
    to the type of your platform service.

After you have made these changes you can run `sedonac` on your new SAX
file to create an binary application (SAB) that your SVM can run.

Finally, you will need to create a scode image corresponding to the kits
in your SAX. There is an example scode configuration file in
`sedona_home/scode/platWin32.xml`. You can use this file as a template
and make the following modifications for your platform:

1.  Modify the `<sedonaCode>` elements to match the settings for your
    device. For example, make sure the blockSize, refSize, endian, etc.
    are correct.
2.  Remove the dependency on "platWin32 1.0" and add a dependency for
    the kit containing your platform service.

After you have made these changes you an run `sedonac` on your new scode
XML file to produce an scode image that your SVM can run.
