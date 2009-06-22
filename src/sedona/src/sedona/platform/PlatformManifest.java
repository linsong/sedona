//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 June 09   Matthew Giannini  Creation
//
package sedona.platform;

import sedona.Depend;
import sedona.xml.*;

/**
 * Platform manifest represents the platform information and metadata for 
 * a platform.
 */
public class PlatformManifest
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public PlatformManifest()
  {
    this(null);
  }
  
  public PlatformManifest(final String platformId)
  {
    this.id = platformId;
    this.nativeKits = new Depend[0];
    this.nativeMethods = new NativeManifest[0];
    this.manifestIncludes = new XElem("manifestIncludes");
  }
  
//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Decode this platform information from an XML document.
   */
  public static PlatformManifest decodeXml(XElem xml)
  {
    if (!xml.name().equals("platformManifest"))
      throw new XException("Expected root to be <platformManifest>", xml);
    
    PlatformManifest m = new PlatformManifest();
    m.id     = xml.get("platformId", null);
    m.vendor = xml.get("vendor", null);
    m.endian = xml.get("endian", null);
    m.blockSize = xml.geti("blockSize");
    m.refSize   = xml.geti("refSize");
    m.armDouble = xml.getb("armDouble");
    m.debug     = xml.getb("debug");
    m.test      = xml.getb("test");
    
    // Native Kits (dependencies)
    XElem[] xkits = xml.elem("natives", true).elems("nativeKit");
    m.nativeKits = new Depend[xkits.length];
    for (int i=0; i<xkits.length; ++i)
      m.nativeKits[i] = Depend.parse(xkits[i].get("depend"));
    
    // Native Methods
    XElem[] xnative = xml.elem("natives", true).elems("native");
    m.nativeMethods = new NativeManifest[xnative.length];
    for (int i=0; i<xnative.length; ++i)
      m.nativeMethods[i] = NativeManifest.decodeXml(m, xnative[i]);
    
    // Manifest Includes
    m.manifestIncludes = xml.elem("manifestIncludes");
    
    return m;
  }
  
  /**
   * Encode this platform information into an XML document.
   */
  public void encodeXml(XWriter out)
  {
    out.w("<?xml version='1.0'?>\n");
    out.w("<platformManifest\n");
    if (id != null)     out.w("    ").attr("platformId", id).w("\n");
    if (vendor != null) out.w("    ").attr("vendor",     vendor).w("\n");
    if (endian != null) out.w("    ").attr("endian",     endian).w("\n");
    
    out.w("    ").attr("blockSize", blockSize).w("\n");
    out.w("    ").attr("refSize",   refSize).w("\n");
    out.w("    ").attr("armDouble", armDouble).w("\n");
    out.w("    ").attr("debug", debug).w("\n");
    out.w("    ").attr("test", test).w("\n");
    out.w(">\n");
    
    out.w("\n");
    out.w("<!-- Natives -->\n");
    out.w("<natives>\n");
    
    // native kits (dependencies)
    for (int i=0; i<nativeKits.length; ++i)
      out.w("  ").w("<nativeKit ").attr("depend", nativeKits[i].toString()).w(" />\n");
    out.w("\n");
    
    // native methods
    for (int i=0; i<nativeMethods.length; ++i)
      nativeMethods[i].encodeXml(out);
    out.w("</natives>").nl().nl();
    
    // Manifest Includes
    manifestIncludes.write(out);
    out.nl();
    
    out.w("</platformManifest>\n");
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public String id;
  public String vendor;
  public String endian;
  public boolean armDouble;
  public int refSize;
  public int blockSize;
  public boolean debug;
  public boolean test;
  
  public Depend[] nativeKits;
  public NativeManifest[] nativeMethods;
  public XElem manifestIncludes;

}
