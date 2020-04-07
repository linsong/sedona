
<!--
[//]: # (Copyright &#169; 2010 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    07 Jan 2010 - Matthew Giannini creation
) -->
[![Sedona](../logo.png)](/)
# Certified Platforms

!!! Info
    Given that Tridium no longer supports Sedona directly, the certification program may have been discontinued. This page is preserved for historical reasons only. Please check [Sedona Alliance](https://www.sedona-alliance.org/history.htm) for background.

## Overview
One of the steps involved in getting your platform [Sedona Framework
certified](http://sedonadev.org/certification.html) is that it must pass
all tests when run against the `sedonacert` test suite. The sedonacert
program verifies that 1) your platform is compliant with the Sox
protocol and 2) your platform can be provisioned in a standard way. In
order for your platform to pass the tests for provisioning, it must
implement the following requirements:

## Requirements

### App and Scode naming requirements

1.  a Sox get file request for a file with URI `app.sab` must return the
    currently running app.sab file.
2.  a Sox get file request for a file with URI `kits.scode` must return
    the currently running kits.scode file.

### Standard provisioning process requirements

A Sox client must be able to provision a new `app.sab` and `kits.scode`
on your device by executing these steps:

1.  Write the new app.sab with URI `app.sab.writing` using a Sox put
    file request.
2.  Write the new kits.scode with URI `kits.scode.writing` using a Sox
    put file request.
3.  Rename URI `app.sab.writing` to URI `app.sab.stage` using a Sox file
    rename request.
4.  Rename URI `kits.scode.writing` to URI `kits.scode.stage` using a
    Sox file rename request.
5.  Invoke the `restart()` action on the device's platform service to
    restart the SVM. When the SVM restarts, it must use the newly staged
    app and scode files to run the application. See the section on
    [bootstrapping](/development/porting#bootstrap) for more details.
