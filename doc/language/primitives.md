
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    29 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Primitives

## Overview

The Sedona programming language contains the following built-in
primitive types:

-   `bool`: boolean value
-   `byte`: unsigned 8-bit integer (as field or array only)
-   `short`: unsigned 16-bit integer (as field or array only)
-   `int`: signed 32-bit integer
-   `long`: signed 64-bit integer
-   `float`: 32-bit floating point
-   `double`: 64-bit floating point

In addition to the primitive types, the following class types have
special language support for literal representation:

-   `Str`: string of ASCII characters terminated by 0 (like C string)
-   `Buf`: chunk of bytes in memory

These types are described in more detail below.

## Bool

The `bool` type stores a boolean variable. Boolean literals are
expressed using the `true`, `false`, and `null` keywords. Use `null` to
indicate an invalid boolean value. If used in a boolean expression,
`null` will evaluate to true (it is represented as 2 in memory).

A `bool` is stored in fields and arrays as an unsigned 8-bit integer.
During stack manipulation `bool`s are stored on the stack as signed
32-bit integers.

The representation for booleans:

| Value   | Binary | String    |
|---------|--------|-----------|
| `false` | 0      | "false"   |
| `true`  | 1      | "true"    |
| `null`  | 2      | "null"    |

## Integers

There are four integer types of varying widths:

-   `byte`: unsigned 8-bit integer (as field or array only)
-   `short`: unsigned 16-bit integer (as field or array only)
-   `int`: signed 32-bit integer
-   `long`: signed 64-bit integer

Both `byte` and `short` are special types that may only be used as
fields or in arrays. Attempting to use `byte` or `short` as a return
type, parameter type, or local variable type is a compiler error. Note
that unlike Java both `byte` and `short` are *unsigned*. Currently there
is no signed 8-bit or 16-bit integer type.

All integer operations on the SVM stack are performed using signed
32-bit integers. When a `byte` or `short` is loaded from a field or
array it is automatically expanded into a 32-bit signed value. Likewise
when it is stored back into a field or array it is narrowed from a
32-bit signed value.

Integer literals are decimal by default. If prefixed with "0x" they
are hexadecimal. You may use the underbar "\_" as separator in both
decimal and hexadecimal formats. To specify a 64-bit long value, you
must append an "L" to the number.

### Char escapes

You may also use single quotes to specify a character as an integer
literal. The following character escape sequences are supported:
```
      \0   zero/null terminator
      \n   newline
      \r   carriage return
      \t   horizontal tab
      \"   double quote
      \'   single quote
      \\   backslash
      \$   dollar sign ($ is used for str interpolation)
```
Examples of integer literals:

    8
    -78
    0xABCD
    10_000
    0xffff_ffff
    'x'
    '\n'
    0L
    0x1234_5678_aabb_ccddL

## Floating Point

The `float` type maps to a 32-bit floating point value and `double` to
64-bit floating point.

Floating point literals are expressed in decimal format using a '.'
dot as the decimal point. The 'F' or 'f' character may be used as a
suffix (required if not using a decimal point). The 'D' or 'd'
character is required as a suffix for a 64-bit double. You may use the
'\_' underbar as a separator.

You can also specify floating point literals in scientific notation. All
numbers given in scientific notation are of type `float` unless
explicitly marked as a `double` using 'D' or 'd'. A floating-point
literal has the following format:

The keyword `null` is used to represent not-a-number for situations
requiring indication of an invalid float or double. The string
representation for `null` floats and doubles is always 'null'. The
'==' operator will return true when comparing two `null` floating
point values (this is different from Java and IEEE). The evaluation of
arithmetic and comparison operations with `null` operands, however, is
unspecified for the Sedona VM.

    3f
    3.0
    40_000F
    -2.00D
    0d
    1e+5
    5.86e12d
    314159E-5
    null

## Time

The Sedona Framework represents time in nanosecond ticks, stored as a
64-bit long. When working with time, you can use a special literal
representation for `longs` using the following suffixes on a decimal
number:

| Suffix | Unit         | Nanosecond Multiplier |
|--------|--------------|-----------------------|
| ns     | nanoseconds  | 1                     |
| ms     | milliseconds | 1,000,000             |
| sec    | seconds      | 1,000,000,000         |
| min    | minutes      | 60,000,000,000        |
| hr     | hours        | 3,600,000,000,000     |
| days   | days         | 86,400,000,000,000    |

Examples of `long` time literals and what they represent:

    5ns          // 5L
    1ms          // 1_000_000L
    10sec        // 10_000_000_000L
    3min         // 180_000_000_000L
    12hr         // 43_200_000_000_000L
    0.5ms        // 500_000L
    0.001sec     // 1_000_000L
    0.25min      // 15_000_000_000L
    0.5days      // 43_200_000_000_000L
    1days        // 86_400_000_000_000L
    36500days    // 31_53_600_000_000_000_000L

## Str

The `sys::Str` class models a string of ASCII characters. Strings are
stored in memory like C strings using a null terminator (a byte with a
value of zero). You should use only 7-bit ASCII characters (clear high
bit) to allow future UTF-8 and Unicode support.

In the SVM, the `Str` class makes use of the [unsized
class](/language/arrays#unsized-classes) feature to create a `byte[]` of the
correct length when the `Str` object is instantiated. No other fields
are declared, so an instance of `Str` is stored in memory just like a
`byte[]`. You can also treat a `Str` reference as a normal C string
(`char*`) when writing [native methods](/language/nativeMethods).

Because `Str` is an unsized class, you must specify the length of the
string when declaring a `Str` field. For example to declare a `Str` that
can hold a max of 8 characters (including the null terminator):

```java
    Str someStr           // reference to Str stored elsewhere
    inline Str(8) myStr   // storage allocated here for 8 byte Str
```

Sedona also supports [string interpolation](/language/expr#string-interpolation)
when writing to an output stream.

`Str` literals are written using double quotes. You may use supported
[escape sequences](#char-escapes) for special characters inside the
quotes. All `Str` literals are *interned* when compiling a scode image -
this means that all `Str` literals with the same sequence of characters
will share the same reference. `Str` literals should be considered
read-only memory - never try to change the contents of a `Str` literal.

Examples of `Str` literals:

    "hello"
    "Hi there.\nHow are you?"

The compiler automatically adds the null terminator byte when interning
the literal. For example a pointer to the literal `"abc"` is really a
pointer to four bytes of memory containing `"abc\0"`.

## Buf

The `sys::Buf` class models a contiguous chunk of bytes in memory. Like
`sys::Str`, it is an unsized class containing a `byte[]` that is
allocated to the specified size when the `Buf` object is created. Unlike
`Str`, however, `Buf` does not treat its contents as a string, so no
null terminator is added. It also has fields that store the size of the
buffer and the number of bytes used.

The syntax for a Buf literal is `0x[hexDigits]`. You can use whitespace
(including newlines) between bytes. For example:

    static Buf literalA = 0x[cafe babe 03 dead beef]

Just like Str literals, Buf literals are interned and stored in scode
memory space. So you should never attempt to write into a Buf literal\'s
memory space - for example never try to set the `bytesLen` field or
change the contents of the `bytes` field.

## Array Literals

Although they are not free-form expressions, you can also declare array
literals in code:

    define Str[] colors = {"red", "green", "blue"}

See [Array Literals](/language/fields#array-literals) for more details.
