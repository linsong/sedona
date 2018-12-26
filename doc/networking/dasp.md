
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    24 Jul 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# DASP

## Introduction

This document specifies the Datagram Authenticated Session Protocol or
DASP. This protocol is designed to provide an unordered, reliable,
secure session for full-duplex datagram exchange that can be implemented
for low power wireless networks and low cost devices. Specifically we
target networks that include 6LoWPAN. These networks are typically
comprised of devices without the resources to run a protocol such as TCP
and must implement their protocol stacks in under 100KB of memory.

## Requirements

The following requirements dictate the design of DASP:

1.  DASP runs over an unordered, unreliable packet based transport
    layer, which is assumed to be UDP or provide the same functionality.
2.  DASP will be a datagram oriented protocol, not a stream oriented
    protocol. Datagram boundaries will be maintained.
3.  DASP will deliver datagrams reliably with associated retries.
    Datagrams are guaranteed to be delivered exactly once.
4.  DASP will not guarantee delivery of datagrams in order. Because DASP
    is designed to run on devices with limited RAM resources, we must
    assume that buffer space is extremely scarce. Any ordering of
    datagrams is the responsibility of the application layer.
5.  DASP datagrams are delivered within the context of a session.
    Sessions allow us to efficiently associate datagrams with
    application context. Application context might include user account
    privileges, eventing state, or the state associated with a file
    transfer.
6.  DASP sessions are full-duplex - either endpoint may initiate a
    datagram.
7.  DASP sessions are established by authenticating a username and
    password. However the term password is loosely defined to include
    any type of secret key.
8.  DASP session setup will include an extensible mechanism for
    negotiating the cryptography capabilities of the two endpoints. The
    protocol will support plugging in new cryptography standards, but
    standardizes on SHA-1 for message digest. Version 1.0 does not
    specify encryption, but could be added via new handshaking headers.
9.  DASP will provide flow control in the delivery of datagrams between
    the endpoints using a sliding window. For example, a session might
    span both a high speed network such as Ethernet and a low speed
    network such as 6LoWPAN running over 802.15.4. Compounding the
    problem is that low speed networks may have few memory resources to
    allocate to buffering and can quickly be overwhelmed by bursts of
    packets.

## Design

A DASP session is established when a client initiates a connection to a
server. The two endpoints exchange a set of messages called the
*handshake* to establish or reject the session. Once a session is
established it is assigned an unsigned 16-bit identifier called the
*sessionId*. The sessionId is included in all subsequent messaging
within the context of that session. Messages within the session are
consecutively numbered with an unsigned 16-bit value, which we call the
*sequence number* or *seqNum*. Each endpoint of the session maintains
its own sequence and sliding window of outstanding datagrams. This
sliding window is used to implement reliability and flow control.
Sessions are terminated explicitly via a special control message, or may
be timed out after a period where no messages have been received from
the remote endpoint.

## Message Format

DASP messages are described as a data structure using the following
primitive types:

-   **u1**: an unsigned 8-bit integer
-   **u2**: an unsigned 16-bit integer in network byte order
-   **str**: UTF-8 encoded text, followed by a null terminate (zero)
    byte
-   **bytes**: an unsigned 8-bit length n, followed by n bytes
-   **x[ ]**: an array of type x that contains zero or more elements

All DASP messages are formatted as follows:

```java
    message
    {
      u2      sessionId
      u2      seqNum
      u1      high 4-bits msgType, low 4-bits numFields
      field[] headerFields
      u1[]    payload
    }
```

The data in the message is summarized:

-   **sessionId**: identifies the session associated with the message.
    During the handshake before a sessionId is allocated, the value
    0xffff is used.
-   **seqNum**: used to sequence each DATAGRAM message for management of
    the sliding window and matching up requests/responses. Each endpoint
    maintains its own sequence numbers.
-   **msgType**: 4-bits are used to identity how to handle the message
    from an DASP perspective. The list of message types are detailed in
    a separate section below.
-   **numFields**: 4-bits specify how many header fields follow in the
    message.
