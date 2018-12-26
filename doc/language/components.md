
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    12 Sep 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Components

## Overview

Sedona is a *component oriented* language, which means that programs are
developed as reusable chunks of code designed to be snapped together
much like Lego building blocks. This chapter discusses components
themselves; the [Apps](/apps/apps) chapter discusses how components are
assembled into applications.

A component is any class that extends `sys::Component`. Components
include several features:

-   Can define [property](#properties) fields
-   Can define [action](#actions) methods
-   Can be organized into a tree structure
-   Can be given a human friendly name (up to 7 ASCII characters)
-   Have a two byte identifier within an application
-   Can be linked within an application
-   Provides [callback methods](#callbacks) that can be overridden
-   Component types have [reflection](/language/reflection) capability

## Properties

Properties are normal instance fields that are annotated with the
`property` keyword. Properties are how components expose configuration
and runtime state. Properties are restricted to the following types:

-   `bool`
-   `byte`
-   `short`
-   `int`
-   `long`
-   `float`
-   `double`
-   `sys::Buf`

Note that you must use the **:=** operator when assigning values to
properties. See [Assigning to Properties](/language/expr#assigning-to-properties) for details.

### Config vs Runtime

Properties are either *config* or *runtime*. Config properties are
persistent and typically are changed by the user. Runtime properties are
transient and are changed by the component itself during runtime.
Properties are runtime by default unless marked with the "config"
facet. A simple example:

    class FooServer extends Component
    {
      @config property bool enabled
      @config property int port = 80
      property int count  // runtime prop
    }

### Buf Properties

Properties typed as `Buf` are a bit special from the primitive property
types. The biggest difference is that primitives are by-value and can't
be changed without setting the field itself. However, buffer properties
are always inlined and accessed by reference. When declaring a buffer
property, the `inline` modifier is implied. The following example
illustrates a buffer property with a total capacity of 8 bytes:

    class Foo extends Component
    {
      property Buf(8) blob
    }

Because buffer properties are accessed by reference, the framework has
no knowledge when the buffer is modified. So it is the developer's
responsibility to notify the framework of a buffer change via the
`Component.changed` method (typically using a slot literal):

    void updateBlob()
    {
      blob.copyFromStr("wow!")
      changed(Foo.blob)
    }

Note that `changed` is called automatically when setting primitive
property types. You should only manually call `changed` for `Buf`
properties.

### AsStr Properties

`Buf` is the only non-primitive property type available. However, it is
a common case to store a `Str` value in a `Buf` property. If you know
that a buffer property will always store a null-terminated string, then
you should mark the property with the "asStr" facet:

    class Foo extends Component
    {
      @config @asStr property Buf(8) descr
    }

The `Buf` size is the total capacity including the null terminator - so
in the example above we can store a string with a max of seven
characters. The `Buf` class has convenience methods when storing a
string such as `toStr` and `copyFromStr`.

Marking a buffer property asStr has a couple of benefits:

-   Buffer overruns are handled to ensure that there is always a null
    terminator during deserialization.
-   It lets higher levels of the framework treat the buffer as more than
    raw binary data. For example, asStr properties are serialized as
    text rather than base64 in XML files.

## Actions

Actions are normal instance methods that are annotated with the `action`
keyword. Every action is implicitly defined to be `virtual`, so they can
be overridden. Actions are typically used as commands on a component. As
methods, actions "do something" rather than store or display a value.
Actions methods must be declared to return void and must have zero or
one parameter. If a parameter is specified then it must be one of the
following types:

-   `bool`
-   `int`
-   `long`
-   `float`
-   `double`
-   `sys::Buf`

Examples of "actions in action":

    class Foo extends Component
    {
      action void actionNoArg()
      {
        Sys.out.print("actionNoArg").nl()
      }

      @asStr action void actionStr(Buf x)
      {
        Sys.out.print(x.toStr()).nl()
      }
    }

    class FooOverride extends Foo
    {
      override action void actionNoArg()
      {
        Sys.out.print("Override actionNoArg").nl()
      }
    }

Note that you can use the "asStr" facet to annotate an action that
takes a Buf, if the argument should be a null-terminated string.

## Callbacks

Any class that extends `sys::Component` includes the following virtual
methods. Each is called automatically by the system at the appropriate
time. These callback methods may be overridden by Component subclasses
to change the behavior of the component.

      virtual void loaded()
      virtual void start()
      virtual void execute()

      virtual void changed(Slot slot)
      virtual void setToDefault(Slot slot)

      virtual int  childEvent(int eType, Component child)
      virtual int  parentEvent(int eType, Component parent)
      virtual int  linkEvent(int eType, Link link)

See the API documentation at `sys::Component` for details on how these
methods are used.
