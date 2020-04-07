
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    29 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Native Methods

## Overview

Native methods are used to create Sedona APIs that bind to native code
written in the C programming language. The following steps are used to
create a native method:

1.  **Native Id**: Every native method is assigned a unique two byte
    identifier in the kit's `kit.xml` file
2.  **Stub**: Every native method is declared in the Sedona code using
    the `native` modifier
3.  **Native Implementation**: Native methods are implemented as
    functions in the C programming language
4.  **Native Tables**: At staging, function pointers to the native
    implementations are mapped into tables for dispatch by the SVM at
    runtime

## Native Id

Every native method is assigned a two byte identifier used to dispatch a
call to the proper C function. The first byte is the *kitId* and the
second byte is the *methodId*. Native ids are expressed as
"kitId::methodId".

Each kit that contains native methods should be assigned a unique kitId.
Technically kitIds don't need to be globally unique, but they must be
unique across all the kits that might be used together for a given
platform. The range of kitIds from 0 to 99 is reserved for core Sedona
Framework kits. Third parties should use kitIds from 100 to 255 (or
contact the Sedona Framework development team). The `sys` kit itself is
assigned the kitId of zero. Any [platform service](/platforms/platTutorial)
kit (i.e. a platform-specific kit containing a PlatformService subclass)
can be given a kitId of 1, since there will never be more than one such
kit loaded on a Sedona device at any given time.

Within a kit, every native method is assigned a unique methodId. Because
the methodId is only a byte, there can be at most 255 native methods in
a single kit.

The list of native ids for a kit is defined in the `kit.xml` file using
the following XML format:

```xml
    <natives>
      <native qname="foo::Type1.method1" id="6::0" />
      <native qname="foo::Type1.method2" id="6::1" />
      <native qname="foo::Type2.method1" id="6::2" />
    </natives>
```

The `natives` element contains one or more `native` elements for each
native method in the kit. The `native` element contains two required
attributes: `qname` specifies the qualified name of the native method
and `id` specifies the native id formatted as "kitId::methodId". In
this example, the kitId for kit `foo` is 6, and the native method
methodIds are 0, 1, and 2.

## Stubs

Native methods are declared like normal methods but without a method
body (just like abstract methods). Native methods must be flagged with
the `native` keyword. Native methods cannot be `abstract` or `virtual`.
For example:

    class Type2
    {
      static native int add(int a, int b)
      static native void test(bool z, int i, float f)
      native float  testf(int i, float f)
    }

The compiler will perform a series of checks upon the native ids and
native stubs when compiling source code into a kit file (in the
`ResolveNatives` step). Unless errors are detected, the native ids are
written into the appropriate IR files of the kit. If any native ids are
modified, you must recompile from source.

## Native Implementation

The SVM is stack based. Each item on the stack is called a *Cell*, which
is a union of `int32_t`, `float`, and `void*`. Unless you are running on
a 64-bit processor, a Cell is 32-bits wide. The definition of Cell in
`sedona.h` is:

    typedef union
    {
      int32_t ival;    // 32-bit signed int
      float   fval;    // 32-bit float
      void*   aval;    // address pointer
    }
    Cell;

Every native method must be implemented in C as a function that takes
two arguments: a `SedonaVM_s` pointer and a `Cell` pointer into the
stack, and returns a `Cell`. (Native methods that return a `long` or a
`double` require special handling, described in more detail below). The
definition for SedonaVM\_s is in `sedona.h`. The typedef for a native
method pointer is:

    typedef Cell (*NativeMethod)(struct SedonaVM_s* vm, Cell* params);

The method parameters are accessed from the stack via the Cell pointer
`params`. You can manually extract the individual parameters using array
indexing. If the native method is not static, then parameter 0 is always
the implicit `this` pointer.

It is important to note that all native method implementations return a
Cell value even when the Sedona signature for the method returns `void`.
You can use the constant `nullCell` to return from a method that returns
`void`. Other predefined Cell constants are `trueCell`, `falseCell`,
`zeroCell`, `oneCell`, and `negOneCell`.

