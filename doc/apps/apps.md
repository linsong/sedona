<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    12 Sep 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Apps

## Overview

Applications for the Sedona Framework are designed and deployed
according to the component model, which separates the code from the
application. In this architecture you have a clean boundary between the
code packaged into kits and the application defined as a tree of
components defined in those kits. This model allows you to build
applications simply by assembling components, configuring their
properties, and linking slots together to define control flow. This
style of programming is especially amenable to graphical programming.

So when we refer to a Sedona Framework application, we are really
talking about a tree of components assembled together. An application is
purely declarative, all the code is encapsulated in the kits. The
application itself is stored as a file using one of two file formats:

-   **SAX**: a simple XML representation of the application that is
    easily generated and consumed by software tools
-   **SAB**: a compact binary representation of the application,
    suitable for storage and execution on a Sedona Framework-enabled
    device

You can convert between the two file formats using
[sedonac](/development/sedonac#compile-app).

## Boot Strap

Applications are boot-strapped in the following phases:

1.  **Loaded**: Each component is loaded from the SAB file into memory
    and its `Component.loaded()` callback is invoked.
2.  **Start**: Once all the components are loaded, each component has
    its `Component.start()` callback invoked.
3.  **Running**: The application enters the main execution loop
    (described next).

## Execution

The Sedona Framework's execution model is based a single-threaded main
loop with a fixed scan rate:

1. Recursively execute components. For each component:

  a. Step through the component's list of children and execute them first

  b.  Propagate incoming links to this component

  c.  Call this component's `execute()` virtual method
2.  Give any remaining time in the scan cycle to services via
    `Service.work()`
3.  If no services have remaining work, then relinquish CPU (via yield or sleep) until next cycle

### Controlling child execution rate

For Sedona 1.2 a new virtual method was added to the `Component` class.
If a component's `allowChildExecute()` returns `false`, then the app
will skip (1a), the recursive step through the component's child
components. It will simply call `execute()` on the current component and
then go on to the next one at the same hierarchy level. (The base class
version of the method always returns `true`, which preserves pre-1.2
behavior.) This method can be overridden to force child components to be
executed at a slower rate than the main loop. As an example, there is a
new component `sys::RateFolder` that uses this strategy to provide
multi-rate app functionality.

## Yield vs Sleep

Depending on the underlying execution environment, the App can either
return control to the OS (exit the SVM) or sleep until it's time to run
again.

### Preemptive multithreaded OS

These include Windows, Linux or QNX. When a thread calls the OS sleep
primitive, other threads are given a chance to run. The SVM thread may
be swapped in and out several times during an execute cycle.

### Main Loop

In this environment, the Sedona VM executes as the main loop and all
other work is done at ISR level. Once the App completes an execute
cycle, it can delay by entering a busy-wait loop.

### Cooperative tasking OS

In this environment, a task must return control to the scheduler to
allow other tasks a chance to run. The sleep and busy-wait approaches
won't work here since the SVM will never exit, so the SVM must support
a clean exit and re-entry. This is accomplished by the *yield* mechanism
described below.

## Yield

The Sedona Framework supports *yielding* to provide a graceful exit (and
subsequent reentry) of the SVM, allowing the platform CPU to perform
other operations.

Systems that require yield functionality must override the following
methods of `PlatformService`:

-   `yieldRequired()` - returns true if the SVM should exit after each
    App execute cycle
-   `yield(long yieldTime)` - indicates the SVM will be exiting and
    requests to be resumed in yieldTime nanoseconds

If a PlatformService subclass returns true to yieldRequired(), then the
App will exit with the error code `ERR_YIELD` after each execution loop.
On exiting, the App will call `yield(yieldTime)` to determine how soon
it needs to be resumed.

Native code should resume the SVM via `vmResume(SedonaVM*)` as soon as
possible, but before the requested time. If vmResume is not called
before yieldTime expires, App cycle overruns could occur.

## Hibernation

When entering hibernation, the App exits and returns control to the
bootstrap code. It is similar to yield but it is expected that the
hibernation time will be much longer than a typical yield time.
Hibernation is driven by application logic and most likely will not
occur each App execute cycle, whereas yield must occur each cycle.

To enter the hibernation state, an application calls `App.hibernate()`.
This will set a flag on App and when the current execution cycle is
complete, it will cause the App to exit with error code `ERR_HIBERNATE`.
This will gracefully unwind the call stack, returning control to the
boot code. The device's boot code should then put the device to sleep.
It is a device dependent issue to decide how it will wake up from
hibernation. When the device does wake, it should restart the SVM with a
call to `vmResume(SedonaVM*)`. If your device doesn't support
hibernation, then you will need to simulate it using code such as the
following:

```java
    result = vmRun(&vm);
    while (result == ERR_HIBERNATE)
    {
      //printf("-- Simulated hibernate --\n");
      result = vmResume(&vm);
    }
```

If you are developing Sedona Framework components, applications, or
drivers you should keep hibernation in mind. Any software that might run
on a battery powered device needs to support hibernation cleanly. This
means that function blocks should assume the scan rate might have
hibernation pauses. If you have services that need to do something
special when hibernating or waking, then you will need to override the
functions `Service.onHibernate()` and `Service.onUnhibernate()`.
`Service.onHibernate()` will be called prior to SVM exit.
`Service.onUnhibernate()` will be called after the SVM is resumed and
prior to the App execution loop starting back up.

## Steady State

Most apps will be fully operational by the end of the first cycle. When
hardware I/O is involved, however, an app may need to allow additional
time for the hardware to warm up, or for complex logic results to
propagate fully to all components. For this purpose, the Sedona
Framework provides a "steady state" timing feature that should be used
to protect the hardware from reading or writing transient values while
the app is starting up.

The steady state feature consists of two pieces:

-   **timeToSteadyState**: This integer specifies the time delay between
    the start of app execution and the time at which the app is assumed
    to be in "steady state". It is a config property of the App class,
    so it can be set in the app definition (.sax file) or at runtime
    from a remote access tool (e.g. the App property sheet in Sedona
    Framework Workbench).

    The default value is 0, meaning that the delay ends when the app
    enters the "Running" phase described above. The correct value for
    a given App will vary depending on the application logic and the
    specific hardware involved.
-   **isSteadyState()**: This is a method that returns `true` if the app
    has entered "steady state" mode, i.e. if the time delay defined by
    `timeToSteadyState` has elapsed, and `false` otherwise. Once steady
    state mode is reached, this method will continue to return `true`
    until the app is restarted. Code that affects hardware on the native
    level should use this method to avoid reading or writing hardware
    values until steady state is reached.

**Note**: This feature is not used internally within the App. It only
affects behavior of components that use the `isSteadyState()` method. It
is the responsibility of each kit developer to call this method as
needed to protect the hardware.

By default, `App.timeToSteadyState` applies only to the first time the
SVM is started. Once steady time has elapsed, hibernate/yield will not
affect it. If `App.hibernationResetsSteadyState` is set to true, then
the steady state flag will be cleared each time device exits
hibernation.

## Links

Links are the mechanism used to define control flow in an app. Links are
said to be *from* a given component's slot *to* another component's
slot.

Currently links are supported only between properties; you cannot link
to an action slot. During link propagation, the *from-property* value is
copied into the *to-property*. This mechanism could be used, for
example, to link sensor inputs through control logic to control actuator
outputs.

## Services

Services are special components that subclass from
[sys::Service](/api/sys/Service). Services have three primary
characteristics that set them apart from other types of components:

-   **Type Lookup**: Services are easy to lookup by type via the
    `App.lookupService` method. This allows other components in the App
    to find a given service at runtime simply by knowing its type.
-   **Background work**: Other components get a single callback,
    `execute()`, to do work during a scan cycle. Service components, on
    the other hand, have an additional callback, `work()` to handle
    background work during any time available at the end of a given scan
    cycle.
-   **Hibernation control**: Each time the App finishes an execute
    cycle, it calls `Service.canHibernate()` on all services. If a
    service is not in a state where it can hibernate, it should return
    false. (For example, if a service is waiting for a network reply, it
    would return false.) [Platform services](/platforms/platTutorial) for
    devices that never need to hibernate should always return false.

Services are often used to provide functionality to other components.
For example the `UserService` is used to lookup and authenticate user
accounts. Many services such as protocol drivers also perform background
work to service network messages.

## SAX File Format

A SAX file is structured as follows:

```xml
    <sedonaApp>
    <schema>
      <kit name='sys'/>
      ...
    </schema>
    <app>
      <comp name="play" id="1" type="sys::Folder">
        <comp name="rampA" id="7" type="control::Ramp">
          <prop name="min" val="20.00000"/>
          <prop name="max" val="80.00000"/>
        </comp>
        ...
      </comp>
      ...
    </app>
    <links>
      <link from="/play/rampA.out" to="/play/something.else"/>
      ...
    </links>
    </sedonaApp>
```

**<sedonaApp\>** root element that contains:

-   **<schema\>**: contains **<kit\>** elements
-   **<app\>**: contains **<comp\>** elements
-   **<links\>**: contains **<link\>** elements

**<kit\>** defines the kits used by the application:

-   **name**: required kit name
-   **checksum**: optional kit checksum; if omitted the latest version
    of the kit is assumed

**<comp\>** defines each component in the application:

-   **<comp\>**: nested elements map to nested components
-   **<prop\>**: property configuration for the component
-   **name**: required name of component (limited in length)
-   **type**: required qname of the component's type
-   **id**: optional two byte identifier; if omitted an id is
    auto-generated

**<prop\>** defines the property value of a component within a **<comp\>** element. Supported attributes:

-   **name**: required name of property
-   **val**: required value. Buf properties should be a base64 encoded
    value (unless asStr in which case just use string value).

**<link\>** element defines a link between two slots using the format
of `/path/comp.slot`:

-   **from**: the from component and slot name
-   **to**: the to component and slot name

## SAB Files

While XML is a nice representation for tools to work with app files, XML
is too big and difficult to work with in an embedded device. So we use
the SAB format when we need a compact binary representation. Sedona
Framework devices typically store their application as an SAB file
(although often it might just be a location in flash memory versus a
real file system).

## Schemas

Sedona Framework application files, whether stored as SAX or SAB format,
always contain a [schema](/deployment/schema). This is simply a list of the
kits (with matching kit checksums) that the app requires in order to
run. The Sedona VM cannot start the app if its current scode image does
not contain all the kits in the app's schema.

## APIs

If you want to develop tools for managing Sedona apps, check out the
`sedona.offline` Java APIs for working with both SAX and SAB files:

-   OfflineApp.encodeAppXml
-   OfflineApp.decodeAppXml
-   OfflineApp.encodeAppBinary
-   OfflineApp.decodeAppBinary
