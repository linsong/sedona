
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    24 Jul 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Sox

## Overview

*Sox* is the standard protocol used to communicate with Sedona
Framework-enabled devices. Sox provides the following services:

-   **Version Info**: ability to query current version of software.
-   **Polling**: ability to poll for current values.
-   **COV**: ability to subscribe to change of value events.
-   **RPC**: ability to invoke component actions remotely.
-   **Programming**: ability to add/remove/rename/link/unlink
    components.
-   **File Transfer**: ability to get or put arbitrary streams of bytes.
-   **Provisioning**: ability to backup/upgrade the SVM, kits, or app
    via file transfers.

## Dasp

Sox is designed to be run over UDP via the [Dasp](/networking/dasp) protocol.
Dasp handles a lot of complexity for networking:

-   Dasp manages authenticated sessions, so that we can associate
    incoming Sox messages against a user account.
-   Dasp manages reliability. When Sox sends a message, it doesn't have
    to worry about retries - Dasp ensures the message gets delivered.
-   Dasp manages flow control. Communication such as a file transfer
    that creates bursts of packets is automatically throttled to the
    network's capacity by Dasp.
-   Dasp is full duplex - once a session is established, either side may
    initiate a message send.

Dasp also defines some restrictions that Sox must take into
consideration:

-   Dasp is packet based, so Sox must design messages to fit within the
    maximum packet size (typically around 500 bytes).
-   Dasp is reliable, but unordered, so Sox cannot assume that messages
    are processed in order.

## Design

Sox has a very basic design. All sox messages are formatted with a
standard two byte header:

    u1    cmd
    u1    replyNum
    u1[]  payload

Every request is assigned a command identifier, which is a lower case
ASCII letter. For example `'r'` indicates a read property request.
Responses to a request use a capitalized ASCII letter, for example a
read property response has a cmd identifier of `'R'`.

Every request is tagged with a one byte replyNum. The replyNum is used
to associate requests with their responses. Because the replyNum is only
byte, there can only 255 outstanding requests (although it is quite
likely Dasp will restrict flow control to a much smaller number of
requests).

