
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Jun 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Language

## Overview

Sedona is a programming language based upon the following principles:

-   **Familiar Syntax**: Reuses C, Java, and C\# syntax where possible.
-   **Object Oriented**: Includes the three pillars of OO: encapsulated
    classes, inheritance, and polymorphism.
-   **Component Oriented**: Based on a development paradigm of reusable
    software components. Applications are assembled by wiring together
    pre-built components.
-   **Static Typing**: Uses a static type system much like Java and C\#
    with explicit typing of fields, methods, and local variables. This
    allows the compiler to perform type checking.
-   **Static Memory**: Manages memory statically at compile time, making
    it ideal for embedded platforms with limited resources.

## Namespaces

Sedona Framework code is organized into a three unit hierarchy:

1.  **Kit**: A module of deployment and versioning. Kits form the top of
    the namespace and must be globally unique. Kit names are required to
    be prefixed with the vendor's name.
2.  **Class**: An object oriented unit of organization. Classes are
    always scoped within a kit.
3.  **Slot**: Special fields and methods defined within a class are
    called *slots*. Slots are the externally visible "wiring points"
    on a component that can be linked to other components in a Sedona
    application.

Kits are globally unique and used to scope classes which in turn scope
slots. We use this structure to define globally unique *qualified names*
or *qnames* for classes and slots. A Class qname consists of the kit
name and class name joined by the "::" separator. A slot qname is the
class's qname joined to the slot's simple name by the "." separator:

    control::Ramp      // class qname
    control::Ramp.out  // slot qname

## Structure

Sedona Framework software is packaged up into modules called *kits*. The
source code for a kit is managed as a set of text files with the
".sedona" extension. A "kit.xml" file instructs the compiler how to
compile the kit and what directories contain the source code. A typical
source directory is structured as follows:

      myCoolKit/                    // directory for myCoolKit
        +- kit.xml                  // kit build manifest
        +- CoolComp.sedona          // source file
        +- AnotherCoolComp.sedona   // source file
        +- test/
            +- CoolCompTest.sedona  // source file for tests

You can then compile the kit using either the directory or "kit.xml"
file. Assuming your current directory is the parent of "myCoolKit" you
could use either of the following commands to compile:

```shell
    sedonac myCoolKit
    sedonac myCoolKit/kit.xml
```

Each source file is called a *compilation unit*. Source text files
**must** use UTF-8 encoding (7-bit ASCII is a clean subset of UTF-8).
Each compilation unit can contain one or more *classes*. Sedona does not
require any correlation between your source file names and class names.
However, by convention each class is sourced in a file with the same
name. For example `CoolComp` is stored in a text file called
"CoolComp.sedona".

## Classes

A kit contains one or more *classes*. A class is very similar to a Java
or C\# class. However, Sedona doesn't have the concept of interfaces -
so everything is strictly based on single inheritance.

A class is composed of zero or more named *slots*. There are two kinds
of slots:

-   **Field**: a memory location for storing a variable to manage
    *state*;
-   **Method**: an executable function to manage *behavior*;

All slots must be uniquely named. Sedona doesn't support method
overloading by parameter types as you might find in Java or C\#.

## Protection Scope

The following keywords are used to annotate the protection scope of
classes and slots:

-   `public`: visible to everyone; this is the default scope if no
    keyword is specified [applies to classes and slots]
-   `internal`: visible to classes in the same kit only [applies to
    classes and slots]
-   `protected`: visible to subclasses only [applies to slots only]
-   `private`: visible to declaring class only [applies to slots only]
