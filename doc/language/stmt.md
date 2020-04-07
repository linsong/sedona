
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    2 Jun 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Statements

## Overview

Statements are very close to those found in C or Java:

-   [Expression](#expression): expression statement
-   [Locals](#local-variables): declare a local variable
-   [Return](#return): exit from a method
-   [If/Else](#if-else): conditional branching
-   [While](#while): conditional looping
-   [For](#for): just like C `for` statement
-   [Foreach](#foreach): array iteration
-   [Goto](#goto): unconditional branching
-   [Switch](#switch): jump tables
-   [Assert](#assert): used for unit testing

Unlike C or Java, statements are not required to be terminated with a
semicolon. By convention they are terminated with a newline, although
they can also be terminated with a semicolon or "}". All of these are
valid statements:
```java
    if (c)
    {
      doSomething()
      orAnother()
    }

    if (c)
    {
      doSomething();
      orAnother();
    }

    if (c) { doSomething(); orAnother() }
```
Note that Sedona's grammar is not always unambiguous when terminating a
statement with a newline. So on occasion you might be required to use a
semicolon. If you get a weird compiler error you don't understand try
sticking in a semicolon. This might happen if you have a statement that
starts with a parenthesis such as `((Type)x).something()`.

## Expression

The most common type of statements are stand alone expressions. The
following expressions can be used as a statement:

-   Assignment: `x = y, x++, x[i] = y, x.f += y`
-   Method calls: `x()`

## Local Variables

Local variables are declared as follows:

    float f
    int i = 5
    Component c = doSomething()

Local variables are scoped within their block - they are not visible to
parent blocks. A local variable must be definitely assigned before it is
used or the compiler will generate an error. Sedona doesn't currently
support declaring multiple local variables in one statement like C or
Java.

## Return

The return statement is used to exit a method. If a return is used in a
non-void method, then it must specify the expression to return:

    // void return
    return

    // non-void return
    return expr

## If Else

Sedona supports C style if/else statements:

    // if, no else
    if (cond)
      block

    // if-else
    if (cond)
      block
    else
      block

    // if, if-else, else
    if (cond)
      block
    else if (cond)
      block
    else
      block

Like C/Java, each block may be a single statement, or a block of
statements wrapped in "{ }" curly braces.

## Looping

Sedona supports the traditional while, do-while, and for loops of C like
languages. Sedona also has a foreach statement for iterating arrays. In
each case, the repeated code can be a single statement or a sequence of
statements wrapped in "{ }" curly braces.

### While

The while statement is used to loop while a boolean expression evaluates
to true:

    while (cond)
      block

### Do While

The do-while statement works just like the while loop, except the
condition is evaluated at the end of the block. It is typically used
when you wish to guarantee the loops executes at least once:

    do
      block
    while (cond)

### For

The for statement works like a while loop except it allows an
initialization expression and an update expression that executes after
each loop iteration:

    for (init; cond; update)
      block

    // example - prints 0 to 4
    for (int i=0; i<5; ++i)
      Sys.out.print("i = $i\n")

Any of the init, cond, or update expressions may be omitted. If the cond
is omitted, then it defaults to true and the loop must be terminated
with a break or return statement inside the block.

### Foreach

Sedona provides the foreach statement to iterate through an array. Using
the foreach statement is more efficient than using a for loop because is
compiles into a specialized instruction. The foreach statement takes a
local variable declaration, an array to iterate, and an optional array
length:

    foreach (type var : array)
      block

    foreach (type var : array, length)
      block

    // example - prints each kit name and version
    foreach (Kit kit : Sys.kits, Sys.kitsLen)
      Sys.out.print("$kit.name  $kit.version").nl()

The length may be omitted only if the compiler can infer the length of
the array from its declaration.

### Break

The break statement is used to exit a loop. It may be used with any
looping statement (while, do-while, for, and foreach):

    // prints 0 to 3, then breaks
    for (int i=0; i<10; ++i)
    {
      if (i == 4) break
      Sys.out.print("i=$i\n")
    }

If a break is used inside nested loops, it breaks from the inner-most
loop. Labeled breaks are not supported.

### Continue

The continue statement causes the next iteration of the containing loop
to begin. For a while and do-while, the loop condition is executed
immediately. For a for or foreach loop, control is passed to the
increment step.

    // prints 0, 1, 3 (skips 2)
    for (int i=0; i<4; ++i)
    {
      if (i == 2) continue
      Sys.out.print("i=$i\n")
    }

If a continue is used inside nested loops, it continues the inner-most
loop. Labeled continues are not supported.

## Goto

The goto statement performs an unconditional jump to a labeled statement
within the same method:

    // this method always returns "y"
    static Str f()
    {
      goto foo
      return "x"
      foo: return "y"
    }

## Switch

The switch statement works just like C and Java. It evaluates an integer
expression and jumps to a cast label:

    Str s = null
    switch (i)
    {
      case 0:
      case 1:
        s = "0 or 1"
        break
      case 2:
        s =  "2"
        break
      default:
        s = "not 0, 1, or 2"
    }

Like C, case blocks fall through to the next case block - use a break
statement to break out of the switch statement. The default block
handles any value without an matching case label. The default block is
optional; if omitted then unmatched values fall through to the next
statement after the switch. However, if the default block exists, it
must be the last label in the switch statement.

If the last case statement does not break and there is a default block,
the last case statement will fall through into the default block.

    switch(i)
    {
      case 0:
        reset()
        break
      case 1:
        init() // fall through to default block
      default:
        work() // whenever i is not zero
    }

### Limits on Case Values

To reduce code size, case values must not be too widely spaced. For a
difference `delta` between the minimum and maximum case values, and
total number of case values `num`, at least one of the following must be
true:

-   `delta <= 30`
-   `num*3 >= delta`

It is a compile time error if both conditions are false. If you get this
compiler error, you should change your code to use the
`if/else if/.../else` idiom.

## Assert

The assert statement is used for [unit testing](/development/testing).

    assert(cond)

The condition must be a boolean expression. Failed assertions result in
a call to the Sedona VM's `onAssertFailure` callback. On most
platforms, this will print a message to stdout. See
[Porting](/development/porting#bootstrap) for more details.
