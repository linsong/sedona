
<!--
[//]: # (Copyright &#169; 2009 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    12 Jan 09  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Kits

## Overview
A *kit* is the basic unit of modularity in the Sedona Framework:

-   Kits define a global name for identifying modules
-   Kits define a global namespace for types and schemas
-   Kits define code versioning
-   Kits define the dependency graphs
-   Kits encapsulate code, types, and meta-data

Because kits define the top of the global namespace, each must have a
globally unique name. The Sedona Framework requires kits to be prefixed
with the vendor's name. See [Vendors](#vendors) for the rules regarding
vendor and kit naming.

## Versioning

Version numbers are specified as a sequence of decimal numbers separated
by the dot character. Convention is to use a four part version of
`major.minor.build[.patch]`.

The `sedona.util.Version` class provides a Java API for working with
versions and doing comparisons.

When a kit is compiled, `sedonac` uses the following rules to determine
the version of the kit (from highest priority to lowest):

1.  If the `-kitVersion` switch appears on the command line, then that
    version is used
2.  If the `version` attribute in "kit.xml" explicitly defines a
    version, then that version is used
3.  Fallback is to use the "buildVersion" definition in
    "lib/sedona.properties"

If the version is not specified in any of the above ways, a compile
error occurs.

## Dependencies

Dependencies identify a kit by name with a set of version constraints.
Dependencies are used whenever the Sedona Framework tools need to
resolve a specific kit version:

-   Kits declare dependencies to use another kit's APIs
    ([kits.xml](/development/sedonac#compile-kit)).
-   SCode images declare dependencies for the kits to link together
    ([scode](/development/sedonac#compile-code)).
-   Platform manifests declare dependencies on kits with native methods
    ([platform](/platforms/platDef#platform-manifest)).

The `sedona.Depend` class provides a Java API for parsing and comparing
dependencies:

     depend        := name space* constraints
     constraints   := constraint [space* "," space* constraint]*
     constraint    := versionSimple | versionPlus | versionExact | versionRange | checksum
     versionSimple := version
     versionPlus   := version space* "+"
     versionExact  := version space* "="
     versionRange  := version space* "-" space* version
     version       := digit ["." digit]*
     checksum      := "0x" 8 hex digits
     digit         := "0" - "9"


Note that a simple version constraint such as `foo 1.2` really means
`1.2.*` - it will match all build numbers and patch numbers within
`1.2`. Likewise `foo 1.2.64` will match all patch numbers within the
`1.2.64` build. The `+` plus sign is used to specify a given version and
anything greater. The `=` equals sign is used to specify an exact
version match. Hence, `foo 1.2.64=` would match `1.2.64` but not `1.2`,
or `1.2.64.1`. The `-` dash is used to specify an inclusive range. When
using a range, then end version is matched using the same rules as a
simple version. For example, not only `1.8`, `2.0.4`, and `3.1.1.1`, but
also `4`, `4.2`, and `4.0.99` are all matches for `foo 1.2-4`.

You may also specify a list of constraints separated by commas. Multiple
version dependencies are evaluated using a logical OR, i.e. any match is
considered an overall match. A version constraint and a checksum
constraint are evaluated using a logical AND, i.e. both must match.

Examples:

    "foo 1.2"                Any version of foo 1.2 with any build or patch number
    "foo 1.2.64"             Any version of foo 1.2.64 with any patch number
    "foo 0+"                 Any version of foo - version wildcard
    "foo 1.2+"               Any version of foo 1.2 or greater
    "foo 1.2.64="            Only foo version 1.2.64
    "foo 1.2.64=,0xaabbccdd" Only foo version 1.2.64 with checksum 0xaabbccdd
    "foo 1.2-1.4"            Any version between 1.2 and 1.4 inclusive
    "foo 1.2,1.4"            Any version of 1.2 or 1.4
    "foo 0x1b02d4fc"         Any version of foo with a checksum of 0x1b02d4fc
    "foo 1.0, 0x1b02d4fc"    Any version of foo 1.0 and a checksum of 0x1b02d4fc

## Vendors

To avoid naming collisions, kits must be prepended by a vendor name. The
vendor name is an alphanumeric text string and must be less than 32
characters. It is normally capitalized.

Examples of valid vendor names:

    Acme
    Acme12
    AcmeCompany

Your kit names must be prepended with your vendor name. Kit names are
alphanumeric and may contain the underscore ('\_') character after the
vendor prefix. The vendor name is treated as case-insensitive when it is
verified, so a kit prefix need not have the same capitalization as the
vendor name.

All these are valid kit names for a kit owned by the `Acme` vendor:

    acme_control
    acmeControl
    Acmecontrol

The prefix will also be checked against the vendor name specified in the
kit manifest (again ignoring case); this will be handled at compile time
by `sedonac` when it generates your kit manifest.

```xml
    <kitManifest
       name="acme_control"
       vendor="Acme"
       checksum="40464bf3"
       hasNatives="false"
       doc="false"
       version="1.0.5"
       description="Basic function block library"
    >
```
Visit <http://sedonadev.org> to register your vendor name.
