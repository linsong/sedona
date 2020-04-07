
<!--
[//]: # (Copyright &#169; 2010 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    07 Jan 2010 - Matthew Giannini creation
) -->
# Platform Archive

## PAR file

A Platform Archive, or PAR file, provides a way of organizing various
files and metadata about a platform into a single entity. A PAR file is
a zip file with a `.par` extension. All paths and filenames within the
PAR file are reserved by the Sedona Framework, except the `vendor`
folder where vendor-specific files may be stored. The following
filenames and paths are currently defined by the Sedona Framework:

    <myplatform>.par/
                    /platformManifest.xml
                    /svm/<svm binary>
                    /vendor/*

-   **platformManifest.xml**: (required) Must be in the root of the PAR
    file. If the PAR file does not have a platformManifest.xml, then it
    is not a valid PAR file. Invalid PAR files will not be accepted by
    the sedonadev.org database. In addition, the manifest *must* set the
    `id` attribute to match the platform id.
-   **svm/<svm binary\>**: (optional) If you wish to package the svm
    binary for the platform, it should be placed in the `/svm`
    directory. It can have any name, as long as it does not have one of
    the scode or app extensions: `xml`, `scode`, `sax`, or `sab`. An
    scode and app file may also be included in this directory with the
    svm.
-   **vendor/**: (optional) The vendor may put any arbitrary contents
    in this location.
-   The PAR file may have any name, but it must have a `.par` extension.

The PAR file should be created as part of the vendor's build toolchain.
`sedonac` will facilitate this by staging the platform manifest in
`stageDir.par`, but ultimately each vendor must create the full
contents of the PAR. You should create a PAR file if you want
information about a platform to be available to Sedona Framework tools,
or if you want to submit your platform for Sedona Framework
certification. If a vendor is registered with sedonadev.org, the PAR
file can be uploaded to sedonadev.org so that tools can retrieve them
based on the platform id returned by the platform service running in the
app.

## Platform Database

When PAR files are stored locally, they are stored in the platform
database. The platform database resides on the local filesystem at

    sedona_home/platforms/db/

When a PAR file is stored in the platform database, it is unzipped and
stored in a directory called `.par`. The location of this directory is
based on the platform id specified in the platform manifest. The
platform database interprets every `'-'` character in the platform id as
a directory separator. For example, if the platform id is
`acme-basicPlatform-win32-1.0.38` the unzipped PAR file contents can be
found in the platform database at

    sedona_home/platforms/db/
      +- acme/
      |   +- basicPlatform/
      |   |   +- win32/
      |   |   |   +- .par/
      |   |   |   |   +- platformManifest.xml
      |   |   |   +- 1.0.37/
      |   |   |   +- 1.0.38/
      |   |   |   |   +- .par/
      |   |   |   |   |   +- platformManifest.xml
      |   |   |   |   |   +- svm/
      |   |   |   |   |   |   +- <svm binary>
      +- tridium/
      |    +- <etc>

You can use the `sedona_home/adm/platformdb.py` script to view and
administrate your local platform database.

When looking up a platform manifest or SVM for a given platform id, the
platform database will use a "best match" algorithm. It begins by
looking for a platform with the exact same id. If one cannot be found,
it "backs up" one directory level in the platform database and
searches for a platform manifest there. If one still cannot be found, it
backs up again until it hits the root directory of the platform
database, or finds a platform manifest.

For example, suppose the platform database looks like the example above.
If a Sedona Framework tool requests the platform manifest for a platform
with id `acme-basicPlatform-win32-1.0.39`, the

    sedona_home/platforms/db/acme/basicPlatform/win32/.par/platformManifest.xml

manifest is the best match.

!!! Note
    The Sedona Framework API contains a PlatformDb object in `sedona.jar` for working with the local platform database. See `sedona.PlatformDb`.