Each specific command has a predefined [payload structure](#messages).

## Sedona Framework APIs

There is a server side Sox implementation written in 100% Sedona code
and deployed in the `sox` kit. The `sox` kit includes both the Sox and
Dasp protocol stack and depends on `inet` kit for the IP stack. You must
have a port of the `inet` kit available for your platform in order to
run Sox.

Most often you will put an instance of the `SoxService` in your
application. The SoxService opens a configured UDP port (default is
1876). The SoxService manages authenticating incoming sessions, handling
all the sox commands, and ensuring that sessions are timed out and
cleaned up properly.

## Java APIs

The open source distribution also includes a full Java implementation of
the Dasp/Sox protocol stacks. These stacks include both a client and
service side implementation (although typically you will only use the
client side). The Java APIs are designed to "Sedona Framework enable"
your tools and supervisor applications.

The `sedona.dasp` package implements the lower level Dasp protocol
stack. This code is cleanly separated from Sox and may be used stand
alone if desired.

The `sedona.sox` package implements the Sox protocol stack and provides
a light weight Java model for Sedona Framework components. The
`SoxClient` class provides a Sox aware wrapper around `DaspSession` for
sending/receiving the set of sox commands. Many of the commands use
`SoxComponent` as the Java side representation of Sedona Framework
components.

The `SoxClient` class is also used to initiate get/put file transfers.
You will use these APIs if implementing your own provisioning strategy.

## Messages

The following Sox message commands are defined:

| Id | Command                     | Description |
|----|-----------------------------|-------------------------------------------|
| a  | [add](#add)                 | Add a new component to the application |
| b  | [fileRename](#filerename)   | Rename or move a file |
| c  | [readComp](#readcomp)       | Read a component in the application |
| d  | [delete](#delete)           | Delete an component and its children from the application |
| e  | [event](#event)             | COV event for a subscribed component |
| f  | [fileOpen](#fileopen)       | Begin a get or put file transfer operation |
| i  | [invoke](#invoke)           | Invoke a component action |
| k  | [fileChunk](#filechunk)     | Receive or send a chunk during a file transfer |
| l  | [link](#link)               | Add or delete a link |
| n  | [rename](#rename)           | Rename a component |
| o  | [reorder](#reorder)         | Reorder a component's children |
| q  | [query](#query)             | Query installed services |
| r  | [readProp](#readprop)       | Read a single property from a component |
| s  | [subscribe](#subscribe)     | Subscribe to a component for COV events |
| u  | [unsubscribe](#unsubscribe) | Unsubscribe from a component for COV events |
| v  | [version](#version)         | Query for the kits installed |
| w  | [write](#write)             | Write the value of a single component property |
| y  | [versionMore](#versionmore) | Query for additional version meta-data |
| z  | [fileClose](#fileclose)     | Close a file transfer operation |
| !  | [error](#error)             | Response id for a command that could not be processed |

### add

The add command is used to add a new component to the application. The
configProps must match the schema definition for the type.

    req
    {
      u1    'a'
      u1    replyNum
      u2    parentId
      u1    kitId
      u1    typeId
      str   name
      val[] configProps
    }

    res
    {
      u1    'A'
      u1    replyNum
      u2    compId
    }

### fileRename

Rename a file from one path to another path. Also see the `sys::File`
API.

    req
    {
      u1  'b'
      u1   replyNum
      str  from
      str  to
    }

    res
    {
      u1  'B'
      u1  replyNum
    }

### readComp

Read a given component's state. Component state is broken up into four
sections:

-   tree: the component's id, name, parent, and children
-   config: all the persistent properties (marked with `@config` facet)
-   runtime: all the transient properties (not marked with `@config`
    facet)
-   links: all the links to and from the component

    req
    {
      u1   'c'
      u1   replyNum
      u2   componentId
      u1   what: 't', 'c', 'r', 'l' (tree, config, runtime, links)
    }

    res
    {
      u1    'C'
      u1    replyNum
      u2    componentId
      compTree | compProps | compLinks
    }

    compTree
    {
      u1    't'
      u1    kitId
      u1    typeId
      str   name
      u2    parent
      u1    permissions
      u1    numKids
      u2[numKids] kidIds
    }

    compProps
    {
      u1     'c' or 'r' for config or runtime, 'C'/'R' if operator only
      val[]  propValues
    }

    compLinks
    {
      u1    'l'
      Link[] links
      u2     0xffff end marker
    }

### delete

Delete a component from the application. This command automatically
deletes any child components as well as any links to or from the deleted
components.

    req
    {
      u1    'd'
      u1    replyNum
      u2    compId
    }
    res
    {
      u1    'D'
      u1    replyNum
    }

### event

The event command packages up a COV event and is pushed asynchronously
from the server to client periodically for subscribed components. It
uses the same component state model as [readComp](#readcomp). Events
don't have responses, but rather rely on Dasp's acknowledgements to
guarantee delivery.

    req
    {
      u1   'e'
      u1   replyNum
      u2   componentId
      compTree | compProps | compLinks
     }

### fileOpen

The fileOpen command is used to initiate a get or put file transfer. The
chunk size is negotiated such that chunk messages fit within the
underlying transport's packet size (for example if running over 6LoWPAN
we want to fit each chunk into a single 802.15.4 packet).

On a put, the fileSize is the number of bytes that will be written. On a
get, the fileSize is the maximum number of bytes to read. The actual
number of bytes read might be less if there are not fileSize bytes
available (taking into account the size of the actual file, and the
potential offset).

    req
    {
      u1   'f'
      u1   replyNum
      str  method ("g" for get, "p" for put)
      str  uri
      u4   fileSize
      u2   suggestedChunkSize  (suggested by client)
      headers[]
      {
        str  name
        str  value
      }
      u1 end of headers '\0'
    }

    res
    {
      u1   'F'
      u1   replyNum
      u4   fileSize
      u2   actualChunkSize
      headers[]
      {
        str  name
        str  value
      }
      u1 end of headers '\0'
    }

#### Supported Headers

| Name   | Value | Method   | Default | Description |
|--------|-------|----------|---------|----------------------------------------|
| offset | `int` | get, put | 0       | Byte offset to use when reading/writing the uri |
| mode   | w, m  | put      | w       | File mode to use on a put. 'w' opens the file and truncates it if the file already exists. 'm' opens the file for random access |


### invoke

Invoke an action on a component. This command can be used as an
[RPC](http://en.wikipedia.org/wiki/Remote_procedure_call) mechanism.
Note that the action is executed synchronously; the Sox server blocks
until the action completes and returns.

    req
    {
      u1   'i'
      u1   replyNum
      u2   componentId
      u1   slotId
      val  argument
    }

    res
    {
      u1   'I'
      u1   replyNum
    }

### fileChunk

The fileChunk command transfers a single chunk within a file transfer
operation. The same message structure is used for both get and put
operations. Because Sox is run over Dasp, it is quite possible for
chunks to be received out of order.

    req
    {
      u1   'k'
      u1   replyNum (ignored)
      u2   chunkNum
      u2   chunkSize
      u1[chunkSize] chunk
    }

### link

The link command is used to add or delete a link between two Component
slots in the application.

    req
    {
      u1   'l'
      u1   replyNum
      u1   'a' | 'd' (add/delete)
      u2   fromCompId
      u1   fromSlotId
      u2   toCompId
      u1   toSlotId
    }

    res
    {
      u1    'L'
      u1    replyNum
    }

### rename

Rename is used to change a Component's name.

    req
    {
      u1    'n'
      u1    replyNum
      u2    compId
      str   newName
    }

    res
    {
      u1    'N'
      u1    replyNum
    }

### reorder

Reorder a component's child components.

    req
    {
      u1    'o'
      u1    replyNum
      u2    compId
      u1    numChildren
      u2[]  childrenIds
    }

    res
    {
      u1   'O'
      u1   replyNum
    }

### readProp

Read a single property value from a component.

    req
    {
      u1  'r'
      u1  replyNum
      u2  compId
      u1  propId
    }

    res
    {
      u1   'R'
      u1   replyNum
      u1   any code
      var  propValue
    }

### subscribe

The subscribe command allows you to subscribe to multiple components by
registering for a set of change [events](#event). The subscribe command
will return the number of components that were actually subscribed. This
number may be less than the number of components requested if the user
does not have permissions on a component, or if the component does not
exist anymore

You can also subscribe to all tree events (instead of subscribing to
individual component tree events). In this case, the response contains no
payload.

    req
    {
      u1   's'
      u1   replyNum
      u1   whatMask: tree=0x1 config=0x2 rt=0x4 links=0x8 0xff all tree
      u1   num:
      u2[] compIds: The ids of all the components to subscribe to
    }

    res
    {
      u1     'S'
      u1     replyNum
      u1     numSubscribed: The actual number of components that were subscribed
    }

### unsubscribe

Unregister a [subscription](#subscribe). The client will stop receiving
change [events](#event) for the set of components specified.

    req
    {
      u1   'u'
      u1   replyNum
      u1   whatMask: tree=0x1 config=0x2 rt=0x4 links=0x8 0xff all tree
      u1   num
      u2[] compIds
    }

    res
    {
      u1  'U'
      u1  replyNum
    }

### version

Get the list of installed kits and their checksums, which defines the
[schema](/deployment/schema).

    versionReq
    {
      u1  'v'
      u1  replyNum
    }

    versionRes
    {
      u1   'V'
      u1   replyNum
      u1   kitCount
      kits[kitCount]
      {
        str name
        i4  checksum
      }
    }

### write

Set a component property value to a specific value.

    req
    {
      u1   'w'
      u1   replyNum
      u2   componentId
      u1   slotId
      val  value
    }

    res
    {
      u1   'W'
      u1   replyNum
    }

### versionMore

Get extra version information, which includes the version of the
installed kits plus platform specific name/value pairs.

#### Name/Value pairs

| Name   | Value                | Notes |
|--------|----------------------|----------------------------------------------|
| soxVer | Sox protocol version | Introduced in build 1.0.45 (see discussion below) |

All builds of the Sedona Framework prior to 1.0.45 will return `null`
for the sox protocol version. Starting in build 1.0.45 the sox protocol
was changed to allow [batch subscription](#subscribe), to components. If
the sox protocol supports batch subscription, then it will return a
soxVer of `1.1` (or higher).

    req
    {
      u1  'y'
      u1  replyNum
    }

    res
    {
      u1   'Y'
      u1   replyNum
      str  platformId
      u1   scodeFlags
      kitVersions[kitCount]
      {
        str version
      }
      u1   pairs
      pairs
      {
        str  key
        str  val
      }
    }

### query

Perform a query for a set of components. Currently you can only query
for a service type using queryType of 's'. But the message is designed
to be enhanced thru additional queryTypes.

    req
    {
      u1   'q'
      u1   replyNum
      u1   queryType
      u1[] queryReq
    }

    queryType 's': query service
    req-service
    {
      u1  kitId
      u1  typeId
    }

    res
    {
      u1   'Q'
      u1   replyNum
      u1[] queryRes
    }

    queryType 's': query service
    res-service
    {
      u2[num]  compIds
      u2       0xffff end marker
    }

### fileClose

Terminates a file transfer operation.

    req
    {
      u1  'z'
      u1  replyNum
    }

    res
    {
      u1  'Z'
      u1  replyNum
    }

### error

The error response may sent back as a response to any request. It
indicates to the client that the request could not be processed. The
cause of failure is returned as a short string in the message.

    res
    {
      u1   '!'
      u1   replyNum
      str  cause
    }
