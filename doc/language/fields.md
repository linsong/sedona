
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Jun 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Fields

## Overview

Fields are storage locations used to store a class variable. The
following keywords may be applied to a field declaration:

-   `static`: class-based field (versus instance-based, see below)
-   `const`: indicates a field is immutable
-   `inline`: allocates memory for the variable instance
-   `define`: used to declare a constant variable
-   `property`: promotes a field to a component property

In addition to the keywords above, a field may be annotated with a
protection scope [keyword](/language/lang#protection-scope). If no protection
scope is specified, then `public` is assumed.

## Static versus Instance Fields

If a field is marked with the `static` keyword, then the field is stored
once for all instances of that class. Static fields are essentially
global variables scoped within a class type. Instance fields on the
other hand allocate storage for each instance of the declaring type:

```
class Display
{
  static int maxId = 5
  int id = -1
  int width
  int height
}
```

In the code above we declare a class called `Display` with three
instance fields `id`, `width`, and `height`. Each instance of `Display`
will store its own copies of these fields. The static field `maxId` has
one fixed memory location for the whole Sedona VM.

## Field Access

Access to instance fields is done through an implicit or explicit
instance pointer. The keyword `this` may be used to reference the
current instance inside an instance method. If no instance pointer is
used, then `this` is implied (only available inside an instance method):

```java
int area()
{
  return this.width * this.height  // this keyword
}

int area()
{
  return width * height  // shortcut for above
}

static int area(Display d)
{
  return d.width * d.height  // explicit instance pointer
}
```

Access to static fields is done through an implicit or explicit type
literal. Implicit static access (where the type name is not specified)
is only available inside methods of the `Display` class:

```java
static bool isValidId(int id)
{
  return id < maxId  // implicit static field access
}

static bool isValidId(int id)
{
  return id < Display.maxId  // explicit static field access
}
```

Also see [Safe Navigation](/language/expr#safe-navigation) for how to use the "?."
operator for field access.

## Field Defaults

Fields of a primitive type can declare a default expression. For example
in the `Display` class declared above, the `maxId` field defaults to `5`
and the `id` field defaults to `-1`.

Static fields are initialized when the Sedona VM is booted. The compiler
will automatically create a synthetic method called `_sInit()`, which
executes the initialization code for static fields.

Instance fields are initialized in the declaring class's
[constructor](/language/methods#constructors).

Any field without a default value has its memory initialized to zero. In
the case of `bool` fields it is `false`, for numeric fields it is 0, and
for references it is `null`.

## Const Fields

Some of the core types in the `sys` kit contain fields marked with the
`const` keyword. This indicates that these fields are immutable, and are
actually stored within the scode memory itself. On some platforms these
fields may be stored in readonly memory such as ROM. Attempts to set a
const field will result in a compiler error. You cannot create your own
const fields directly - only the predefined `sys` types can use this
keyword. However you can use the `define` keyword to declare user
defined constants.

## Define Fields

The `define` keyword is used to declare a named constant. Defines are
like const fields - if you try to assign to a define field you will get
a compiler error. Defines don't actually allocate memory, rather they
are inlined at compile time. Because of this trait, the value of a
define field must be expressed as a literal value:

```
class Flags
{
  define int tooBig   = 0x01
  define int tooSmall = 0x02
}
```

Defines are accessed just like static fields:

```java
if ((f & tooBig) != 0) return true        // implicit access
if ((f & Flags.tooBig) != 0) return true  // explicit access
```

The following types are supported for defines:

-   `bool`: `true`, `false`, or `null`
-   `int`: 32-bit integer literal value
-   `long`: 64-bit integer literal value (or time literal)
-   `float`: 32-bit float literal value
-   `double`: 64-bit float literal value
-   `sys::Str`: null terminated string literal
-   `sys::Log`: see [Logging](/language/logging)
-   Array literals: see [next section](#array-literals)

## Array Literals

The `define` keyword may be used to create constant array literals. This
allows you to declare readonly data lookup tables that will be stored in
scode. The following array literal types are supported:

-   `byte[]`
-   `short[]`
-   `int[]`
-   `long[]`
-   `float[]`
-   `double[]`
-   `Str[]`

Array literals are declared using curly braces with a comma separator.
The values inside an array literal must be literals themselves (they
cannot be expressions). Examples:

```
define byte[] daysInMonths = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}
define Str[] weekdays = {"sun", "mon", "tue", "wed", "thu", "fri", "sat" }
```

Array literals are treated like `const` structures and are read-only. It
is a compile time error to assign to a array literal (or any define).

## Inline Fields

A field with a non-primitive (i.e. object) type is normally a reference
to a `sys::Obj` object. Inside the SVM, an object field is a pointer to
an object. For example on a 32-bit machine, an object field would
allocate 4 bytes for a pointer. The object itself resides elsewhere in
memory.

However, often we wish the field to contain an *instance* of the object.
This is done with the `inline` keyword. Let's look at an example:

```java
class Point { int x; int y }

class Foo
{
  Point a
  inline Point b
}
```

In this example `Foo.a` is a pointer field - it allocates just enough
space to store a pointer to a `Point` instance (typically 4 bytes).
However, `Foo.b` actually allocates memory to store the entire `Point`
instance (8 bytes to store two 32-bit ints).

Inline fields play an important role in the Sedona Framework's static
memory management. They allow you to develop complex data structures
where memory is laid out by the compiler. This makes it possible for
tools to calculate ahead of time exactly how much memory a component
requires, since they can assume the component will not be allocating any
additional memory during runtime.

Inline fields can be static or instance based. Inline fields use the
same syntax as reference fields. However an inline field is not
assignable. You cannot point the reference to another instance since the
entire instance is already embedded (you will get a compiler error if
you try).

## Property Fields

Fields may be annotated with the `property` keyword to promote the field
into a Component property. Properties must be instance fields on a
subclass of `sys::Component`. See [Component Properties](/language/components#properties) for more details.
