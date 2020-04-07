
<!--
[//]: # (Copyright &#169; 2009 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    10 Feb 09  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)

# Conventions

## Overview

This chapter documents the coding conventions we use for the core Sedona
Framework code, as well as the supporting Java and C code.

## Source Files

-   Use 7-bit safe ASCII (subset of UTF-8)
-   Prefer "\\n" unix line endings
-   Prefer to put class `Foo` in source file called "Foo.sedona"

## Indentation

-   Absolutely no tab characters, spaces only
-   Two space indentation
-   Allman styling curly braces:

        if (flag)
        {
          something()
        }
        else
        {
          somethingElse()
        }

-   Prefer single statement on each line with no semicolon
-   Leave one space between keyword and opening paren for statements
    such as `if` and `while`.

## Naming

-   Name classes with upper camel case such as "Foo" and "FooBar"
-   Name everything else with lower camel case such as "foo" and
    "fooBar" including kit names, field names, method names, local
    variables, and facets
-   Do not use screaming caps such as FOO_BAR (except for defined
    constants)

## Comments

-   Prefer `/* */` for commenting out sections of code
-   Prefer leading and trailing `**` in doc sections, especially for
    methods:

        **
        ** This is a method that does something.
        **
        Void something()

-   Break logical sections up using line of 65 / chars:

        ////////////////////////////////////////////////////////////////
        // Methods
        ////////////////////////////////////////////////////////////////

-   Use Javadoc-style `/** */` comments on classes, methods, and fields
    so that `sedonac` can auto-generate documentation for them.

## Facets

-   All facets should be on the line above the property definition.
    Exception: The `@config` facet should always be on the same line as
    the property definition.

```java
        @min=0 @max=minutesInADay @unit=Units.minute
        @config property short duration

        @min=0 @max=100
        property int range

        @asStr
        property Buf(32) filename = "foo.txt"
```
