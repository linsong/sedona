//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Feb 07  Brian Frank  Creation
//

package sedonac.scode;

/**
 * SCodeImage models a Sedona bytecode image in memory.
 */
public class SCodeImage
{
  public int endian;         // 'B' or 'L'
  public int blockSize;      // block size in bytes
  public int refSize;        // address pointer width in bytes
  public boolean armDouble;  // 64-bit double layout of ARM using byte little endian, word big endian
  public String main;        // main method qname
  public String resume;      // resume method qname
  public boolean debug;      // include debug meta-data
  public boolean test;       // include tests
  public byte[] code;        // image file
}
