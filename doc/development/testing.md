
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    30 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Testing

## Overview

We all love test driven development! The Sedona Framework comes with
built-in support for writing and running unit tests. You won't have any
excuses to skip unit testing. The test framework is based upon the
following principles:

-   **Bundled with Kits**: the goal of the test framework is to enable
    you to bundle unit tests for each kit with the kit itself
-   **Test Only Code**: a set of classes within a kit is marked *test
    only* so that we can include or exclude test code easily
-   **Test Methods**: classes that extend `sys::Test` are automatically
    scanned for *test methods* and added to the test harness
-   **Test Harness**: the Sedona Framework runtime includes a harness to
    automatically run all the tests for a given scode image

## Test Only

Because the Sedona Framework targets very resource constrained devices,
we need to maximize every byte available. So the compiler lets you mark
which classes are test code versus normal runtime code. The standard
mechanism to do this is via the `test` attribute in your "kit.xml"
build file:

```xml
      <source dir="." />
      <source dir="test" testonly="true"/>
```
The XML above illustrates a kit build file that contains two source
directories. The "." directory contains the normal runtime classes. On
the other hand, all the classes defined in the "test" directory are
automatically marked *test only*. If you look at the IR for these
classes you will see each test class annotated with the `@testonly`
facet. Convention is to place all test code in a subdirectory called
"test" inside your source folder.

## Test Methods

Tests are organized into *test classes*, which are classes that extend
`sys::Test`. Every method inside a test class whose name starts with
"test" is a *test method*. Test methods are the primary unit of test
execution. Test methods must be public, static, non-native methods that
return void and declare no parameters.

Each test method should be designed to test a single feature or function
of your kit's code. The test is implemented using one or more `assert`
statements. This is an example test class for testing `sys::Str`:

    class StrTest extends Test
    {
      static void testEquals()
      {
        assert("a".equals("a"))
        assert(!"a".equals("x"))
      }
    }

## Test Harness

Follow these steps to run the test harness:

1.  Ensure that your scode image includes test code by setting the
    `sedonaCode.test` attribute to `true` in your scode XML build file.
2.  If you need to free up extra memory, or wish to narrow the tests to
    run, you can set the `depend.test` attribute on each `kit` element
    in your scode XML build file. This allows you to include or exclude
    test code on a kit by kit basis (if `sedonaCode.test` is `false`
    then these attributes are ignored).
3.  Ensure the `main` attribute is set to `sys::Sys.main` in your scode
    XML build file
4.  Build the test version of your scode
5.  Run the VM using the `-test` argument, which will execute all
    the test methods and report assert successes and failures:

```shell
        svm kits.scode -test
```
