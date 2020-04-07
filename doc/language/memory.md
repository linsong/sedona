
<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    2 Jun 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Memory

## Overview

Utilizing memory as efficiently as possible is a core requirement for
making the Sedona Framework run on small, embedded devices. We divide
memory into the following sections:

-   **Stack**: the call stack (RAM)
-   **Data**: all the memory declared as static fields in the current
    scode (RAM)
-   **Sedona Framework App**: the components that define the current
    application (RAM)
-   **Sedona Framework Code**: the scode image for the installed kits
    (RAM, or possibly flash memory)
-   **SAB File**: the compact binary representation of the application
    (usually stored in flash)
-   **C Code**: native code and SVM executable (RAM, or possibly flash)
-   **C Data**: native code data segment (RAM)
-   **C Stack**: the C native call stack (RAM)

We use the terms RAM and flash a bit loosely here: RAM must be high
speed read/write memory, and flash is persistent memory (on a large
device this could be a disk drive). If flash is high speed you might be
able to run code directly out of flash, but normally those code segments
will be loaded into RAM.

While reading this topic you may find it useful to refer to the Sedona
VM source code, located in the `src/vm` folder.

## Computing Memory Requirements

So how do you figure out how much memory each of the sections declared
above requires? Let's look at each section:

### C Code, C Data, and C Stack

The C code, data, and stack segments will be determined by your
development tools and C compiler. We use the term "C Code" to denote
any non-Sedona code such as C, C++, or assembly. This code typically
includes boot code, OS code (if your device has one), all your native
method implementations, comm stacks, and the SVM itself. All this is
normally lumped into a single binary executable file called "svm". The
C Data segment includes all the memory buffers used by this code.

**Note:** On some platforms, part of the SVM may already be in ROM or
other permanent storage. In that case the "svm" executable created
during compilation will contain only the portion of the SVM that is not
in ROM, and its RAM footprint will be reduced.

### Stack

The Sedona stack is defined by the `stackBaseAddr` and `stackMaxSize`
fields of the `SedonaVM` struct when the VM is launched. Typically the
size is one or two KB - you may wish to test your application to find
the maximum call stack. Heavy use of recursion and methods with a large
number of locals will affect your call stack size.

### Code and Data

The sizes of the Sedona Code and Data segments are calculated when the
scode image is compiled:

```shell
    D:\sedona>sedonac scode\x86-test.xml
      ReadKits [5 kits]
      WriteImage [D:\sedona\scode\x86-test.scode] (29632 bytes)
      +----------------------------------
      |  Data:      0.4kb (360 bytes)
      |  Code:     28.9kb (29632 bytes)
      |  Total:    29.3kb (29992 bytes)
      +----------------------------------
```

These numbers are directly related to how many kits you include and the
code size of those kits. Declaring static fields will consume memory in
your Data segment; instance fields only affect the application size. If
you are running your code out of RAM, then the Total size is what
matters. If you can run your code out of flash, then the Code will not
use any RAM but the Data segment must still be stored in RAM. Note that
the scode flags `test` and `debug` can have a huge impact on scode size.

The location of the Code is passed to the VM in the `SedonaVM` struct
via the `codeBaseAddr` field. The Data section is allocated by the
`malloc` macro - see "sedona.h" for details.

### Sedona Framework Application

The Sedona Framework application itself is always run out of RAM. This
is where we instantiate the components and links. Memory for components
and links is allocated by the `malloc` macro. If components or links are
removed during runtime, then the memory is freed by the `free` macro (if
the platform supports it).

The Sedona Framework application runs out of RAM, but has configuration
data that must be persistent between power cycles. So we also store the
application to flash as a [SAB file](/apps/apps#sab-files). How this file is
loaded on startup and stored back to flash on save is handled by the
platform's implementation of the `sys::FileStore` native methods.

Sedonac will report how much RAM and flash a given application consumes
when you do a conversion between SAX or SAB:

```shell
    D:\sedona>sedonac apps\test.sax
      ConvertAppFile [D:\sedona\apps\test.sax -> D:\sedona\apps\test.sab]
      +----------------------------------
      |  RAM:     14.2kb (14588 bytes)
      |  FLASH:    0.4kb (382 bytes)
      +----------------------------------
```

See the [Field Layout](#field-layout) section to evaluate how much memory each
component type consumes.

## Heap

The memory for the Data section and for the components and links in the
App is allocated using the `malloc` macro as defined in "sedona.h". If
components or links are removed during runtime, then this memory is
freed by the `free` macro. You can implement your "heap management"
using three strategies:

1.  **Stdlib**: if you have the resources, you can just use C's
    built-in malloc and free. However if using a compiler like GCC this
    might require importing a huge chunk of library code into your
    native image.
2.  **Custom Heap**: if the stdlib implementation of malloc and free is
    too big, you might consider writing your own simple heap manager.
3.  **Malloc Only**: many devices might require only a static
    application in which case there is no requirement for freeing
    memory. In resource limited devices, the ability to free memory
    might be outweighed by the risk of heap fragmentation. In this case
    your heap management might be nothing more than a pointer to the
    next chunk of memory to allocate. A call to free would be a no-op.
    If objects are removed from the application the memory would not be
    recovered until the system is reset.

## Field Layout

You can pass the `-layout` flag to sedonac when compiling your scode
image to dump the memory layout of each type. This gives you exact
details for how memory is being consumed:

-   How many bytes each component type consumes in RAM
-   Memory offset of every instance field against the object's base
    address
-   Memory address of every static field against the Data base address

A good rule of thumb is that each component averages between 50 and 100
bytes and each link consumes 16 bytes. A network protocol service will
typically consume several KB since it must allocate buffers and internal
data structures. However, you really must use the `-layout` flag to see
exactly how many bytes each component will consume.

Note that sedonac has no visibility into the behavior of native methods.
Any dynamic memory allocation that occurs at the native level must be
calculated separately and added manually to the totals provided by the
`-layout` flag.

## Hibernation

The Sedona Framework supports *hibernation*, which allows a device to
enter a low power state. The Sedona Framework assumes that during
hibernation its data section in RAM (managed by the heap `malloc` and
`free` calls) will not be affected. The device can either maintain RAM
during hibernation or save and restore to the same memory addresses. See
[Hibernation](/apps/apps#hibernation).
