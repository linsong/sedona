//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 June 09  Matthew Giannini  Creation
//
package sedonac.platform;

import sedona.Depend;
import sedona.xml.*;

public class PlatformDef
{
  
//////////////////////////////////////////////////////////////////////////
// Constructors
//////////////////////////////////////////////////////////////////////////
  
  public PlatformDef()
  {
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  public String vendor;     // platform vendor
  public String idPattern;  // platform id pattern before resolution
   
  public String endian;     // "big" or "little"
  public boolean armDouble; // 64-bit double layout of ARM using byte little endian, word big endian

  public int refSize;               // reference/pointer size of platform
  public int blockSize;             // block size to use for scode
  public boolean debug;             // include debug meta-data
  public boolean test;              // include testsp
  public boolean embedManifest;     // generate platformManifest.c

  public Depend[]  nativeKits;       // kits which supply native implementations
  public String[]  nativeFiles;      // source files for staging native code
  public String[]  nativePatches;    // list of native method qnames to patch
  public XElem[]   manifestIncludes; // resolved manifest include elements
  
}
