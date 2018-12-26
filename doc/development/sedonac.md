
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    15 Jul 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)

# Sedonac

## Overview

The sedonac tool is like a swiss army knife, it is used for the
following tasks:

-   [Compile Kit](#compile-kit): compile Sedona source code into a kit
    file.
-   [Compile Dir](#compile-dir): compile a group of kits
-   [Compile Code](#compile-code): compile a set of kits into an scode
    file
-   [Compile Platform](#compile-platform): stage the native code for a
    specific platform binary
-   [Compile App](#compile-app): convert between sax and sab formats
-   [Run](#run): run an arbitrary class within "sedona.jar" or
    "sedoanc.jar".
-   [Build Docs](#build-docs): auto-generate HTML documentation

## Compile Kit

Sedonac compiles Sedona source files into a kit file when run on a
"kit.xml" file (or a directory that contains a "kit.xml" file).
```xml
    <sedonaKit
     name = "serial"
     vendor = "Tridium"
     description = "Serial I/O support"
     includeSource = "true"
     doc = "true"
    >

      <!-- Dependencies -->
      <depend on="sys 1.0" />

      <!-- Source Directories -->
      <source dir="."/>
      <source dir="test" testonly="true"/>

      <!-- Natives -->
      <native qname="serial::SerialPort.doInit"   id="4::0" />
      <native qname="serial::SerialPort.doClose"  id="4::1" />
      <native qname="serial::SerialPort.doRead"   id="4::2" />
      <native qname="serial::SerialPort.doWrite"  id="4::3" />

    </sedonaKit>
```
Specification of elements and attributes:

**<sedonaKit\>** top level element:

-   **name**: (required) name of kit. See [Vendors](/deployment/kits#vendors)
    for rules on kit names.
-   **vendor**: (required) name of vendor for kit
-   **description**: (required) short description for kit
-   **includeSource**: (optional) boolean indicates whether to include
    source in kit zip. Defaults to false.
-   **doc**: (optional) boolean indicates whether to include this kit in
    API documentation. Defaults to false.
-   **version**: (optional) version string. Supports [variable substitution](#variable-substitution). See [versioning](/deployment/kits#versioning)

**<depend\>** specifies the dependencies of the kits

-   **on**: (required) [dependency](/deployment/kits#dependencies) as a kit name and version constraint.

**<source\>** specifies a directory of source code:

-   **dir**: (required) directory path where "kit.xml" is located,
    relative to top directory
-   **testonly**: (optional) boolean indicating whether the classes in
    the kit should be used in the test harness. If true, it infers the
    `@testonly` facet on all the classes in that directory. Default is
    false.

**<native\>** specifies a [native method](/language/nativeMethods#id)
identifier:

-   **qname**: qualified name of the native method
-   **id**: qualified id of the native method

## Compile Dir

If sedonac is run on a file containing a `sedonaDir` element (usually
named "dir.xml"), it compiles all the kits named in the list:
```xml
    <sedonaDir>
      <target name="sys" />
      <target name="inet" />
      <target name="sox" />
    </sedonaDir>
```
**<target\>** specifies a child target:

-   **name**: (required) name of child directory.

The kits will be compiled separately; to assemble them into an scode
image, see [Compile Code](#compile-code) below.

## Compile Code

If sedonac is run on a file containing a `sedonaCode` element, it
compiles the given set of kits into an scode image. The filename of the
input XML file is used for the output scode file, e.g. if the input file
is "foo.xml" sedonac will create a file called "foo.scode" in the
same directory.
```xml
    <sedonaCode
       endian="little"
       blockSize="4"
       refSize="4"
       main="sys::Sys.main"
       debug="true"
       test="true"
    >

      <depend on="sys 1.0" />
      <depend on="sox 1.0" />
      <depend on="inet 1.0" />

    </sedonaCode>
```
**<sedonaCode\>** top level element for scode compile:

-   **endian**: either "little" or "big" based on target processor.
-   **blockSize**: size of a scode block in bytes (see "sedona.h")
-   **refSize**: size of a memory pointer in bytes for target processor
    (4 for 32-bit processor).
-   **main**: qualified method name of main method (typically
    `sys::Sys.main`).
-   **debug**: (optional) boolean to include debug meta-data in image.
    Defaults to false
-   **test**: (optional) boolean to include test code in image. Defaults
    to false
-   **armDouble**: (optional) set to true if using an ARM microprocessor
    where 64-bit doubles are stored using byte level little endian, word
    level big endian.

**<depend\>** specifies the kits to compile into the image:

-   **on**: (required) [dependency](/deployment/kits#dependencies) as a kit name and version constraint.
-   **test**: (optional) boolean to include tests for this key. Defaults
    to value of `sedonaCode.test` if unspecified.

Note that the `test` and `debug` flags may have a significant impact on scode size.

## Compile Platform

Sedonac is used to *stage* the native code when compiling a binary image
for a given platform. This happens when sedonac is run against an XML
file with a `sedonaPlatform` root element. See
[Staging](/development/porting#staging) and [Platform Definition](/platforms/platDef)
for more details.

## Compile App

If you run sedonac against a file with a ".sax" extension it converts
the application to a ".sab" file, and vice versa. The output file is
placed in the same directory as the input file:

```shell
    D:\sedona\pub\apps>sedonac test.sax
      ConvertAppFile [D:\sedona\pub\apps\test.sax -> D:\sedona\pub\apps\test.sab]
      +----------------------------------
      |  RAM:     14.2kb (14588 bytes)
      |  FLASH:    0.4kb (382 bytes)
      +----------------------------------
```

Running sedonac on an application file also prints a memory impact
report. The RAM value is an estimate of how much memory the application
consumes in RAM during runtime. FLASH is the size required to persist
the application to durable storage like flash memory (always exactly the
same as the size of the sab file). Also see the [Apps](/apps/apps) and
[Memory](/language/memory) chapters.

## Run

If you pass a qualified Java classname to sedonac it will attempt to run
that class's main() method:
```shell
    D:\sedona>sedonac sedona.util.UserUtil brian pass
    User:   brian:pass
    Digest: 0x[ca4d1fd9a089ff9d50ab1f1dc4e4772a6b24c6bb]
    Base64: yk0f2aCJ/51Qqx8dxOR3Kmskxrs=
```
The class must be defined in "sedona.jar" or "sedonac.jar":

## Build Docs

The `-doc` switch builds HTML documentation along with the scode. For
example:
```shell
    D:\sedona>sedonac -doc pub/src/sys
      Parse [44 files]
      WriteKit [D:\sedona\pub\kits\sys\sys-b0ce639-1.0.36.kit]
      WriteDoc [D:\sedona\pub\doc\sys]
    *** Success! ***

    D:\sedona\sedonac -doc pub/doc/toc.xml
      TableOfContents [D:\sedona\pub\doc -> D:\sedona\pub\doc]
      CheckHtmlLinks [D:\sedona\pub\doc]
    *** Success! ***

    D:\sedona\sedonac
```
If the input file is a directory of kit source folders, then it will
build the kits and automatically generate HTML documentation for each
class, as well as a summary page for each kit. All fields and methods
for each class will be included in the documentation, and any `**` style
comments above a given field or method will be included with the entry
for that field or method.

If instead the input file to `sedonac -doc` is an XML file, it will be
processed for information on generating a table of contents. See the
file `doc/toc.xml` for an example.

## Variable Substitution

Some attributes in the various input XML files allow for variable
substitution. Such attributes will be read by sedonac, and any variables
present in the attribute text will be replaced by the variable value. A
variable is specified as `${variableName}`. For example:

`<sedonaPlatform vendor="Tridium" id="tridium-foo-${sedona.env.version}" />`

The `id` attribute above might end up being resolved as
` tridium-foo-1.0.38`. `Sedonac` will resolve variables according to the
following rules:

1.  If the variable starts with `os.env.` then sedonac will attempt to
    resolve an environment variable. For example, `${os.env.USER}` will
    cause sedonac to look in the environment for a variable called
    `USER`.
2.  If the variable starts with `sedona.env.` then sedonac will attempt
    to resolve a variable in the Sedona Framework environment (see
    sedona.Env.java). For example, `${sedona.env.buildVersion}` will
    cause sedonac to try and resolve property `buildVersion` in the
    sedona environment. Note: as a convenience `${sedona.env.version}`
    resolves to `sedona.Env.version`
3.  Otherwise, attempt to resolve the variable name against any
    variables that the compiler might set. For example, when compiling a
    platform definition file the compiler will set a variable called
    `${stage.nativeChecksum}`.
4.  It is always a compiler error if a variable cannot be resolved.

The most common place that variable substitution is used is in the
[platform definition](/platforms/platDef) file. The documentation for each XML
file will explicitly indicate which elements and attributes support
variable substitution (if any).
