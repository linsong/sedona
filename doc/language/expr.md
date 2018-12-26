
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    12 Sep 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Expressions

## Overview

Expressions are almost identical to those found in C or Java. The same
operator precedence rules apply:

| Expression(s)  | Operator(s) |
|----------------|----------------------------------|
| Accessors      | `. ?. () []` |
| Unary          | `- ! ~ ++ -- (cast)` |
| Multiplicative | `* / %` |
| Additive       | `+ -` |
| Bitwise Shift  | `>> <<` |
| Elvis          | `?:` |
| Relational     | `< <= > >=` |
| Equality       | `== !=` |
| Bitwise AND    | `&` |
| Bitwise XOR    | `^` |
| Bitwise OR     | `|` |
| Logical AND    | `&&` |
| Logical OR     | `||` |
| Ternary        | `? :` |
| Assignment     | `= += -= *= /= |= ^= &= >>= <<=` |


## Arithmetic Operators

The following arithmetic operators may be used with numeric primitives:

| Name                | Operator |
|---------------------|----------|
| Multiplication      | `x * y`  |
| Division            | `x / y`  |
| Modulus (remainder) | `x % y`  |
| Addition            | `x + y`  |
| Subtraction         | `x - y`  |
| Negation            | `-x`     |
| Prefix Increment    | `++x`    |
| Prefix Decrement    | `--x`    |
| Postfix Increment   | `x++`    |
| Postfix Decrement   | `x--`    |

The Modulus operator is only available for integer types, it may not be
used with floats or doubles.

The prefix/postfix operators work just like C and Java. If using the
prefix operator the result of the expression is the new value. If using
the postfix operator the result is the old value:

      int x = 4    // x == 4
      int y = ++x  // x == 5, y == 5
      int z = x++  // x == 6, z == 5

## Bitwise Operators

The following bitwise operators may be used with integer primitives
(byte, short, int, long):

| Name        | Operator |
|-------------|----------|
| Or          | `x | y`  |
| Xor         | `x ^ y`  |
| And         | `x & y`  |
| Left Shift  | `x << y` |
| Right Shift | `x >> y` |
| Not         | `~x`     |

## Logical Operators

The following logical operators may be used with booleans:

| Name | Operator |
|------|----------|
| Or   | `x || y` |
| And  | `x && y` |
| Not  | `!x`     |

Both the Or and And operators are short circuiting. If the first term of
Or evaluates to true, then the second term is not evaluated. If the
first term of And evaluates to false, then the second term is not
evaluated.

## Comparison

The following operators are used for comparison:

| Name                  | Operator |
|-----------------------|----------|
| Equal                 | `x == y` |
| Not Equal             | `x != y` |
| Greater Than          | `x > y`  |
| Greater Than or Equal | `x >= y` |
| Less Than             | `x < y`  |
| Less Than or Equal    | `x <= y` |

Non-numeric types may only use the equality (`==` and `!=`) operators.
Reference types compare identity (pointer address).

## Assignment

The `=` operator is used to assign the right hand side expression to the
left hand side. The left hand side must be *assignable*. Assignable
expressions are local variables, fields, and array indices.

The compound assignment operators `+= -= *= /= |= ^= &= >>= <<=` can be
used to combine an arithmetic or bitwise operator with assignment:

      int x = 5   // x == 5
      x += 3      // x == 8

### Assigning to Properties

If the left hand side of an assignment expression is a [component
property](/language/components#properties) then you must use the **:=**
assignment operator instead of **=**. This highlights the fact that
assigning a new value to a property results in more than just the
storage of the new value; it also has potential side effects that may
not always be visible to the user.

Note that there are no compound property assignment operators;
increment, decrement, etc must be spelled out explicitly in the
expression.
```java
    // Property px
    property int px

    // Non-property field ix
    int ix

    void myfunc()
    {
      // Use := when lhs is a property
      px := 3

      // Use = when lhs is a non-property
      ix = px

      // No compound assignment operators
      px := px + 1
    }
```

## Casting

Sedona uses a syntax just like C or Java to perform a cast. Casts are
required when the compiler cannot perform a static type check. For
example, if you need to assign an Obj to a Component, then you must use
a cast: `c = (Component)obj`. This type of cast is for compile time
checking only, at runtime it is basically a no-op like C.

Casts are also used with numeric types to perform a type conversion. For
example to convert an int into a float: `f = (float)i`. Note that unlike
C and Java, upcasts such as from an int to a long are not implicit - you
must be explicitly use a cast.

## Safe Navigation

Sedona supports Groovy's *safe navigation* operator: `x?.slot`. You can
use the safe nav operator to access fields or call methods. If the
target expression of a slot access is null, then the whole expression
short circuits to evaluate to null. If the field or method returns a
primitive then it short circuits to false/zero. Using the safe nav
operator is a convenient and more efficient way than manually checking
for null:

```
      // hard way
      DeviceNetwork net = null
      if (point != null)
      {
        Device dev = point.getDevice()
        if (dev != null) net = dev.getNetwork()
      }

      // easy way
      DeviceNetwork net = point?.getDevice()?.getNetwork()
```

## Elvis

Sedona supports Groovy's *elvis* operator: `lhs ?: rhs`. If lhs
evaluates to null, then the whole expression evaluates to rhs. If lhs is
non-null, then the rhs is short circuited and the whole expression
evaluates to lhs. The elvis operator is a convenient and more efficient
way to write code where you might use the ternary operator:

```
      // hard way
      name != null ? name : "unknown"

      // easy way
      name ?: "unknown"
```

## Ternary

Sedona supports the C/Java ternary operator: `c ? t : f`. The boolean
expression `c` is evaluated. If true then the ternary expression
evaluates to `t`, otherwise it evaluates to `f`:

```
      bool x = true
      Str msg = x ? "On" : "Off"   // msg == "On"
      x = false
      msg = x ? "On" : "Off"       // msg == "Off"
```

## String Interpolation

Sedona supports string interpolation, which allows for concise string
formatting. String interpolation can be used with any method that has
one `Str` parameter and returns an `OutStream`. You may use the `"+"`
operator to concatenate multiple expressions to a string literal:

      // using the + operator
      int x = 77
      Sys.out.print("x=" + x)

      // is equivalent to this statement
      Sys.out.print("x=").printInt(x)

The example above illustrates why interpolation is only used with
methods that take a `Str` and return an `OutStream`. The compiler
doesn't actually create a new string, rather it chains multiple print
calls.

Use of the "+" operator is allowed, but the preferred mechanism for
string formatting is to embed the expressions directly into the string
literal itself using the `'$'` character. You can embed any arbitrary
expression into a string literal using the syntax `"${expr}"`. If the
expression is a simple variable name or a variable name followed by a
dot field access, then you can omit the curly braces. Use the `'\$'`
escape sequence to print the dollar sign itself. Some examples:

```java
      // example from above
      Sys.out.print("x=${x}")

      // same but omitting the braces
      Sys.out.print("x=$x")

      // embedded expressions
      Sys.out.print("x=0x${Sys.hexStr(x)}")
```

The following types are supported with string interpolation:

| Type     | Print Method            |
|----------|-------------------------|
| `Str`    | `OutStream.print`       |
| `bool`   | `OutStream.printBool`   |
| `int`    | `OutStream.printInt`    |
| `long`   | `OutStream.printLong`   |
| `float`  | `OutStream.printFloat`  |
| `double` | `OutStream.printDouble` |
