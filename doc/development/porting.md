
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    30 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Porting

## Overview

The SVM is designed to be easily ported to new hardware and OS platforms
using the following steps:

1.  Add platform specific declarations to `sedona.h`
2.  Select the kits with native methods that you plan to support
3.  Write custom implementations for native methods where needed
4.  Write bootstrap code to start the SVM
5.  Stage the VM code and platform manifest using an XML build script
6.  Compile the C code using your platform's C compiler
7.  Run the test suite to verify a successful port

## sedona.h

Porting to a new target platform begins with `sedona.h` which is located
in the `src/vm` directory. This file is included by every native source
file, and contains essential definitions required for building the
native layer. It contains sections for major target platforms such as
Win32, QNX, and UNIX. Each platform's section is wrapped by an
`#if defined()` directive for the given target compiler/platform. If
none of the existing sections is matched, the preprocessor will look for
a file named `sedona-local.h` in the include path, and include its
contents in place of the other target sections.

Definitions for a new platform can be added in a new `#elif defined()`
section in `sedona.h`, but the modified `sedona.h` will then need to be
updated manually if any changes are made to the public version of the
file. A simpler solution is to create a local `sedona-local.h` file to
hold the definitions for the new platform. This file will then
automatically be included in the build and can be maintained separately
from the core Sedona Framework distribution.

There are instructions at the top of `sedona.h` that describe the types
and macros that must be defined for each platform, such as the ANSI C 99
integer types and macros for endianness and block sizes. Definitions for
key types like `Cell` and `SedonaVM` are defined at the bottom of the
file, as well as the function declarations for working with the VM.

## Natives

Kits without native methods require no changes to run on a new target
platform. For each kit with native methods, the existing code must first
be examined to see what additional work needs to be done.

As described in the [Native Methods](/language/nativeMethods) chapter, every
native method must have an appropriately-named C function that
implements that method for the target platform. However some C functions
are more portable across platforms than others. For example a native
method like `sys::Sys.copy` may be written in ANSI C and shared by many
or perhaps all platforms. Other methods, such as `sys::Sys.ticks`,
almost always require a custom implementation for each hardware or OS
platform.

All the native source code is organized under a directory called
`native` in the kit directory. Native functions that are portable across
*all* platforms should be contained in C files located directly in the
`native` directory, one file per class using the naming convention
"*{kit}\_{class}.c*". Any native functions that are implemented
separately for each platform should be located in sub-directories under
`native`, one per platform. Code files under these directories are named
using the convention "*{kit}\_{type}\_{platform}.c*". This helps avoid
file name collisions.

For example, given a kit `myKit` with one class `MyClass` that has
native functions, some portable and some platform-specific, the code
would be organized as follows:

```
myKit/
  +- kit.xml
  +- MyClass.sedona
  +- native/
  |    +- myKit_MyClass.c
  |    +- qnx/
  |    |    +- myKit_MyClass_qnx.c
  |    +- win32/
  |    |    +- myKit_MyClass_win32.c
  +- test/
  |    +- MyClassTest.sedona
```

All the source code files for the native methods are stored under the
folder `myKit/native`. Functions that can be shared across all platforms
are in the file named `myKit_MyClass.c`, located in the `native` folder.
These functions should not need to be implemented again when porting to
a new platform.

Source files for platform-specific implementations are located in a
separate subfolder for each platform, and the platform name is appended
to the source file name. When porting the kit to a new platform
`newPlat`, simply create a new folder `native/newPlat` and put the
native method implementations into a source file
`myKit_MyClass_newPlat.c` under the new folder.

## Bootstrap

In addition to the native methods for each kit, the new platform port
will need some native bootstrapping code to start the SVM. For
sophisticated devices with an OS and a file system (such as a PC), it
may be sufficient to build and run the code provided in `main.c`, which
is located in the `src/vm` directory. This function can be executed from
the OS command line, providing on the command line the filenames for the
scode image and the app to be run. (Even with platforms that cannot
support `main.c`, it may still be useful as a guide for writing the new
bootstrap code.)

On smaller, simpler platforms, new bootstrap code will need to be
written. Every platform will have a unique implementation, but at some
point the SVM will need to be started by calling `vmRun(SedonaVM*)`.
(Both `vmRun()` and the `SedonaVM` struct are defined in `sedona.h`.)
Before calling `vmRun()`, a SedonaVM struct must be created and then
initialized as follows:

1.  Configure `codeBaseAddr` and `codeSize` to point to the scode image.
    Typically the scode is stored in a disk file or in flash memory, and
    loaded into RAM at this point. If so then `codeBaseAddr` is simply a
    pointer to the scode image in RAM.
2.  Configure `stackBaseAddr` and `stackMaxSize` to point to an area of
    RAM that can be used for the stack. Most commonly this is a static
    or dynamically-allocated array of the desired size.
3.  Configure `args` and `argsLen` to pass in the arguments to the
    Sedona Framework main method. The `args` pointer should reference a
    normal array of C null terminated strings (just like a standard C
    main signature).
