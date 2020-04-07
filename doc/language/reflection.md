
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    12 Sep 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Reflection

## Overview

Despite its tiny runtime footprint, the Sedona Framework has powerful
reflection support for accessing kit, type, and slot meta-data at
runtime. However unlike the general purpose reflection found in Java,
Sedona reflection is only supported for *Component* types and
*Property/Action* slots.

## Kits

You can access the installed kits with the [sys::Kit](/api/sys/Kit)
class:

```
    // iterate over the installed kits
    foreach (Kit kit : Sys.kits, Sys.kitsLen)
      Sys.out.print("$kit.name $kit.version ${Sys.hexStr(kit.checksum)}").nl()

    // look up a kit by name
    Kit kit = Sys.findKit("sys")
```

## Types

The [sys::Type](api/sys/Type) API is used to reflectively work with
types at runtime. Only types that extend from `Component` are available
for reflection:

```
    // walk the types installed in a given kit
    Kit kit = Sys.findKit("control")
    foreach (Type t : kit.types, kit.typesLen)
      Sys.out.print("  ${kit.name}::${t.name}").nl()

    // lookup a type by qname
    Type t = Sys.findType("control::Ramp")

    // type literal for class User
    Type t = User.type
```

If you know the type you need at compile time, you should use type
literals since they are most efficient:

    UserService s = (UserService)Sys.app.lookupService(UserService.type)

## Slots

The [sys::Slot](api/sys/Slot) API is used to reflectively work with
slots at runtime. Only the property and action slots of component types
are available for reflection:

```
    // walk a type's slots
    Type t = App.type
    foreach (Slot slot : t.slots, t.slotsLen)
      Sys.out.print("  ${slot.name}: $slot.type.name").nl()

    // lookup a slot by name
    Slot restart = t.findSlot("restart")

    // slot literal for the App action "restart"
    Slot restart = App.restart
```

If you know the slot you need at compile time, you should use slot
literals since they are most efficient.

## Ids

Note that `Kit`, `Type`, and `Slot` each have an `id` field, which is a
direct index into the corresponding array:

| Id Field | Index Into |
|----------|------------|
| Kit.id   | Sys.kits   |
| Type.id  | Kit.types  |
| Slot.id  | Type.slots |

This is an extremely efficient way to lookup kits, types, and slots.
However, be aware that the id lookup only works within a specific
schema. As soon you change the installed kits or the version of those
kits, then all of the reflection ids will likely change. This trade-off
between the efficiency of id based lookup versus the ability to version
kits is the basis of [Schemas](/deployment/schema).
