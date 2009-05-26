//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Dec 07  Brian Frank  Creation
//

package sedona.platform;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * Platform defines the meta-data for provisioning 
 * a specific device platform.
 */
public class Platform
{ 
  
////////////////////////////////////////////////////////////////
// Construction
////////////////////////////////////////////////////////////////
  
  /**
   * Constructor.
   */                 
  public Platform(String id)
  {
    this.id = id;              
  }              
  
////////////////////////////////////////////////////////////////
// Load
////////////////////////////////////////////////////////////////

  /**
   * Decode from an XML manifest document.
   */
  public void load(XElem xml)       
    throws XException
  {   
    // <sedonaPlatform>
    if (!xml.name().equals("sedonaPlatform"))
      throw new XException("Root element must be <sedonaPlatform>", xml);
            
    // <compile>
    loadCompile(xml.elem("compile"));
            
    // <include>
    XElem[] includes = xml.elems("include");
    for (int i=0; i<includes.length; ++i)
      loadInclude(includes[i]); 
    
    // sanity
    sanityCheck(xml);
  }                     
  
  /**
   * Load scode compiler options.
   */
  private void loadCompile(XElem xml)
    throws XException
  {                            
    if (xml == null) return;    
    refSize   = xml.geti("refSize", refSize);
    blockSize = xml.geti("blockSize", blockSize);
    endian    = xml.get("endian", endian);
    armDouble = xml.getb("armDouble", armDouble);
    debug     = xml.getb("debug", debug);
    test      = xml.getb("test", test);
    
    XElem[] x = xml.elems("nativeKit");
    Depend[] kits = new Depend[x.length];
    for (int i=0; i<x.length; ++i)
      kits[i] = x[i].getDepend("depend");
    nativeKits = (Depend[])ArrayUtil.concat(nativeKits, kits);
      
    x = xml.elems("nativeSource");
    String[] paths = new String[x.length];
    for (int i=0; i<x.length; ++i)
      paths[i] = x[i].get("path");
    nativePaths = (String[])ArrayUtil.concat(nativePaths, paths);
    
    x = xml.elems("nativePatch");
    String[] slots  = new String[x.length];
    for (int i=0; i<x.length; ++i)
      slots[i] = x[i].get("qname");
    nativePatches = (String[])ArrayUtil.concat(nativePatches, slots);
  }       
  
  /**
   * Load a include by recursively loading myself with its xml.
   */
  private void loadInclude(XElem xml)
    throws XException
  {
    String id = xml.get("id");
    XElem include = PlatformDb.loadXml(id);
    if (include == null)
      throw new XException("Cannot resolve include: " + id, xml);
    load(include);
  }  
  
  /**
   * Check that key meta-data is correct.
   */
  private void sanityCheck(XElem xml)
    throws XException
  {
    if (refSize == 0) throw new XException("compile.refSize not defined", xml);
    if (blockSize == 0) throw new XException("compile.refSize not defined", xml);
    if (endian == null) throw new XException("compile.endian not defined", xml);
    if (!endian.equals("big") && !endian.equals("little"))                      
      throw new XException("compile.endian must be 'big' or 'little'", xml);
    if (nativeKits == null || nativeKits.length == 0) 
      throw new XException("no compile.nativeKit elements defined", xml);
  }
    
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final String id;        // platform identifier
  public String endian;          // "big" or "little"
  public boolean armDouble;      // 64-bit double layout of ARM using byte little endian, word big endian
  public int refSize;            // reference/pointer size of platform
  public int blockSize;          // block size to use for scode
  public boolean debug;          // include debug meta-data
  public boolean test;           // include tests
  public Depend[] nativeKits;    // kits which supply native implementations
  public String[] nativePaths;   // source paths for staging native code
  public String[] nativePatches; // list of native method qnames to patch 
            
}
