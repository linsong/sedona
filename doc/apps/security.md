
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    17 Jun 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Security

## Overview

The Sedona Framework's security model is based upon the following
principles:

-   Users represent an authenticated user account (human or machine)
-   Components are assigned to one or more of four security groups
-   Slots are designated as operator level or admin level
-   Users are assigned permission levels for the four groups
-   Any network access such as Sox checks permissions before any
    operation

## Users

Any Sedona Framework application that wishes to utilize security must
include an instance of the `sys::UserService` containing at least one
`sys::User` component. Each `sys::User` component represents a user
account. User components must be direct children of the UserService. The
`Component.name` of the User component is the username.

## Authentication

Each User component stores a 20 byte `cred` property, which is the
credentials hash of the user's password. Because credentials are a
one-way hash, the password is never readable. Credentials are computed
as follows:

1.  `text`  = username + ":" + password
2.  `bytes` = `text` from step 1 encoded as UTF-8
3.  `cred`  = SHA-1 hash of `bytes` in step 2

You can use the `sedona.util.UserUtil` to programmatically compute
credentials. Or you can use the command line:
```shell
    D:\sedona>sedonac sedona.util.UserUtil brian secret
    User:   brian:secret
    Digest: 0x[74091bc2a1f43108df56281b6a74975bab86236f]
    Base64: dAkbwqH0MQjfVigbanSXW6uGI28=
```
Network protocols that support authentication should work as follows:

1.  Generate a random *nonce* to be used only one time
2.  Send the nonce to the client
3.  Client should generate its credentials from username and password
4.  Client creates digest by hashing the credentials from step 3 with
    the nonce
5.  Client sends digest from step 4 to server
6.  Server computes expected digest from nonce and known credentials
7.  If client's digest from step 5 matches server's digest from step 6
    then client is authenticated

If a protocol doesn't support authentication, then it should define a
configuration property that references a user account to use for
security checking.

## Groups

Components are assigned into one or more security groups. There are four
security groups indicated by the least significant nibble in the
`Component.meta` property:

  | Group   | Value |
  |---------|-------|
  | Group 1 | 0x01 |
  | Group 2 | 0x02 |
  | Group 3 | 0x04 |
  | Group 4 | 0x08 |

Components can be in multiple groups. For example: to assign a component
to groups 2 and 3, set the bottom nibble of `Component.meta` to `0x06`. If
a component is not included in any groups, then that component is not
network accessible.

## Slots

Every slot in a component is defined as *operator* level or *admin*
level for security purposes. By default a slot is admin level. You can
mark a slot operator level with the `@operator` facet. Use
`Slot.isOperator()` to check a slot's level.

Permissions
===========

There are seven security permissions defined as a bitmask:

 | User    | Permission      | Bitmask |
 |---------|-----------------|---------|
 | User.or | operator read   | 0x01    |
 | User.ow | operator write  | 0x02    |
 | User.oi | operator invoke | 0x04    |
 | User.ar | admin read      | 0x08    |
 | User.aw | admin write     | 0x10    |
 | User.ai | admin invoke    | 0x20    |
 | User.ua | user admin      | 0x40    |

Users are granted zero or more of each of these seven permissions for
each user group via the `User.perm` property. Each group's permissions
is stored in a byte:

| Group    | Byte   |
|----------|--------|
| Group 1  | byte 0 |
| Group 2  | byte 1 |
| Group 3  | byte 2 |
| Group 4  | byte 3 |

For example to grant a user operator read and write in group 1 and full
permissions in group 3:

```java
    perm = ((or|ow|oi|ar|aw|ai|ua) << 16) | ((or | ow) << 0)
```
## Permission Grants

To compute the permissions a user has on a given component, we combine
the user's configured permissions for each group the given component is
a member of. Consider this example of a User's configured permissions:

| Groups   | or | ow | oi | ar | aw | ai | ua |
|----------|----|----|----|----|----|----|----|
| Group 1  | x  | x  | -  | x  | -  | -  | -  |
| Group 2  | x  | -  | x  | -  | -  | -  | -  |
| Group 3  | x  | x  | x  | x  | x  | x  | x  |
| Group 4  | -  | -  | -  | -  | -  | -  | -  |

Some examples of what permissions this user would be granted:

-   Permissions for component in group 1: or, ow, ar
-   Permissions for component in group 2: or, oi
-   Permissions for component in group 3: all
-   Permissions for component in group 4: none
-   Permissions for component in group 1 and 2: or, ow, oi, ar (union of
    1 and 2)

## Access Control

The previous sections explain how we compute the permissions granted to
a user for a specific component. The following table defines what
permissions are required for operations:

  | Perm  | Operation |
  |-------|--------------------------------------------------------------------|
  | or    | to read or subscribe to a component (1) |
  | or    | to read the current value of an operator property |
  | ow    | to modify the current value of an operator property |
  | oi    | to invoke an operator action |
  | ar    | to read the current value of an admin property (2) |
  | aw    | to modify the current value of an admin property |
  | ai    | to invoke an admin action |
  | aw    | on parent to add a child component |
  | aw    | on parent to reorder children components |
  | aw    | on component to rename |
  | aw    | on component to delete |
  | ar    | to read or subscribe to a components links |
  | ar/aw | **ar** on "from" component and **aw** on "to" component to create link |
  | aw    | on "to" component to delete a link |
  | ua    | on User component to read, write, modify, delete |

Sox Implementation Notes:

1.  When reading a component's children via a tree update, unreadable
    children components are automatically omitted in the result
2.  When performing a property update or subscribe, admin properties are
    not serialized when not available

Provisioning
============

Each user is explicitly granted permissions to read and write the files
associated with provisioning:

 | Method        | Permission for file     | Byte |
 |---------------|-------------------------|------|
 | User.provApp  | read/write "app.sab"    | 0x01 |
 | User.provKits | read/write "kits.scode" | 0x02 |
 | User.provSvm  | read/write "svm.\*"     | 0x04 |