-   **headerFields**: a list of zero or more fields that carry
    additional data about the request much like HTTP header fields.
    Header fields are detailed in a separate section below.
-   **payload**: zero or more bytes of message payload. A DASP message
    does not specify the payload size, but assumes that the underlying
    transport such as UDP provides the total size of the entire message
    from which payload size can be computed.

## Message Types

The following table specifies the full list of message type identifiers:

| Id  | Name         | Description |
|-----|--------------|--------------------------------------------------------|
| 0x0 | discover     | Sent by client or server during device discovery process |
| 0x1 | hello        | Sent by client to initiate handshake |
| 0x2 | challenge    | Response to client's hello message |
| 0x3 | authenticate | Sent by client to provide authentication credentials |
| 0x4 | welcome      | Response to client's authenticate message if successful |
| 0x5 | keepAlive    | Heartbeat message and message acknowledgements |
| 0x6 | datagram     | Indicates an application datagram message |
| 0x7 | close        | Close the session |

## Header Fields

Header fields define a generic way to include an arbitrary set of
name/value pairs in an DASP header. They are used much like HTTP header
fields - each message type defines required and optional header fields
and how they are to be interpreted. Additional header fields can be
added to future versions of this specification without breaking the
generic processing of DASP messages.

Header fields are prefixed with a one byte headerId. The headerId
conveys the name of the header in the high 6 bits and the value type of
the header in the bottom 2 bits. Implementations can always look at the
least significant 2-bits of the headerIds to determine how to decode or
skip the header value. Implementations must ignore unknown headers. The
2-bit value type identifiers:

| Id  | Name  | Encoding |
|-----|-------|----------------------------------------------------------------|
| 0x0 | nil   | The header has no value - the header itself indicates a boolean |
| 0x1 | u2    | Unsigned 16-bit integer in network byte order |
| 0x2 | str   | UTF-8 encoded text string, followed by null terminator (zero) byte |
| 0x3 | bytes | 8-bit length, followed by string of raw bytes |

The list of headerIds defined by this specification:

| Id (6b, 2b)  | Name            | Type  | Description (default) |
|--------------|-----------------|-------|-------------------------------------|
| 0x05 (1,1)   | version         | u2    | Version of the protocol to use |
| 0x09 (2,1)   | remoteId        | u2    | Remote endpoint's session id |
| 0x0e (3,2)   | digestAlgorithm | str   | Name of digest to use ("SHA-1") |
| 0x13 (4,3)   | nonce           | bytes | Nonce to use for digest hash |
| 0x16 (5,2)   | username        | str   | Name of user to authenticate |
| 0x1b (6,3)   | digest          | bytes | Value of digest hash to authenticate |
| 0x1d (7,1)   | idealMax        | u2    | Ideal max size in bytes (512) |
| 0x21 (8,1)   | absMax          | u2    | Absolute max size in bytes (512) |
| 0x25 (9,1)   | ack             | u2    | Ack a single seqNum |
| 0x2b (a, 3)  | ackMore         | bytes | Ack a list of seqNums (0x01) |
| 0x2d (b,1)   | receiveMax      | u2    | Max size of receiving window in bytes (31) |
| 0x31 (c, 1)  | receiveTimeout  | u2    | Receive timeout in seconds (30sec) |
| 0x35 (d,1)   | errorCode       | u2    | One of the predefined error codes |
| 0x3a (e,2)   | platformId      | str   | Device's platform ID string |

Header fields may be specified in any order. Implementations must never
assume a specific order.

## Error Codes

The following types defines the error codes to be used with the
errorCode header field:

| Id   | Name                | Description
|------|---------------------|-------------------------------------------------|
| 0xe1 | incompatibleVersion | Server doesn't support version specified by hello |
| 0xe2 | busy                | Server is too busy to allocate new session |
| 0xe3 | digestNotSupported  | Client does not support digest algorithm in challenge |
| 0xe4 | notAuthenticated    | Client supplied invalid credentials |
| 0xe5 | timeout             | Remote endpoint is timing out the session |

## Handshake

A session is established by a series of messages called the handshake:

