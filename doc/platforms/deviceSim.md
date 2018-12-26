
<!--
[//]: # (Copyright &#169; 2013 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    05 Mar 2013   Elizabeth McKenney   Creation
) -->
[![Sedona](../logo.png)](/)
# Device Simulator

## Overview
A *Sedona Device Simulator* allows an app designed for a specific Sedona
platform to run on another platform. The Simulator consists of a
simulator Sedona VM plus a special scode file and platform manifest that
allow the app to run as if it were on the original device.

In a simulator Sedona VM, the native methods for the original platform
are replaced with functions compatible with the simulator platform. The
new functions may supply functionality equivalent to the originals, or
they may be stubs, depending on how the simulator SVM is built. The
degree to which a given Device Simulator will mimic the actual device is
largely a function of the effort level put into creating it.

-   **Example**: The native code for `DigitalIo.gpioGet()` in the SVM
    for a specific hardware platform reads an actual GPIO input on the
    device. In a *simulator* SVM designed to run apps for that platform
    on Windows, the `DigitalIo.gpioGet()` native table entry might
    simply point to a stub function that always returns 0. Or, it may
    point instead to an implementation that returns a calculated value
    that simulates a particular behavior. In either case the native
    function pointer is valid, and an app designed to run on the
    original platform should also run successfully on the simulator SVM.

The Sedona compiler `sedonac` provides some functionality to aid in the
creation of a simulator SVM. In particular, it can automatically create
empty stub functions for any native methods that have not already been
supplied by the developer. These source stubs can then be edited to add
the desired functionality, or can simply be left as-is to create a
limited (but usable) simulator SVM.

## Build Steps

The following instructions will help you create a Sedona device
simulator SVM for your own device. (It is usually convenient to bundle
these operations into a script that can be run as needed.)

