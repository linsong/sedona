<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Aug 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# New for v1.2

## Highlights

With the release of Sedona Framework 1.2 a number of features have been added.

- <b>Versioning</b>:
  You didn't miss anything, the Sedona open source version skipped from 1.0 straight to 1.2

- <b>Kit and Platform Manifest Server</b>:
  Kit and platform manifest files can now be stored directly on the Sedona device, if desired, and served automatically to Sox clients as needed.  See the section [Manifest Server](/deployment/schema#kit-manifest-server) to learn how to implement it on your platform

- <b>Device Discovery</b>:
  Sedona devices can now be discovered automatically using IP multicast.  Details can be found in the section on [Device Discovery](/networking/discover)

- <b>Multi-rate App Components</b>:
  Components now have some control over when the App cycle executes their child components. This can be used, for example, to have certain child components execute at a lower rate than the App cycle rate. See the [Apps](/apps/apps#execution) chapter for more details

- <b>Device Simulation</b>:
  New capability has been added to <code>sedonac</code> to make it easy to create a simulator version of any Sedona VM. Apps that depend on kits with platform-specific native methods can now be run on a different host platform using a simulator SVM. Detailed instructions on how to create a simulator SVM will be presented in the [Sedona Device Simulator](/platforms/deviceSim) chapter

- <b>Refactored Control Kit</b>:
  The <code>control</code> kit has been split into several smaller kits, organized roughly by functionality.  This should make it easier to use the control components on resource-limited devices

- <b>Prioritized Array Components</b>:
  A new kit <code>pricomp</code> offers components with prioritized inputs, including override capability:
  <br/> <br/>
  Each component in the <code>pricomp</code> kit uses a 16-level priority scheme, with inputs <code>in1</code> (highest) through <code>in16</code> (lowest) plus a <code>fallback</code> property, and a single output <code>out</code>. It also has override actions that explicitly set certain input slots.
  <br/> <br/>
  The value of <code>out</code> is determined by a priority scan of the inputs, looking for a valid value at <code>in1</code> first, then each of the other inputs in turn, all the way down to <code>in16</code> and then <code>fallback</code>. (A "valid" value is one that is not set to <code>null</code>, or its equivalent
  for that property's type.) The highest priority valid input propagates to the <code>out</code> slot.
  <br/> <br/>
  Most of the component inputs are linkable, with the few exceptions being inputs that can be set only via right-click override actions. Overridden inputs are evaluated along with the linkable ones, using the same priority scheme. <code>PrioritizedBoolean</code> components also offer built-in timers for minimum on and/or off times to protect sensitive equipment.
  <br/> <br/>
  See the [pricomp](/api/pricomp/index) kit docs for more about these components.
