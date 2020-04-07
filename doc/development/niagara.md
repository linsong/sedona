
<!--
[//]: # (Copyright &#169; 2009 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    23 Mar 09 Matthew Giannini  Creation
) -->
[![Sedona](../logo.png)](/)
# Niagara

## Overview

This section covers integration of the Sedona Framework with the Niagara
AX Framework.

-   [Sedona Framework components in Niagara](#sedona-in-niagara)
-   [Provisioning Sedona Framework-enabled devices from Niagara](#provisioning-from-niagara)

## Sedona in Niagara

This section details how to write Sedona Framework components in such a
way that they can be easily integrated with the Niagara AX component
architecture. This section will help developers familiar with the
Niagara AX Framework to understand how Sedona idioms translate to
Niagara idioms. In particular, this section will detail various aspects
of programming Sedona Framework components so that the user experience
inside Workbench is very similar to that of using standard BComponents
in Niagara.

-   [How Niagara uses sys::Facets when modeling Sedona Framework
    Components](#component-facets)
-   [Niagara Views on Sedona Framework Components](#niagara-views)

In Niagara, all Sedona Framework components are modeled as a special
kind of BComponent called BSedonaComponent. Regardless of your Sedona
Framework component type, it will be modeled as a BSedonaComponent,
which essentially wraps your `sys::Component` type. The primary means of
communicating information about a Sedona Framework component type and
its slots to Niagara is by using `sys::Facets`. The following tables
detail which facets are recognized by Niagara, and how they are used
when Niagara constructs a BSedonaComponent to model your
`sys::Component`.

### Component Facets

The following table lists the facets for a `sys::Component` type that
Niagara will recognize.

 | Facet       |  Type |   Description |
 |-------------|-------| ------------------------------------------------------|
 | niagaraIcon | Str   | The Ord to the location of the icon to use for this component |
 | palette     | bool  | If set to false, then the type will not be displayed in the Sedona Palette. Otherwise, it will be shown in the palette |

Examples
```java
    @niagaraIcon="module://icons/x16/control/math/add.png"
    public class Add2 extends Component { ... }

    @palette=false
    public class CommonBase extends Component { ... }
```

### Slot Facets

#### BComponent slot flags

If you define a facet on your Sedona component slot that has the same
name as a Niagara flag, *and* its value is `true` or `false`, then the
corresponding slot in the BSedonaComponent will be likewise set;
otherwise, the facet will simply be treated like all other facets (see
BComponent [facet list](#BComponent-Slot-Facets)). The most common slot flags to set are
listed in the table below.

 | Facet    | Description |
 |----------|-------------|
 |readonly  | The `readonly` flag is used to indicate slots that cannot be changed by the user. |
 | hidden   | Hidden slots are designed to be invisible to the user, and exist only for Java developers. User interfaces should rarely display hidden slots. |
 | summary  | Summary properties are the focal points of any given BComponent. This flag is used by user interface tools to indicate primary properties for display. This might be as a column in a table, or as a glyph in a graphical programming tool. |
 | confirmRequired | When the action is invoked by a user, a confirmation dialog must be acknowledged before proceeding. |
 | operator | This gives a slot an operator security level. By default when this flag is clear, the slot has an admin security level. |
 | noAudit | Setting this flag prevents property changes and action invocations from being audited. |

!!! Note
    All Sedona runtime slots are treated as summary unless the summary facet is explicitly set to false: `@summary=false`.

Examples:

```
    public class Foo extends Component
    {
      ...

      // Require the user to confirm that they actually
      // want to reboot the system before executing the action.
      @confirmRequired
      action void reboot() { ... }

      // Don't want UI to allow user to edit password property
      @config @readonly @asStr property Buf(17) password

      // This slot we don't want displayed to users, but we
      // want operators to be allowed to change it.
      @hidden @operator property int debugLevel = 0
    }
```

!!! Note
    See [Component Properties](/language/components#properties) for a discussion of `@config` and other Sedona-only facets.

#### BComponent slot facets

If a slot facet does not match a Niagara flag name, then it will stored
as a a BFacet for the slot. Some BFacets have a special meaning in
Niagara. This table lists some of the more common BFacets:

| Facet         | Type     | Description |
|----------------|----------|-------------|
| min            | number   | Used to specify the minimum value for a number, or the minimum number of characters in a Str. |
| max            | number   | Used to specify the maximum value for a number, or the maximum number of characters in a Str. |
| unit           | Str      | Display text that describes the property's units. |
| precision      | int      | Used with floating point numbers to define the number of digits after the decimal point. |
| radix          | int      | Used with integers to qualify base radix. |
| showSeparators | bool     | Used with numerics to enable/disable displaying of separators between every 3 digits (e.g. 10,000 vs. 10000). |
| multiLine      | bool     | Used with Str to support a multiline editor. |
| fieldWidth     | int      | Used with Str to specify the number of columns in a text field. |
| allowNull      | bool     | Tells the field editor allow the value to be set to null. |
| fieldEditor    | Str      | Indicates the BTypeSpec of the field editor to use for editing a property value. It overrides the default field editor registered for the property's value. |
| trueText       | Str      | Display text to be used for a boolean property when true. |
| falseText      | Str      | Display text to be used for a boolean property when false. |
| nullText       | Str      | Display text to be used for a boolean property when null. |

Examples:

```
    public class Foo extends Component
    {
      ...

      // Property for password - must be at least 8 chars long
      // and no more than 16.
      @min=8 @max=16
      @config @asStr property Buf(17) pwd

      // The debug level should be specified in base 16.
      @radix=16 @min=0 @config
      property int dbgLevel = 0x1234
    }
```

### Niagara Views

Normally in Niagara, views are registered on a BTypeSpec by using the
`<agent/>` specification in the `module-include.xml` file of your module.
However, since all Sedona Framework components are modeled as
BSedonaComponents in Niagara, a different mechanism is used to register
views on your Sedona Framework component. Namely, `<def/>` blocks are
used to define agents on specific Sedona Framework component types. An
example follows.

Suppose you have developed a Service called MyService in kit `foo`. The
qname for this type is `foo::MyService`. Suppose, further, that you have
written a Niagara view `BMyServiceView` to manage this service and that
it resides in module `bar`. The BTypeSpec for this service is
`bar:MyServiceView`. To register your view on the `MyService` service,
you would add the following lines to the `module-include.xml` for your
module:

```xml
    <defs>
      <!-- This declares the agent on the foo::MyService service -->
      <def name="sedona.foo::MyService#agent" value="bar:MyServiceView" />
    </def>

    <types>
      ...
      <!-- Declare the Niagara type as usual, but no agent block is required -->
      <type name="MyServiceView" class="com.bar.BMyServiceView" />
      ...
    </types>
```
When your view loads, a BISedonaComponent will be passed to the
`doLoadValue` method of your view.
```java
    protected void doLoadValue(BObject value, Context cx) throws Exception
    {
      BISedonaComponent myService = (BISedonaComponent)value;
      // See the javax.baja.sedona.sys.BISedonaComponent interface
      // for more details.
    }
```

!!! Note
    The concrete class BSedonaComponent is deprecated as of the TXS 1.1 release. You should migrate your views to use the new BISedonaComponent interface instead. If you have questions about writing views for Sedona Framework components, ask them on one of the Sedona Framework forums on [Niagara Central](http://niagara-central.com/ord?portal:/discussion).

## Provisioning from Niagara

When using Niagara tools to provision a Sedona Framework-enabled device,
there are a few things to be aware of:

**File names**
:   Niagara makes assumptions about the file names used to launch the
    remote platform. In particular, it assumes: the SVM is named
    "svm.bin" (or "svm.exe"), the kit bundle file is named
    "kits.scode", and the app binary is named "app.sab".

**File PUT**
:   To be Sox compliant, Sedona Framework platforms must use `app.sab`
    as the file name of the app to run, and `kits.scode` as the file
    name of the scode image to run. Further, to be Sox compliant, a
    platform must look for an `app.sab.stage` and a `kits.scode.stage`
    file when starting and rename them to `app.sab` and `kits.scode`
    respectively before the SVM attempts to load the app and scode.

    Hence, when Niagara writes new app and scode files to the device, it
    does so in three stages:

    1.  Writes the app as `app.sab.writing` and the scode as
        `kits.scode.writing`.
    2.  Renames those files to `app.sab.stage` and `kits.scode.stage`
        respectively.
    3.  Restarts the device.

**File GET**
:   A file GET operation from Sedona to Niagara should prompt the user
    for the path and filename to be used on the local host for the
    received file. The name of the file as it is being transferred is
    normally not visible (or relevant) to the user.
