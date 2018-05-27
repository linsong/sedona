[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    7 Mar 07  Brian Frank  Creation
)
# Change Log

![Sedona](doc/logo.png "Sedona Framework Logo")

### Build 1.2.29 (29 Nov 2015)
This release is development improvement release, although there is nothing new added to the Sedona framework, but we did add following improvements that will help developers to build/develop Sedona easier.

Changes:
* Remove jikes dependency for POSIX OS platforms
* Wireshark protocol dissector for Sox/Dasp [More Info](./tools/README.md)
* Linux Docker images(centos & ubuntu) [More Info](./tools/README.md)
* Mac OS X build support
* Testing in non-Windows environment
* Code structure cleanups
* new svm test command line option to run tests matching a filter string

### Build 1.2.28 (26 Mar 2013)
  - Kit and Platform Manifest Server
  - Device Discovery
  - Multi-rate App Components
  - Device Simulation
  - Refactored Control Kit
  - Prioritized Array Components

### Build 1.1 (skipped)

### Build 1.0.48 (18 Oct 10)
  - logManager kit: allows persisting log levels on the device without
    requiring the web kit to be installed.
  - fix inline Buf compiler bug
  - Expose more override points in DASP api
  - fix logLevels sizing in scode bug
  - other enhancements and bug fixes (see hg.sedonadev.org)

### Build 1.0.47 (09 Jun 10)
  - fix DASP ackMore header bug (sox kit and SoxClient)
  - changes to support yield in the SVM and App
  - other bug fixes (see hg.sedonadev.org)

### Build 1.0.46 (17 Mar 10)
  - generic unix platform
  - add steady-state to App
  - platform certification in `soxcert`
  - various other enhancements and bug fixes (see hg.sedonadev.org)

### Build 1.0.45 (07 Dec 10)
  - sedonac enhancements: definite assignment, dead code, unused types/methods
  - generic svm enhancements: run svm in `"platform"` mode
  - version the sox protocol and change sox subscribe to support batch
    subscription. See docs for SOX
  - fix a number of `sedonac` bugs (see HG repository for changelogs)

### Build 1.0.44 (12 Oct 09)
  - reserve `function` as a keyword
  - add support for qualified names to Sedona grammar (type disambiguation),
    and other compiler grammar refinements and fixes
  - various enhancements and bug fixes

### Build 1.0.43 (30 July 09)
  - Various bug fixes

### Build 1.0.42 (skipped)

### Build 1.0.41 (21 July 09)
  - Sox enhancements for Chopan
  - Weblet enhancements
  - Various bug fixes

### Build 1.0.40 (24 June 09)
  - New `sedonacert` Java module
  - Add Context parameter to all the Java native methods
  - Platform Redesign

### Build 1.0.38 (09 June 09)
  - Units.sedona with defines for standard unit database
  - Rework `sys::File` API and natives
  - Sox enhancements for file get/put

### Build 1.0.37 (14 May 09)
  - Support define exprs as facet values
  - updated docs
  - Enum range facet

### Build 1.0.36 (22 Apr 09)
  - Add version information into `sedona.jar`
  - Support scientific notation
  - All actions are now virtual by definition. (Action overridding)
  - Roll scode version to 1.5 for virtual actions

### Build 1.0.35 (27 Mar 09)
  - Rename Platform to PlatformService
  - Move Sys.platform to App.platform
  - Require all sab files to declare PlatformService

### Build 1.0.34 (05 Mar 09)
  - Sox queryService message type

### Build 1.0.33 (24 Feb 09)
  - Updated documentation
  - Add exact version match to Depend grammar. (`"foo 1.0.10="`)
  - Check that continue/break are inside of loop

---
_Copyright &#169; 2007-13 Tridium, Inc._
