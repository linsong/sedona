
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    29 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Arrays

## Overview

The Sedona Framework model for arrays is very similar to the C language.
An array is just a block of memory large enough hold a declared number
of items. Arrays do not store their own length, so bounds checking is up
to the developer.

The format of an array type declaration is:

      <Type>[<size>]  // size is optional
      byte[10] buf      // example of sized array type declaration
      Person[] people   // example of unsized array type declaration

If the size of the array is specified between the brackets we call it a
*sized array*, otherwise we call it a *unsized array*. The Sedona
Framework does not support multi-dimensional arrays.

## Memory Management

Arrays are always passed by reference - the reference is just a pointer
to the base address of the array. To actually allocate memory for an
array, it must be declared as an inline field. For example:

    class Foo
    {
      static int[5] x
      static inline int[5] y
    }

In the class above we declare two static fields. The field `x` is a
reference to an array of five ints - so it allocates enough memory to
store a pointer (typically 4 bytes on a 32-bit machine). The field `y`
on the other hand is *storage* for five ints, so it allocates 20 bytes.

In the example above `Foo.x` is just a pointer, so you can set `x` to
any value that points to 5 ints. For example the assignment `x=y`
updates `x` to point to the block of memory allocated by `y`.

On the other hand, `y` actually allocates a block of memory to store 5
ints. You can pass `y` around as a pointer and index its values, but you
cannot change what it points to. Like all inline fields, `y` can never
be used as the left hand side of an assignment statement.

## Arrays of References

An array of objects is stored as an array of references to those
objects. On a machine with 32-bit pointers, this means that an array of
3 objects will always be 12 bytes no matter how big the objects
themselves are. However, there is a special syntax for allocating enough
memory to hold the objects too:

```java
    class Point
    {
      int x
      int y
    }

    class Shape
    {
      static Point[] a
      static inline Point[3] b
      static inline Point[3] c = {...}
    }
```

The `Point` class stores two ints, which means a single instance of
Point is 8 bytes. Note the three different field declarations in
`Shape`:

-   **Shape.a**: The first field `a` declares a pointer to an array of
    `Point`s, so `a` is allocated enough memory to store a single
    pointer (most likely 4 bytes).
-   **Shape.b**: The field `Point.b` allocates enough memory to store *3
    references to Points*, but doesn't allocate any memory for actual
    `Point` instances - so `b` would allocate 12 bytes on a 32-bit
    machine. The references don't point to anything yet; they must be
    initialized elsewhere to point to actual objects.
-   **Shape.c**: The `Point.c` field uses the special syntax ` = {...}`
    to allocate enough memory for *an array of 3 references to Points*
    **and** *3 instances of Points*. So `c` will allocate 36 bytes of
    memory on a 32-bit machine: 12 for the array itself, plus 3\*8 for
    the instances. The compiler auto-generates initialization code to
    assign the references to the instances.

## Unsized Classes

Under most circumstances you must specify a size to declare an inline
array. However a class may declare one instance field as an unsized
inline array. We call these classes *unsized classes* because their size
isn't fixed. To make this work, unsized classes have a few special
rules:

1.  The class can have no more than one unsized inline array
2.  The unsized inline array must be an instance field (not static)
3.  The array length must be assigned in the constructor
4.  The length must be assigned directly from a constructor parameter
5.  The class must be declared `final` (true of all classes with
    constructor arguments)

The `sys::Buf` class is a good example of an unsized class:

```java
    final class Buf
    {
      Buf(int maxBufLen)
      {
        this.bytesLen = maxBufLen
        this.bytes.length = maxBufLen
      }

      inline byte[] bytes    // raw byte array
      short bytesLen         // length of bytes array
    }
```

The code snippet above illustrates how to declare an unsized inline
array. The constructor must have a statement that assigns the "length
field" of the array. The right hand side of the assignment must be a
parameter of the constructor (it can't be a calculated value).

To declare an instance of a `Buf` we pass the length to the inline
constructor:

      inline Buf(3) buf1st
      inline Buf(10) buf2nd

In the example above we declared two instances of `Buf`. The first
buffer allocates enough memory to store `Buf`'s fixed slots (such as
`bytesLen`) plus enough memory for 3 bytes. The second buffer allocates
enough memory for `Buf`'s fixed slots plus 10 bytes. This feature is
used extensively when working with the `sys::Str` class.

## Array Literals

Some array types may be declared as literals in code:

    define Str[] colors = {"red", "green", "blue"}

See [Array Literals](/language/fields#array-literals) for more details.