1.  The client sends the server a hello message
2.  The server responds with the challenge, welcome, or close message
3.  The client responds with the authenticate or close message
4.  The server responds with either a welcome or close message

Once the client receives the welcome message from either step 2 or step
4, then the session is established. An close message during any step
terminates the handshake.

### Hello

The hello message is sent by a client to initiate a session. The
following header fields apply:

-   **version** (required): specifies the protocol version to be used.
    The version of this specification is 1.0, which is represented as
    0x0100 in a 16-bit unsigned integer.
-   **remoteId** (required): the client side sessionId used for messages
    sent by the server to the client.
-   **idealMax** (optional): the ideal maximum of bytes per message from
    the client perspective - see Tuning. If not specified 512 is
    assumed.
-   **absMax** (optional): the absolute maximum of bytes per message
    from the client perspective - see Tuning. If not specified 512 is
    assumed.
-   **receiveMax** (optional): the absolute maximum size of the
    client's receiving windowing (number of messages). If not
    specified, then a window size of 31 messages is assumed.
-   **receiveTimeout** (optional): the number of seconds that may elapse
    without receiving a message before the session is timed out from
    client perspective; if not specified then a default of 30 seconds is
    assumed.

The sessionId of a hello message must be set to 0xffff to indicate a new
session from the server's perspective. The remoteId header specifies
the client's sessionId, all messages back to the client will use this
sessionId.

The seqNum should be a randomly chosen number between `0` and `0xffff`,
which becomes the start of the client sending window - this sequence
number must be the seqNum of the first datagram message sent by the
client to server.

### Challenge

Once a hello message has been received, the server assigns a randomly
chosen, unused sessionId and then returns a challenge message. The
remoteId header of the challenge contains the server side's newly
assigned session identifier. The sessionId of the challenge message is
the value of the hello's remoteId header.

The seqNum should be randomly chosen between `0` and `0xffff` - this becomes
the start of the server's sending window - the first datagram sent by
the server to the client must use this seqNum.

The following header fields apply:

-   **remoteId** (required): the server's sessionId to use for messages
    sent by the client to the server.
-   **digestAlgorithm** (optional): this specifies the digest algorithm
    to use for hashing the username, password, and nonce returned by the
    authenticate message. If this field is unspecified, then "SHA-1"
    is assumed. Other values that might be used are "MD5",
    "SHA-256", "SHA-384", and "SHA-512". Implementations are only
    required to implement "SHA-1", so this field should only be used
    when knowledge of client capability is known. Values of this field
    should always use upper case.
-   **nonce** (required): the challenge message is required to specify a
    nonce used to generate a secure hash of the username and password.
    This value should be randomly generated using a cryptographically
    strong algorithm. A nonce should only be used once to prevent replay
    attacks.

If the server doesn't support the version specified by the client in
the hello message, then it should send the close message with the
incompatibleVersion error code. The close message returned should
include the version header field, which tells the client which version
the server does support. The client may then choose to retry the
handshake if supports the server's version.

If the server is too busy to allocate a new session, then it should send
back a close with the busy error code.

A server can by-pass authentication completely by directly sending back
a welcome message.

### Authenticate

Once the client receives the challenge message, it has enough
information to generate the authenticate message. Clients never pass
user credentials directly to the server over the network. Instead the
client sends the server a digest of the username, password, and nonce.
The algorithm to compute the digest is defined by the digestAlgorithm
field defined in the challenge message (or if unspecified then SHA-1 is
assumed). The following function is used to compute the digest:

```
    credentials = digestAlgorithm(username + ":" + password)
    digest      = digestAlgorithm(credentials + nonce)
```

First we hash the UTF-8 encoded credentials string, which is the
username and password separated by a single ":" colon character. We
call this hash the credentials. We then run the hash function against
the credentials and the nonce, which produces a one-time use digest.
This mechanism permits DASP enabled devices to avoid storing a password
in plain text by storing the username and only the credentials hash.
However a device could store the username and password directly and
compute the credentials hash as needed.

Once the client computes the digest, it sends the server the
authenticate message. The authenticate message uses the sessionId
specified in the challenge's remoteId header. The seqNum should be the
same as that used by the hello message.

