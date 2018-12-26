
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    24 Jul 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Networking

## Overview

The Sedona Framework is designed from the ground up to make building
smart, networked devices easy. The key networking concepts:

-   Networking APIs: the `inet` and `serial` APIs
-   Driver API: framework building your own drivers
-   Sox/Dasp Protocols: the standard protocol stack for provisioning
    Sedona Framework-enabled devices.

## APIs

The lowest levels of the networking stack are the `inet` and `serial`
kits.

The `inet` kit specifies the set of APIs used to work with TCP and UDP
sockets. These APIs are all designed to be non-blocking to work within
the Sedona Framework's single threaded main loop. In order to use the
`inet` API you will need to ensure that your target platform has
implemented all the `inet` native methods. The open source distribution
provides an implementation that uses the WinSock and Berkeley socket
APIs.

The `serial` kit specifies the set of APIs used to perform serial port
communications. The main goal for the `serial` APIs is to insulate
application code from the operating system or hardware's serial port
interface. Drivers should use the `serial` API whenever possible to
ensure portability across devices. In order to use the `serial` API you
will need to ensure a port of the native methods is available for your
platform. The open source distribution provides a Win32 implementation
of the serial natives.

## Driver Framework

You can build your own device/IO drivers for the Sedona Framework.
Typically you will use the `inet` or `serial` [APIs](#apis) to interface
with the network.

The `driver` kit provides a mini-framework used to standardize how
custom drivers are built for the Sedona Framework platform. Drivers are
structured using the component tree:

-   **DeviceNetwork**: models the driver's network connectivity and is
    the container for the devices.
-   **Device**: is the Sedona Framework representation for an external
    device.
-   **Point**: is the Sedona Framework representation for a data point
    in the external device.

## Sox Protocol

The *Sox* protocol is the standard protocol used to communicate with
Sedona Framework-enabled devices. Sox provides the following services:

-   **Version Info**: ability to query current version of software.
-   **Polling**: ability to poll for current values.
-   **COV**: ability to subscribe to change of value events.
-   **RPC**: ability to invoke component actions remotely.
-   **Programming**: ability to add/remove/rename/link/unlink
    components.
-   **File Transfer**: ability to get or put arbitrary streams of bytes.
-   **Provisioning**: ability to backup/upgrade the SVM, kits, or app
    thru file transfers.

The Sox protocol is designed to efficiently enable "live programming"
of the component model over the network. The Sox protocol itself is run
over the *Dasp* protocol, which provides session based security and
reliability over UDP.

See [Sox](/networking/sox) and [Dasp](/networking/dasp) to learn more about these
protocols.
