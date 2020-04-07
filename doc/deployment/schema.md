
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    17 May 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Schema

## Overview

Because the Sedona Framework is designed to work in very constrained
embedded environments, we have to make design tradeoffs. One of the
biggest tradeoffs is using a low level binary format for Sedona
Framework application files and the Sox protocol. This compact form
requires out-of-band information for encoding and decoding.

For example the binary format does not contain kit, type, or slot definitions, only their numeric ids. So in order to reconstruct the
original app from its binary SAB file, we must have some external way to
map those ids to the right definitions. Correct versioning is also
important, since kits may evolve over time to include new types and new
slots which could change their ids. The schema tells us how to create
the right mapping for the app, by specifying the kit versions and
checksums that were used to create the app.

## Kit Parts

Every kit contains zero or more named types. Each type declares zero or
more slots. For a given version of a kit, there is a fixed list of types
and their declared slots. Each slot has a fixed name, flags, and type.
When a kit is compiled from source into a kit zip file, the compiler
generates a *checksum* for this fixed list of types and slots. The
combination of a kit name and checksum is called a *kit part*.

The kit meta-data, checksum, and list of types and slots is included in
the kit file as an XML file called "manifest.xml". Here is an example
manifest file:

```xml
    <?xml version='1.0'?>
    <kitManifest
       name="sysTest"
       checksum="da52f78f"
       version="1.0"
       vendor="Tridium"
       description="Test suite for core language and sys APIs"
       buildHost="BLAZE"
       buildTime="2007-05-17T16:21:08.030-04:00"
    >

    <type id="0" name="AbstractTestComp" base="sys::Component">
      <slot id="0" name="az" type="bool"/>
      <slot id="1" name="ai" type="int"/>
    </type>

    <type id="1" name="TestComp" base="sysTest::AbstractTestComp">
      <slot id="0" name="z1" type="bool"/>
      <slot id="1" name="b1" type="byte"/>
      <slot id="2" name="addF1" type="float" flags="a"/>
    </type>

    <type id="2" name="SubTestComp" base="sysTest::TestComp">
      <slot id="0" name="sb" type="byte"/>
      <slot id="1" name="si" type="int"/>
    </type>

    </kitManifest>
```

The checksum is based only on the kit's component types (those that
subclass `sys::Component`) and slots. The checksum is not based on
variable meta-data such as the kit's version number, or on
non-component types which could never appear in an app definition. This
means that multiple versions of the same kit might share the same
checksum if no component types or slots have been modified between
versions.

Don't confuse **version** with **checksum**. Version represents a
revision of the whole kit including its code, algorithms, and when it
was built; the version only changes when the developer *chooses* to
specify a different version number. Checksum represents a revision of
the declared types and slots, and it changes *automatically* whenever
the type or slot definitions change.

## Kit Database

Within a Sedona Framework installation we store all the local copies of
kits with the file pattern:
`{Sedona home}/kits/{kit}/{kit}-{checksum}-{version}.kit` For example:

    {Sedona home}/
      kits/
        control/
          control-cdf5f0f0-1.0.28.kit
          control-cdf5f0f0-1.0.29.kit
          control-1239c0de-1.0.29.kit

        sys/
          sys-ef94b11d-1.0.28.kit
        .
        .
        .

We call this directory the *kit database*. It can store multiple
versions of each kit with different checksums. The `sedona.kit.*` Java
APIs can be used to work with the kit database.

## Manifest Database

Kit files contain all the information we need when working with schema.
However, to interface with a device that is using a kit we don't need
the full kit file, only the XML manifest file that represents it. So in
addition to the kit database, we also create a *manifest database* with
the file pattern: `{Sedona home}/manifests/{kit}/{kit}-{checksum}.xml`
For example:

    {Sedona home}/
      manifests/
        control/
          control-cdf5f0f0.xml
          control-1239c0de.xml

        sys/
          sys-ef94b11d.xml
        .
        .
        .

By storing the manifests in a separate database, we don't need to use
the kit files themselves to work with the kits' types. This is
typically more efficient, and in addition it allows a vendor to publish
just the manifests for their kits as opposed to entire kit files.

Note, however, that manifests cannot be used to create an SCode image.
Building an SCode image requires access to the actual kit files.

The `sedona.manifest.*` APIs are used to work with manifests and the
manifest database via the Java toolkit. The `KitManifest` class
represents the information stored a kit manifest file and provides
methods for encoding and decoding from XML. The `ManifestDb` class is
used to load and save `KitManifest`s to the file system.

## Schemas

A Sedona Framework application is composed of multiple kits. A specific
list of kit parts (kits at a specific checksum revision) is called a
*schema*. Matching schemas guarantee compatibility between the app
running on the device and the app's representation elsewhere, such as
in a PC-based tool.

Only when a kit manifest is accessed for a specific schema can we
correctly interpret binary information such as kit id and slot id. For
example the kit id for the "control" kit might be 3 in one schema, but
5 in another schema that has a different list of kit parts. Slot ids for
a given kit part can also change across schemas, if there are changes to
slots inherited from base types.

The Java API for working with schemas is in `sedona.*`. Schemas are
built with the `sedona.Schema` class from a list of `KitParts` which are
resolved against the manifest database. Assuming all the kit parts can
be resolved to kit manifests, we can build a complete representation of
the schema including the full list of kits, types, and slots along with
their respective ids. The `Schema` is then used to work with binary
formats such as ".sab" files and Sox messages.