The following header fields apply:

-   **username** (required): the UTF-8 encoded string identifier of the
    user to authenticate.
-   **digest** (required): the message digest hash computed from the
    algorithm described above. If using SHA-1, this will be list of 20
    bytes (160-bit digest).

If the client does not support the digest algorithm specified by the
server's challenge message, then the client should immediately send the
server a close message with the digestNotSupported error code.

### Welcome

Once the server receives the authenticate message, it validates the
username and digest against its user database. If the authentication is
successful, then the server responds to the client with the welcome
message type. The sessionId of the welcome is the remoteId specified by
the client in its hello. The seqNum should be the same as that used by
the server's challenge message.

The following fields apply:

-   **idealMax** (optional): the ideal maximum of bytes per message from
    the server perspective - see Tuning. If not specified 512 is
    assumed.
-   **absMax** (optional): the absolute maximum of bytes per message
    from the server perspective - see Tuning. If not specified 512 is
    assumed.
-   **receiveMax** (optional): the absolute maximum size of the
    server's receiving windowing (number of messages). If not
    specified, then a window size of 31 messages is assumed.
-   **receiveTimeout** (optional): the number of seconds that may elapse
    without receiving a message before the session is timed out from
    server perspective; if not specified then a default of 30 seconds is
    assumed.
-   **remoteId**: if we are skipping the challenge, then the server's
    sessionId must be returned in the welcome, otherwise this header
    must be omitted.

If authentication fails, then the server sends the client back a close
message with the notAuthenticated error code.

### Tuning

The absMax and idealMax header fields are used to negotiate message
sizes between the client and server. The absMax is the absolute maximum
number of bytes that a message may contain including the DASP headers,
but not the transport headers (such as the UDP headers themselves).
Typically this value maps to the amount of buffer space a device can
dedicate to processing DASP messages. For example a device that can only
allocate 256 bytes to buffering an DASP message will not be able to
handle larger messages.

The idealMax header field specifies the ideal maximum number of bytes a
message should contain including the DASP headers (not including the
transport headers). Often DASP is running over a network like 6LoWPAN
that can support UDP packets larger than the MTU of a 802.15.4 frame.
However if implementing a protocol like file transfer, it is desirable
to chunk the stream such that messages fit within individual 802.15.4
frames without additional fragmentation overhead. The idealMax header
provides for this optimization.

Both absMax and idealMax negotiation works the same way. The client
specifies its absMax and idealMax fields in its hello message. If either
of the header fields are omitted, then they are implied to be 512 bytes.
The server has its own absMax and idealMax, which it returns in the
welcome message (or else they default to 512). The actual absMax and
idealMax used for the session is the minimum between the client and
server. For example:

```
    client: absMax=512, idealMax=256
    server: absMax=1024, idealMax=64
    session: absMax=512, idealMax=64
```

### Error Handling

Because we assume DASP runs over an unreliable transport,
implementations must be prepared to handle lost, delayed, or unordered
messages during the handshake process. The following are specific
conditions that may arise, and the recommended action for each:

**The client sends the hello, but does not receive the challenge.** In
this case, either the hello or the challenge may have been lost. In
either case, a new hello request should be sent. If after three attempts
fail, then the client should assume communication with the server is not
available

**The server sends the challenge, but doesn't receive an authenticate
message.** In this case, either the challenge or the authenticate
message might have been dropped. In either case the server should never
attempt resending the challenge, but rather should time out the session
if no authenticate is received. During the handshake process, the server
should use a shortened time out period (compared to normal session
communication).

**The client sends the authenticate, but does not receive the welcome.**
In this case, either the authenticate or the welcome message was lost.
The client should resend the authenticate message two more times, before
giving up. Servers should gracefully handle receiving multiple
authenticate messages for the same session.

## Messaging

Once the session has successfully been established, either end point may
initiate application level messaging via the datagram message type. When
an endpoint receives a datagram message it performs the following steps:

1.  Matches the sessionId to a valid session. If not a valid session,
    the request is ignored.
