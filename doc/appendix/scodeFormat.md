
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    6 Mar 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)

# SCode Format

## Image Format

    image
    {
      header  header
      item[]  items   // each item aligned by swSize
    }

    header
    {
      0:   u4    magic: 0x5ED0BA07 for big endian, 0x07BAD05E for little endian
      4:   u1    majorVer: 1
      5:   u1    minorVer: 0
      6:   u1    blockSize:  addr = blockIndex*blockSize + codeBaseAddr
      7:   u1    refSize: num bytes in address pointers
      8:   u4    imageSize: num bytes of whole image including full header
      12:  u4    dataSize: num bytes for static field data
      16:  bix   main method block index
      18:  bix   test table
      20:  bix   kits array block index
      22:  u1    number of kits in kits array
    }

    item
    {
      vtable | kit | type | int | float | long | double | str |
      slot | log | method | tests | qnameType | qnameSlot
    }

    vtable
    {
      u2[]  block indexes to each virtual method
    }

    kit
    {
      // see sys::Kit field memory layout
    }

    type
    {
      // see sys::Type field memory layout
    }

    slot
    {
      // see sys::Slot field memory layout
    }

    log
    {
      // see sys::Log field memory layout
    }

    int
    {
      0: s4  32-bit integer constants
    }

    long
    {
      0: s8  64-bit integer constants
    }

    float
    {
      0: f4  32-bit float constants
    }

    double
    {
      0: f8  64-bit double constants
    }

    str
    {
      0: u1[]  ASCII char string terminated by 0 (C string literal)
    }

    method
    {
      0: u1      numParams (including implicit this)
      1: u1      numLocals
      2: u1[]    opcodes
    }

    tests
    {
      u2     count
      test[] table
    }

    test
    {
      u2:  qnameSlot
      u2:  test method
    }

    qnameType
    {
      u2:  kit name str index
      u2:  type name str index
    }

    qnameSlot
    {
      u2:  qnameType index
      u2:  slot name str index
    }