An example implementation of the `foo::Type2.add` method:
```c
    Cell foo_Type2_add(SedonaVM* vm, Cell* params)
    {
      int32_t a = params[0].ival;
      int32_t b = params[1].ival;
      Cell result;

      result.ival = a+b;

      return result
    }
```
An example implementation of the `foo::Type2.test` method:
```c
    Cell foo_Type2_test(SedonaVM* vm, Cell* params)
    {
      int32_t z = params[0].ival;
      int32_t b = params[1].ival;
      float   f = params[2].fval;

      printf("test %d %d %f\n", z, b, f);

      return nullCell;
    }
```
An example implementation of the (non-static) `foo::Type2.testf` method:
```cpp
    Cell foo_Type2_testf(SedonaVM* vm, Cell* params)
    {
      void* this = params[0].aval;   /* 'this' pointer is implicit first element of params[] */
      int32_t b  = params[1].ival;
      float   f  = params[2].fval;
      Cell result;

      result.fval = b*f;

      printf("test %d*%f=%f\n", b, f, result.fval);

      return result;
    }
```
Note in the examples above how each parameter is extracted using array
indexing and the union member name. Pointers (including strings) should
use the `aval` member, `floats` the `fval` member, and all other
primitives are accessed using the `ival` member. Note that a Sedona
`bool` maps into zero and non-zero for `false` and `true` respectively.
Arrays of primitives are accessed like their C counterparts.

Native methods that pass or return `long` or `double` are a bit
trickier. A single `long` or `double` value requires two Cells to store
the full 64-bits. To access a `long` or `double` function argument
requires the use of pointer casting to access two consecutive elements
of the parameter array. A native method that returns a `long` or
`double` should declare the return type to be `int64_t` instead of
`Cell`. The following is an example - note how each `long` parameter
actually consumes two cells of the parameter list:
```cpp
    native static long addTwoLongs(long a, long b)

    int64_t foo_Type3_addTwoLongs(SedonaVM* vm, Cell* params)
    {
      int64_t a = *(int64_t*)(params+0); // param 0+1
      int64_t b = *(int64_t*)(params+2); // param 2+3
      return a+b;
    }
```
A summary of common mappings from Sedona to their C equivalents:

| Sedona | C          |  | Sedona     | C            |
|--------|------------|--|------------|--------------|
| bool   | int32\_t   |  | bool\[\]   | uint8\_t\*   |
| byte   | int32\_t   |  | byte\[\]   | uint8\_t\*   |
| short  | int32\_t   |  | short\[\]  | uint16\_t\*  |
| int    | int32\_t   |  | int\[\]    | int32\_t\*   |
| long   | int64\_t   |  | long\[\]   | int64\_t\*   |
| float  | float      |  | float\[\]  | float\*      |
| double | double     |  | double\[\] | double\*     |
| Obj    | void\*     |  | Obj\[\]    | void\*\*     |
| Str    | uint8\_t\* |  | Str\[\]    | uint8\_t\*\* |

Note that strings can be used as a normal null terminated C string.

Refer to the [Porting](/development/porting#natives) chapter for how to
structure your native C code.

## Native Tables

When the SVM is compiled, the SVM is bound to a lookup table for all the
native methods available. This lookup table is a two level array of
function pointers. The first level of the array maps to the kitIds and
the second level maps to the methodIds. For example the function pointer
for the native id of "2::7" would be `nativeTable[2][7]`.

The native lookup table is automatically generated as "nativetable.c"
when `sedonac` is used to [stage a VM](/development/porting#staging).

## Additional Issues

The existing native method facility provides low level hooks to bind
Sedona Framework APIs into the native platform. However due to its low
level nature it maps fairly closely to the stack architecture of the VM.
This design has the major limitation that it only works well when
accessing primitives off the stack. There is currently no safe mechanism
to access individual fields of an Object within a native method, as you
would need to know exactly how the compiler will layout the memory (even
then it would be quite brittle). In the meantime the best practice is to
pass only primitives (or arrays of primitives) as parameters.

## Predefined Kit Ids

The following table shows some of the currently predefined native kit
ids. All PlatformService kits use a native kit id of 1.

| Kit             | Id |
|-----------------|----|
| sys             | 0  |
| *platform svcs* | 1  |
| inet            | 2  |
| serial          | 3  |
| basicio         | 4  |
| bacnet          | 5  |
| smbus           | 6  |
| spibus          | 7  |
| nrio            | 8  |
| datetimeStd     | 9  |