2.  Checks that the remote address matches the one used to setup the
    session. If not the request is ignored.
3.  Checks that the seqNum is within the valid receiving sliding window,
    otherwise it is ignored.
4.  Checks that the datagram hasn't already been processed, otherwise
    it is ignored. Each endpoint must keep track of messages it has
    already processed within its receiving window.
5.  Updates its receiving window
6.  Sends the datagram to the application layer for processing.
7.  Receiver eventually piggybacks an ack on an outgoing message

### Acks

In order to provide reliability, datagrams have to be acknowledged. If
there are outgoing messages, then the ack header should be piggybacked
to avoid extra messaging. Otherwise a keepAlive message with an ack
header is sent back to the remote endpoint to acknowledge the datagrams
received so far. The ack header specifies the most recent seqNum
successfully received. When an ack header is received, an endpoint can
assume that all `seqNums <= ackNum` have been successfully received and
that `ackNum + 1` has not been received yet by the remote endpoint.

If an endpoint has received messages with gaps in the sequence
numbering, it can use the ackMore header to enumerate the seqNums it has
received. This allows us to avoid resending those messages just because
we've had partial failures. The ackMore is encoded as a list of bits
that indicate messages received (bit is set), and messages not received
(bit is zero). The least significant bit corresponds to the seqNum in
the ack header, and the most significant bit n corresponds to `ackNum + n`.
The ackMore header must always be accompanied by the ack header. By
definition the least significant bit of the bitmask must always be one
since it corresponds to ackNum itself. Some examples:

```
    ack=10 ackMore=15          -> 0x21
    ack=10 ackMore=12, 13      -> 0x0d
    ack=10 ackMore=15, 18, 19  -> 0x03 0x21
```

### Retries

!!! Note
    The following behavior is not currently implemented in the opensource Sedona Framework implementations of DASP.

