
<!--
[//]: # (Copyright &#169; 2013 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    05 Mar 13  Elizabeth McKenney   Creation
) -->
[![Sedona](../logo.png)](/)
# Device Discovery

## Introduction

Sedona Framework (starting with version 1.2) provides a mechanism for
performing Sedona device discovery using a special DASP message directed
to a multicast address. Any Sedona device on the network that has joined
the multicast group then responds to the message with its platform ID
and IP address.

## Basic Operation

1.  **Join**: Each Sedona device that supports DASP device discovery
    joins a specific IP multicast group at startup.
2.  **Discover**: The Sox client kicks off a device discovery process by
    sending a DASP `DISCOVER` request to the multicast group address.
3.  **Respond**: When a device receives the `DISCOVER` message, it sends
    a response containing its address and platform ID.
4.  **Process**: The client processes any `DISCOVER` responses it
    receives, and maintains a cached list of devices that responded.

## Porting Tips

-   When porting Sedona to a new platform, the native method
    `inet::UdpSocket.join()` must be implemented with the target OS
    function calls to join the socket to a specified multicast group
    address. `UdpSocket.join()` will be called by the Sox service when
    it opens the socket. The multicast address to use is defined in the
    `inet::UdpSocket` native code.
-   Do not change the multicast group address definition in the `inet`
    natives unless you also change the corresponding definition in
    `DaspConst.java` Whatever address you select, it must be the same in
    both locations.
-   On the client side, the discover functionality is in
    `sedona.dasp.DaspSocket`. Invoking `DaspSocket.discover()` begins a
    DASP discovery operation. This clears the current cached list of
    discovered devices and sends the DASP `DISCOVER` message to the
    multicast group address.
-   Call `DaspSocket.getDiscovered()` to access the list of discovered
    devices; it returns an array of `DiscoveredNode` objects.

## Limitations

-   DASP device discovery cannot be implemented on platforms that do not
    support multicast addressing on either IPv4 or IPv6.
-   By default the open source code uses IPv4 multicast addressing.
    (IPv6 addressing is not officially supported for Sedona 1.2.)
-   The specific multicast group address used by this feature is
    currently not configurable at runtime. It is hardcoded for the
    Sedona VM within the `inet` natives, and for the Java side as a DASP
    constant. To use a different address, it must be changed in both
    source locations and both the SVM and the `sedona.jar` module must
    be rebuilt and deployed.
-   **Caution**: For DASP Discovery to work, the IP settings for the
    host's network interface(s) must be correct, including netmask and
    gateway address. Also, some networks may filter multicast messages,
    or prohibit them altogether.
