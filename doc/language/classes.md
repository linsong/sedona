
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Jun 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Classes

## Overview

Classes are the primary way you organize your code within kits. Classes
are named containers for fields and methods. A field or method
definition is called a slot and identified by a unique name within a
given class. Slots may not be overloaded by name, which means you cannot
declare a field and method to have the same name (allowed in Java). Nor
can you declare multiple methods with the same name overloaded by
parameters (allowed in both Java and C\#).

You can declare multiple classes in a single source file, although by
convention each class is placed in its own source file with the same
name. For example the class "Thing" would be declared in the file
"Thing.sedona".

## Modifiers

By default a class is `public`, or you can explicitly use the `public`
keyword. Alternatively use the `internal` keyword to declare a class
that only has visibility within its kit.

The `abstract` keyword is used with classes that can never be
instantiated directly. Any class that declares [abstract methods](/language/methods#abstract-methods) must be marked abstract.

Use the `final` class modifier to prevent a class from being subclassed.

## Inheritance

The Sedona Framework supports inheritance much like Java or C\#.
However, there is no support for interfaces or any type of multiple
inheritance.

Similar to Java, all Sedona classes implicitly extend `sys::Obj`, a
class with no fields or methods of its own. To support polymorphism and
virtual methods, however, a Sedona class (or some class in its
hierarchy) must explicitly extend `sys::Virtual`, which allocates space
for the virtual table.

The syntax for inheritance looks just like Java with the `extends`
keyword:

```java
    abstract class Shape extends Virtual
    {
      abstract int area()
    }

    class Square extends Shape
    {
      override int area() { return width * width }
      int width
    }
```

Note that extending `Virtual` has some memory impact: In addition to the
space required for the type's virtual table, it imposes a two byte
overhead per class instance.