## Kits versus Manifests

So when do you need a kit and when do you only need a manifest? This
table helps summarize the differences:

 | What     | Encapsulates         | Versioned By   | Uses |
 |----------|----------------------|----------------|--------------------------|
 | Manifest | type and slot schema | checksum       | sax, sab, and sox |
 | Kit      | code                 | version number | compiling dependencies and scode images |

For example when working with an application file or Sox, only the type
schemas are needed. No knowledge of the internal code is required.
However when compiling, you need the full kit that contains the actual
code. So a manifest is somewhat analogous to a C header file, which
declares function prototypes but does not contain any code for the
functions.

Manifests are versioned with a checksum each time a type or slot
definition is modified. Kits are versioned with a version number
typically whenever code is modified.

## Resolving Manifests

To fully connect to a remote Sedona device, there must be local copies
of the kit manifests corresponding to the device's current schema. The
recommended rules for locating each kit manifest are:

1.  Check the local manifest database first; if the manifest is found
    then use it
2.  Check the local kit database for the given kit, if found then
    extract the manifest file and store it in the local manifest
    database
3.  Download the manifest from sedonadev.org to the local manifest
    database

These steps are automatically implemented by the Sedona `ManifestDb`
API. If any manifests required by the schema are not found a
`MissingKitManifestException` is thrown, which contains information
about the specific manifests that are missing. This information can be
used to compose a friendly error message to the user about why the
connection failed, or to request the missing file(s) from the device
itself (see next section).

## Kit Manifest Server

Beginning with Sedona 1.2, the Sedona Sox Client supports retrieving
missing manifest files from the remote Sedona device itself. If it
detects the `MissingKitManifestException` when resolving the device's
schema locally, it requests the missing manifests from the device. If
the device is set up to serve the requested manifests it will send them.
This feature can make it easier to deploy new devices, as they can be
shipped with the necessary manifests installed on the device rather than
requiring them to be installed separately in advance on the Sox Client
host.

Serving kit manifests is accomplished very simply, using a special
filename prefix called a *scheme* to indicate to the device where to
look for the file in its local file system. This allows the manifest
retrieval to be accomplished using the regular Sox file transfer
protocol. When the Sox Client detects that a manifest is not found by
the usual rules described above, it sends a Sox "get file" request to
the device using the filename of the missing manifest adding the
appropriate scheme prefix. When the device receives the request, it
recognizes by the prefix that this is a manifest file, and looks in its
local database for the one requested. If the file is found, it sends it
via Sox back to the requesting client.

### Implementation Details

To make this feature work on a given Sedona platform, the following
things must be true:

-   The required manifest file(s) must be stored on the device
-   The "**m:**" scheme must be implemented on the remote Sedona
    device (see below)
-   The `sedonadev.autodownload` property in sedona.properties must be
    set to false, or omitted entirely

A *scheme* is simply a prefix to the filename, such as "m:", which is
translated on the remote device into a local path where the file should
be located. There are two ways to implement a scheme:

1.  It can be implemented entirely at the native level, encapsulated in
    the appropriate `sys::FileStore` natives
2.  It can be implemented at the Sedona level using a platform-specific
    FileStore subclass.

If it is implemented at the native level, then at the Sedona level the
device treats the prefix as part of the actual filename. The native
implementation of `FileStore.doOpen()` must then strip off the prefix
and substitute the path to the local manifest database. The resulting
full path to the file is then passed to the local file system and
handled just like any other file transfer request.

If it is implemented using a FileStore subclass, then the FileStore
subclass should override the `accept()` method such that it returns true
if the filename begins with the desired prefix. Then the `open()` method
can be overridden to strip off the scheme prefix and substitute the
desired path, or the FileStore subclass may have an additional native
method that provides the appropriate path for all files in that scheme.

The Sedona 1.2 open source includes a class in the 'win32' platform
kit, called `Win32ProvDbFileStore`. This class demonstrates the second
(Sedona-level) implementation strategy described above.

N.B. An ambitious Sedona developer could use this same strategy to
provide kit files and/or platform manifests as well as kit manifests.

## Review

To summarize the schema pipeline:

1.  **Checksum**: Generated by the compiler when compiling a kit file
    from source, the checksum is based on the declared types and slots
    in the kit.
2.  **Kit Part**: The combination of kit name and kit checksum that
    uniquely identifies a kit for a specific schema revision.
3.  **Kit Manifest**: A file containing a kit's checksum and type
    definitions, stored as a zip archive entry named "manifest.xml".
4.  **Kit Database**: A local database of kit versions, created and
    maintained by the Sedona Framework Java toolkit.
5.  **Manifest Database**: A local database of all the kit manifests
    that have been accumulated, created, and maintained by the Sedona
    Framework Java toolkit. These manifests are XML files keyed by kit
    name and checksum.
6.  **Schema**: A list of kit parts aggregated by the Sedona Framework
    Java toolkit. A schema defines the full meta-data required to work
    with binary format entities with matching schemas. Each Sedona
    Framework runtime and application file has a single, fixed schema.
    The Java toolkit models schemas via the `sedona.Schema` API.
