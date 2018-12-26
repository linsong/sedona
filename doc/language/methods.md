
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Jun 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
## Overview

Methods are functions used to implement executable behavior. The
following keywords may be applied to a method declaration:

-   `static`: class based method versus instance based
-   `virtual`: indicates method that may be polymorphically overridden
-   `abstract`: indicates a pure virtual method
-   `override`: required to indicate an override of an inherited method
-   `native`: indicates a method implemented in native C code
-   `action`: promotes a method to a component action

In addition to the keywords above, a method may be annotated with a
[protection scope](/language/lang#protection-scope) keyword. If no protection
scope is specified, then `public` is assumed.

## Return Values

If the method returns a value to the calling function, the return value
type is included in the function definition. If the method does not
return anything, the keyword `void` is used instead of a return type.

    class Example
    {
      void echo() { Sys.out.print("echo").nl() }   // returns nothing
      int add(int x, int y) { return x + y }       // returns an int value
    }

In Sedona, a method may return any primitive type, or a reference to any
built-in or user-defined class type; the only exception is an `action`
method, which always returns `void`.

## Static Methods

Static methods are prefixed with the `static` keyword. Static methods
are essentially global functions scoped within a class name. They are
declared just like Java methods:

    class Example
    {
      static void echo() { Sys.out.print("echo").nl() }
      static int add(int x, int y) { return x + y }
    }

Static methods are called with an implict or explicit type literal:

    Example.echo()       // explicit
    int five = add(2, 3) // implicit (only inside Example or subclasses)

## Instance Methods

Instance methods are declared whenever the `static` keyword is omitted.
Instance methods contain an implicit `this` parameter, which is the
instance the method is called on:

    class Example
    {
      int add() { return x + y }
      int sub() { return this.x - this.y }
      int x
      int y
    }

Note in the example, that every instance method has an implicit
parameter accessed via the `this` keyword. Instance methods are called
with an implict or explicit instance:

    add()        // implicit against this
    this.sub()   // explicit against this
    x.sub()      // explicit against x
    x?.sub()     // null safe call

See [Safe Navigation](/language/expr#safe-navigation) for how to use the "?."
operator.

## Virtual Methods

Virtual methods are designed to be overridden by a subclass to enable
polymorphism. Methods must be marked using the `virtual` keyword before
they can be overridden by subclasses. Subclasses must declare they are
overriding a method using the `override` keyword:
```
    class Animal extends Virtual
    {
      virtual void talk() { Sys.out.print("generic\n") }
    }

    class Cat extends Animal
    {
      override void talk() { Sys.out.print("meow\n") }
    }

    animal.talk()   // prints generic
    cat.talk()      // prints meow
```

By default an overridden method cannot itself be overridden by a
subsequent subclass. In order for `Cat.talk()` to be overridden by the
subclass `Kitten`, it must include again the keyword `virtual`:

```
    class Cat extends Animal
    {
      override virtual void talk() { Sys.out.print("meow\n") }   // override AND virtual
    }

    class Kitten extends Cat
    {
      override void talk() { Sys.out.print("mew!\n") }           // this now compiles
    }

    kitten.talk()      // prints mew!
```

Classes that declare virtual methods must derive from `sys::Virtual`. Be
aware that virtual classes have the overhead of an extra pointer for
their vtable (typically 4 extra bytes).

## Abstract Methods

Abstract methods are virtual methods without an implementation. They are
declared using the `abstract` keyword. Abstract methods are implied to
be virtual - it is an error to use both the `abstract` and `virtual`
keyword. Abstract methods must not provide a method body. The containing
class must also be declared `abstract`.

## Super

By default any virtual method call with an implicit or explicit target
of `this` invokes the most specific version of that method. You can use
the `super` keyword to invoke the super class version of a method:

    class Kitten extends Cat
    {
      override void talk() { super.talk() }
    }

    kitten.talk()      // now prints meow

## Constructors

A class may have one constructor, which is compiled into a method called
`_iInit`. Whenever a class declares instance fields with a [default
value](/language/fields#field-defaults), the compiler auto-generates a constructor
for you. A class may declare an explicit constructor using a syntax
similiar to Java:

```
    final class BufInStream extends InStream
    {
      BufInStream(Buf abuf) { this.buf = abuf }
      Buf buf
    }
```

To keep Sedona lightweight and simple, the following rules apply to
constructor methods:

-   A class may only declare one explicit constructor (parameter
    overloading is not supported).
-   A class with an explicit constructor must be marked `final`.
-   Subclasses of `sys::Component` cannot declare a constructor with
    parameters.
-   Declared constructors are used with [unsized
    classes](/language/arrays#unsized-classes) to specify an array length.

The constructor method for a class is called whenever an object of that
type is instantiated. For static inline fields, this happens as soon as
the scode is loaded when the Sedona VM boots up. For instance inline
fields, this happens when the Sedona VM loads the Sedona application and
calls the constructors for the application's components as well as each
component's inline object fields. If the running app is modified
remotely, however, any new components will be instantiated immediately
by `App.add()`, which will call the component's constructor (and all
its non-static inline object fields) at that time.

For example:

```
    class Foo
    {
      // static constructor calls
      static inline Buf(100) buf
      static inline BufInStream(buf) in
      static inline BufOutStream(buf) out
      static inline Foo inst

      // instance constructor calls
      inline Buf(20) ibuf
    }
```

Inline static fields are initialized in declaration order on VM startup,
which calls the appropriate constructors. These constructor calls often
result in instance constructor calls, which in turn recursively chain
for nested inline fields. For the example above, the compiler will
create the following code; `Foo._sInit` is then automatically called
when the VM boots:

```java
    static void Foo._sInit()
    {
      Foo.buf._iInit(100)
      Foo.in._iInit(Foo.buf)
      Foo.out._iInit(Foo.buf)
      Foo.inst._iInit()
    }

    void Foo._iInit()
    {
      this.ibuf._iInit(20)
    }
```

## Native Methods

The `native` keyword is used on methods that are implemented in C code.
Like abstract methods, native methods must not define a body. See
[Native Methods](/language/nativeMethods) for more details.

## Action Methods

Methods may be annotated with the `action` keyword to promote the method
into a Component action. Actions must be instance methods on a subclass
of `sys::Component`. See [Component Actions](/language/components#actions)
for more details.