Once an endpoint has sent a datagram message, it should wait for a
period of time called the *sendRetry* (default = 1 sec) (see [Flow Control](#flow-control) for details). If no acknowledgement has been
received for the datagram's seqNum, then one of two things has
happened: the original datagram was lost or the acknowledgement was
lost. The endpoint should resend the datagram message with the original
sequence number, then wait again for sendRetry period. This process
should be repeated until the datagram has been sent the number of times
specified by the *maxSend* (default = 3) parameter. If the maxSend
attempt fails, then the session should be timed out and the close
message sent to the remote endpoint with the timeout error code.

### Sliding Window

DASP supports full duplex messaging - either side may initiate
application messages, which are identified with a 16-bit sequence
number. An endpoint can have multiple outstanding messages that have not
been acknowledged. The sequence of unacknowledged messages sent is
called the *sending window*.

Each endpoint also maintains a *receiving window*, which is the sequence
of messages it is prepared to receive. Any messages outside of the
receiving window are ignored. The receiveMax header is used to
communicate the maximum size of receiving window so that the remote
endpoint can tune its sending window. The receiveMax header of the
client is specified in its hello message, and the receiveMax of the
server is specified in the welcome message. Once receiveMax is
established during the handshake, it cannot be changed.

The receiveMax header specifies the maximum size of the sliding window,
since any messages outside of that window will be ignored. An endpoint
should never use a sending window size greater than the remote
endpoint's receiving window. However, often the sending window is
smaller than the remote's receive window. The sending window is grown
and shrunk during the lifetime of the session to allow dynamic
optimization of the session's throughput. Tuning the size of the
sending window is the basis for flow control and congestion control.

Since seqNum is stored in an unsigned 16-bit integer, implementations
must handle rollover. The seqNum `65535` is followed by the sequence
number 0. For example given lower bound of 65530 and a window size of
10, then the window wraps from 65530 to 3 inclusively.

Sequence numbers and the sliding window are only used for datagram
messages. The client's handshake hello and authenticate messages define
the client's starting sequence number, which is the seqNum of the
client's first datagram. The server's handshake challenge and welcome
define the sequence number of the server's first datagram. The close
and keepAlive messages should always use a sequence number of 0xffff.

## Flow Control

A primary goal for DASP is to provide communication for traffic spanning
networks and devices of varying capabilities. For example a common use
case is a PC on an Ethernet network communicating to a low cost device
on a 6LoWPAN network. The ability to tune a session's communication
rates based on the capability of the endpoints is called *flow control*.

It is also common for traffic on networks to vary over time during the
course of a session. Tuning the session to handle varying network loads
is called *congestion control*. For practical purposes, flow control and
congestion control are handled using the same mechanisms - so we will
use the term flow control generically.

Flow control in DASP is always managed on the sending side by
dynamically tuning the size of the sending window and the sendRetry
time. These values are tuned based on analysis of the session's recent
past performance.

## Keep Alive and Termination

A session is terminated via one of the following conditions:

-   One endpoint sends the close message
-   No packets are received after a timeout period

### Close

If possible, sessions should be gracefully shutdown using the close
message type. If the failure is in the DASP layer (as opposed to the
application layer), then the errorCode header should be specified.

If an endpoint receives either the close or error message types, then
the session is immediately terminated and the sessionId invalidated. No
acknowledgement is sent on close messages.

It is recommended that close messages be sent twice - endpoints should
automatically ignore duplicate close/errors because the sessionId will
be invalid. Errors during the handshake should only be sent once.
However, since there is no acknowledgment, we can never guarantee that
both endpoints are aware of the session termination (in which case we
must rely on timeouts).

### Timeout

We can never assume that sessions are closed gracefully, because
real-world applications and networks can't be trusted. If an endpoint
has not received any messages after a period of time, then the session
is timed out. A timed out session is terminated and the sessionId is
invalidated. The endpoint should send the remote endpoint an close
message with the timeout error code - this error should only be sent
once since there is a good chance the remote endpoint is no longer
available.

Each endpoint specifies its configured timeout via the receiveTimeout
header. The client specifies receiveTimeout in its hello message, and
the server in the welcome message. If the receiveTimeout is omitted
during the handshake, then 30 sec should be assumed. Once the session is
established the overall timeout of the session is the maximum timeout
between the client and the server. Both endpoints must then use the
longer timeout.

Because the timeout must be negotiated between the endpoints, care
should be taken with using very long time outs. The longer timeout is
used because we assume the less capable endpoint's network or device
drives overall reliability and speed. However longer timeouts also mean
longer periods where the critical memory resources of session state are
tied up waiting for a session timeout.

### keepAlives

The keepAlive message type is used to prevent session timeouts and carry
acknowledgments when there are no outgoing application messages.

If there are no outgoing datagram messages to piggyback an ack header,
then an endpoint should sent a keepAlive to acknowledge received
messages. Implementations can send these acknowledgements immediately or
may use a small delay called the *ackDelay*. The ackDelay provides a
period of time to coalesce multiple acknowledgements and wait for an
outgoing datagram message to be generated.

Even if all received messages have been acknowledged, then an endpoint
still needs to send keepAlive messages to ensure that the remote
endpoint doesn't timeout the session. In the absence of other
messaging, an endpoint should send keepAlives three times as fast as the
timeout period. For example if the negotiated timeout is 30 sec, then
keepAlives should be sent every 10 sec. Keep alives should only be used
when no application datagrams are being transmitted.

KeepAlives are themselves never part of the sliding window. The seqNum
of a keepAlives should be 0xffff. When a keep alive is received, it is
never checked against the receiving window and is never acknowledged. An
endpoint that sends a keepAlive should never attempt to retry the
keepAlive, since no acknowledgement is expected.

## Device Discovery

DASP supports device discovery with the following sequence:

1.  The client sends a discover request to all listening Sedona servers
2.  Any server that receives the discover request opens a Sox session
3.  The server then sends a discover response containing its platform ID
4.  The server closes the Sox session immediately

Each discover response the client receives is added to a list of
discovered nodes.

### Discover

A discover message may be sent by a client or a server. The client sends
a discover message with no header fields. The server's response message
is identical except that it adds a single header field containing its
platform ID.

-   **platformId** (response only): a string containing the platform ID
    of the responding device.

How the discovered devices are collected and processed is not specified.
