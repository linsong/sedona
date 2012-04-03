//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 June 09   Matthew Giannini  Creation
//
package sedona.platform;

import sedona.Depend;
import sedona.util.VendorUtil;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedona.xml.XWriter;

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
    m.vendor = xml.get("vendor");
    m.endian = xml.get("endian");
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
    XElem[] xnativeMethods = xml.elem("natives", true).elems("nativeMethod");
    m.nativeMethods = new NativeManifest[xnativeMethods.length];
    for (int i=0; i<xnativeMethods.length; ++i)
      m.nativeMethods[i] = NativeManifest.decodeXml(m, xnativeMethods[i]);
    
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
// Utility
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Validates the given platform manifest to see if it would be valid
   * in a PAR file. The following checks are done and an Exception is 
   * thrown if any one of them fails.
   * <ol>
   * <li>manifest cannot be {@code null}
   * <li>{@code manifest.id} cannot be {@code null}
   * <li>{@code manifest.vendor} cannot be {@code null}
   * <li>{@link sedona.util.VendorUtil#checkVendorName(String)} validates
   * <li>{@link sedona.util.VendorUtil#checkPlatformPrefix(String, String)} validates
   * <li>The manifest id must be less than 128 characters long
   * </ol>
   * 
   * @throws Exception Thrown if the manifest fails validation for any
   * of the reasons listed above.
   */
  public static void validate(PlatformManifest manifest) throws Exception
  {
    if (manifest == null) throw new Exception("null manifest");
    if (manifest.id == null) throw new Exception("manifest doesn't specify a platform id");
    if (manifest.vendor == null) throw new Exception("manifest doesn't specify a vendor");
    VendorUtil.checkVendorName(manifest.vendor);
    VendorUtil.checkPlatformPrefix(manifest.vendor, manifest.id);
    if (!(manifest.id.length() < 128))
      throw new Exception("platform id '" + manifest.id + "' must be less than 128 characters long: " + manifest.id.length());
  }

//////////////////////////////////////////////////////////////////////////
// Scheme metadata
//////////////////////////////////////////////////////////////////////////

  /**
   * Checks the manifest includes to see if scheme information is present, and
   * returns a list of schemes the platform declares that it implements.
   *
   * @return a non-null String[] listing all schemes the platform implements.
   * The array may be empty if no schemes are declared.
   */
  public String[] getSchemes()
  {
    String[] schemes = new String[0];
    if (manifestIncludes == null)
      return schemes;

    XElem xschemes = manifestIncludes.elem("schemes");
    if (xschemes == null)
      return schemes;

    XElem[] xlist = xschemes.elems("scheme");
    schemes = new String[xlist.length];
    for (int i=0; i<xlist.length; ++i)
      schemes[i] = xlist[i].get("id");
    return schemes;
  }

  /**
   * Return true if the platform declares that it implements the given scheme.
   *
   * @param scheme the scheme name to check
   * @return true if the platform declares that it implements the given scheme.
   */
  public boolean implementsScheme(String scheme)
  {
    String[] schemes = getSchemes();
    for (int i=0; i<schemes.length; ++i)
      if (schemes[i].equals(scheme))
        return true;
    return false;
  }

  /**
   * Get the XElem for the scheme with the given id, or null if the scheme
   * is not implemented by this platform.
   *
   * @param id the scheme to check for
   * @return the XElem for the given scheme, or null if it is not implemented
   * by this platform
   */
  public XElem getSchemeElem(final String id)
  {
    if (manifestIncludes == null)
      return null;
    XElem xschemes = manifestIncludes.elem("schemes");
    if (xschemes == null)
      return null;
    XElem[] xlist = xschemes.elems("scheme");
    for (int i=0; i<xlist.length; ++i)
      if (id.equals(xlist[i].get("id")))
        return xlist[i];
    return null;
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