4.  Configure a callback function for `onAssertFailure`. This function
    is generally used in test code, and is called whenever an assert
    condition fails. More details on using the Sedona Framework test
    facility may be found in the [Test](/development/testing) chapter.
5.  Configure the `call` function pointer to point to `vmCall`. This
    indirection provides a hook for patching a ROM based VM.
6.  Configure a pointer to the `nativeTable` array generated
    automatically during the VM code generation stage. See the [Native Methods](/language/nativeMethods) chapter for more information.

When the `SedonaVM` struct has been initialized, start the SVM by
calling `vmRun()` and passing a pointer to the struct.

### Sedona VM Exit Values

When the Sedona VM exits, the calling function must decide what to do
next based on the return code.

If the VM exits normally with a return code of `0`, it generally means
the VM is not expecting to restart. The most common use of this is when
running the test harness. If so, then the global variables
`assertSuccesses` and `assertFailures` will contain the number of calls
to `assert` that passed and failed, respectively.

When the action `App.restart()` is invoked, the app stops running and
sets the return code to `Err.restart`. On most platforms this leads to
the SVM exiting with the value `ERR_RESTART`. If this value is received
then the calling function should repeat the above steps to restart the
SVM (possibly using different `app.sab` or `kits.scode` files if the
device is restarting after being [provisioned](/development/niagara#provisioning-from-niagara)). Note
however that `App.restart()` also calls the platform service's
`restart()` method, which could produce different behavior depending on
how that method is implemented. For example, on a device with no OS it
might issue a soft restart to the CPU, in which case there would be no
return code to process.

Some platforms may require the Sedona VM to yield execution control to
allow other tasks to run. When `vmRun()` returns `ERR_YIELD`, the
application is yielding CPU control. See the [Yield](/apps/apps#yield)
section for more details.

Each platform requires the implementation of an appropriate hibernation
strategy. When `vmRun()` returns `ERR_HIBERNATE` then the application
has requested that the platform go into a low-power or sleep state.
Awakening from this state is under the control of the device, not the
Sedona Framework application. Upon awaking, call the `vmResume()`
function to start the SVM again from where it left off. See the
[Hibernation](/apps/apps#hibernation) section for more details.

If `vmRun()` returns a non-zero value *other* than `ERR_RESTART`,
`ERR_HIBERNATE`, or `ERR_YIELD`, then there is a problem. Error codes
generated in the scode image or the app are defined in
`src/sys/Err.sedona`; errors from the VM itself are usually indicated by
an error code in `src/vm/errorcodes.h`.

## Staging

Once the native code has been implemented, the next step is to *stage*
the VM and native code. This copies the relevant native source files
into a single directory in order to compile them using the appropriate
native toolchain.

Staging is accomplished by running `sedonac` with a [platform definition](/platforms/platDef#platform-definition-xml) file and specifying the output
directory for the staged files. The platform definition lists all the
directories containing C source files required to build the SVM for a
target platform. (The platform toolchain may add additional non-Sedona
files when it builds the final executable). See the [platform definition](/platforms/platDef) section for more details and an example
platform definition file.

The platform definition file supplies some basic information about the
platform, then lists the kits with native code that will be supported by
the VM. Kits without native methods do not need to be mentioned in the
platform XML. Finally it identifies the path to each directory
containing native code that will be required by the VM at runtime. This
includes not only the relevant Sedona native methods implementations,
but also the source for the SVM itself. (On platforms where some portion
of the VM is in ROM, only the RAM-based code may need to be included
here.)

Staging is performed by calling `sedonac` with the platform definition
file, specifying the target directory via the `-outDir` option. For
example,

```shell
$  sedonac platforms/src/acme/basicPlatform-win32.xml -outDir tempStageDir
```

This results in the following:

-   All existing files are removed from the folder `tempStageDir`
-   All source files are copied from the listed paths into
    `tempStageDir`
-   The file `nativetable.c` is generated, which defines the native
    function [lookup table](/language/nativeMethods#native-tables)
-   If the platform id is known at staging time, a `sedonaPlatform.h`
    file is created, which contains the `PLATFORM_ID` macro.
-   The [platform manifest](/platforms/platDef#platform-manifest) is staged in
    `tempStageDir/.par/platformManifest.xml`. The toolchain can later
    add the SVM binary to into the `.par/svm/` directory (if desired)
    and then generate a [PAR file](/platforms/par#par-file). The PAR file could be
    uploaded to [sedonadev.org](http://sedonadev.org) or installed in
    the local [platform database](/platforms/par#platform-database).

    !!! Note
        Given that Tridium no longer supports Sedona, the par file can no longer be uploaded to _sedonadev.org_.

## Wrap Up

Note that `sedonac` does not build the C code, it simply assembles the
files together into a single directory. The C code must then be built
with the appropriate toolchain for the target platform.

Neither does `sedonac` generate an actual PAR file for you. It will help
by staging a basic `.par/` directory containing the platform manifest.
However, the toolchain build process will need to include steps/scripts
to generate the PAR file for local and/or public use.

Once the VM executable has been built, a good next step is to run the
[test harness](/development/testing) and verify the port was successful.