1.  [Step 1](#create-platdef-file): Create a platform XML file for the simulator SVM
2.  [Step 2](#stage-source-files): Stage the Device Simulator SVM source files
3.  [Step 3](#build-simulator-svm): Build the simulator SVM
4.  [Step 4](#build-scode-file): Build scode for the Device Simulator
5.  [Step 5](#build-test-app): Include an app for the Device Simulator
6.  [Step 6](#package-and-install): Package and install the Device Simulator
7.  [Step 7](#run-simulator): Run the Device Simulator

## Create platDef file

The first step in creating a Device Simulator is setting up a simulator
version of the platform XML file. By convention, the new file should
have the same name as the original, with `-sim` inserted just before
the extension.

-   **Example**: the simulator version of `generic-unix.xml` would be
    `generic-unix-sim.xml`.

### Platform ID

Inside the new XML file, the platform ID should be modified in the same
way as the filename.

-   **Example**: `generic-unix-sim-${sedona.env.version}`

### Simulator platform compile attributes

Next, the attributes of the `<compile>` tag should modified to match the
simulator platform rather than the original target platform.

-   **Example**: For a Device Simulator that will run on Windows, set
    `endian="little"` and `blockSize="4"`.

### Native kits

The list of `<nativeKit>` tags requires no modification.

### Native source paths

The list of `<nativeSource>` tags must be modified to reflect the
location of the native method implementations for the simulator
platform. If any native methods are not supplied in the paths provided
here, sedonac will create empty stubs for them at staging time. This is
also where any simulator-specific native method implementations will be
included - just provide the path to the desired source code in a
`<nativeSource>` tag like the others.

-   **Example**: to create a Device Simulator to simulate a unix
    platform on Windows, modify the `<nativeSource>` tags as follows:

```xml
            <nativeSource path="/src/sys/native/unix" />
            <nativeSource path="/platforms/src/generic/unix/native" />
```
should become

```xml
            <nativeSource path="/src/sys/native/win32" />
            <nativeSource path="/platforms/src/generic/win32/native" />
```

If there is a native method for the original platform that **must**
return a specific value (or must **not** return 0), the developer can
provide source code for a substitute that returns the desired value. The
native source code file containing the substitute can be stored
anywhere, as long as there is a `<nativeSource>` tag in the sim platform
XML file that points to its location.

At the other end of the simplicity spectrum, it is also possible to
develop a more elaborate Device Simulator. For example one could provide
a graphical representation for digital outputs on the simulator
platform. The native code that generates the graphics, as well as the
native method implementations that control them, would similarly be
located and staged via one or more `<nativeSource>` tags.

!!! Tip
    If you are creating a device simulator for a device running Sedona 1.0 / TXS 1.1, native source must be provided for whatever methods the simulated platform uses to populate the `platformId`, `platformVer`, and `memAvailable` slots. These slots are often used by provisioning tools, and must have valid values for the tools to work. (Beginning with 1.2, there are native methods of `sys::PlatformService` so that `sedonac` can provide them automatically. Pre-1.2 however, they were unique to each platform.)

### Native patches

Any native patches required for the Simulator SVM need to be accounted
for in `<nativePatch>` tags. Note: The set of patches for the Device
Simulator SVM may not be the same as the set for the original platform.
In fact, since native patches are generally created only for SVMs that
cannot be rebuilt easily, it is unlikely any will be needed in a
simulator SVM.

### Manifest includes

If the platform definition file uses the `<manifestInclude>` tag, the
developer should judiciously consider whether it can remain as-is or
needs to be modified for the simulator SVM. In particular, depending on
how (or if) the original platform supports the Manifest Server feature
then it may need to be omitted or re-implemented for the simulator.


## Stage source files

When the platform XML file for the simulator SVM is complete, run
`sedonac` on the new file using the option `-stageSim`. Specify the
location to be used for the staging area via the `-outDir` option.

-   **Example**: `sedonac -stageSim -outDir temp generic-unix-sim.xml`

Sedonac will collect the files from all the `<nativeSource>` paths and
store them in the staging area, and then scan the source code to
identify any missing native method implementations. It will then create
empty stubs for the missing methods. It will also create a `.par` folder
in the staging directory to hold the platform archive (generated later),
and create the platform manifest file for the simulator platform.

-   **Example**: After running the above command, the directory `temp`
    has the following contents:

```shell
        win32> ls -A temp

        .par/                             inet_util_std.c                         sys_FileStore_std.c
        datetimeStd_DateTimeServiceStd.c  inet_util_std.h                         sys_File_std.c
        errorcodes.h                      main.c                                  sys_StdOutStream_std.c
        inet_Crypto_sha1.c                nativetable.c                           sys_Str.c
        inet_TcpServerSocket_std.c        platUnix_native_stubs.c                 sys_Sys.c
        inet_TcpSocket_std.c              platWin32_Win32PlatformService_win32.c  sys_Sys_std.c
        inet_UdpSocket_std.c              scode.h                                 sys_Sys_win32.c
        inet_sha1.c                       sedona.h                                sys_Test.c
        inet_sha1.h                       sedonaPlatform.h                        sys_Type.c
        inet_util.h                       sys_Component.c                         vm.c
```

And the subfolder `.par` contains:

```shell
        win32>ls -A temp/.par

        platformManifest.xml  svm/
```

At this time, the source code for the Simulator's SVM is ready for
building. If desired, any stub functions created by sedonac may be
edited now to add functionality.

!!! Tip
    If you are creating a device simulator for a device running Sedona 1.0 / TXS 1.1, you will need to use `sedonac.exe` and `sedonac.jar` from a 1.2 installation for this step, since the pre-1.2 versions do not have the simulator feature.

## Build simulator SVM

The next step is to compile the source code into an SVM executable. Use
any toolchain that is appropriate for the **simulator** platform. When
the executable has been created, copy it into the `.par/svm` folder
inside the staging folder.

## Build scode file

The Device Simulator requires a specially built scode file, so that the
scode will run on the simulator platform. To do this, simply copy a
basic scode XML file (such as one that is normally used for building
scode for the original platform) and change the platform-specific
parameters in the `<sedonaCode>` tag to match the **simulator**
platform. This is the same as what was done earlier for the platform
definition XML, e.g. to run on Windows set `endian="little"` and
`blockSize="4"`. The list of kits requires no modifications.

Run `sedonac` on the new scode file, and place it in `.par/svm` with the
SVM executable. (Name it `kits.scode` for later convenience.)

## Build test app

Finally, find or create a basic app with a schema that matches the scode
built in the previous step. It does not require any modification. Simply
build it and place the `.sab` file into `.par/svm` with the SVM and
scode files. (Name it `app.sab` for later convenience.)

## Package and install

Install the simulator platform into the platform database using the
`platArchive` tool, as follows:
` platArchive --stage [path to .par folder] --svm --db `

-   **Example**: `platArchive --stage temp/.par --svm --db`

This will also package the `.par` folder into a zip archive (in this example, the archive would be named `tridium-generic-unix-sim-1.2.2.par`), which is a convenient way to distribute the Device Simulator with its associated files.

## Run Simulator

To run the simulator SVM from the command line, simply navigate to the
`svm/` folder on the simulator host and run the SVM executable, e.g.

```shell
    svm>svm --plat kits.scode app.sab

    Sedona VM 2.1.28-sim
    buildDate: Jan 14 2013 10:01:34
    endian:    little
    blockSize: 4
    refSize:   4

    Running SVM in Platform Mode
    -- MESSAGE [sys::App] starting
    -- MESSAGE [sox::SoxService] started port=1876
    -- MESSAGE [sox::SoxService] DASP Discovery enabled
    -- MESSAGE [sys::App] running
```

Make sure you are running the SVM executable in the `svm/` folder, and
not the one in `{Sedona home}/bin`. (You may need to modify your path
variable to pick up the current directory first.)

The simulator SVM is now running on the simulator host. It is a real
Sedona VM and can be discovered and connected to by any Sox client on
the same network. Any Sedona app that was designed to run on the
original hardware platform should run successfully on this SVM as well.
The main difference is that any native methods that are stubbed out in
the simulator SVM will offer only their stubbed behavior to the app. The
simulator SVM will be as realistic as the implementations of its native
methods will allow.

!!! Note
    The `svm/` folder is probably *not* the best location for running the simulator SVM, since it is likely just a temporary directory. This example is just a way to demonstrate the simulator SVM functionality.
