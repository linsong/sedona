//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 May 07  Brian Frank  Creation
//

package sedona.manifest;

import java.util.*;
import sedona.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * KitManifest represents the Kit information stored
 * in a specific check summed manifest.
 */
public class KitManifest
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Constructor.
   */
  public KitManifest(String name)
  {
    this.name = name;
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the name and checksum as a KitPart.
   */
  public KitPart part() { return new KitPart(name, checksum); }

//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this kit information into an XML document.
   */
  public void encodeXml(XWriter out)
  {
    out.w("<?xml version='1.0'?>\n");
    out.w("<kitManifest\n");
    out.w("   ").attr("name",        name).w("\n");
    out.w("   ").attr("checksum",    Integer.toHexString(checksum)).w("\n");
    out.w("   ").attr("hasNatives",  hasNatives).w("\n");
    out.w("   ").attr("doc",         doc).w("\n");
    if (version     != null) out.w("   ").attr("version",     version.toString()).w("\n");
    if (vendor      != null) out.w("   ").attr("vendor",      vendor).w("\n");
    if (description != null) out.w("   ").attr("description", description).w("\n");
    if (buildHost   != null) out.w("   ").attr("buildHost",   buildHost).w("\n");
    if (buildTime   != null) out.w("   ").attr("buildTime",   buildTime.encode()).w("\n");
    out.w(">\n");
    
    out.w("\n");
    out.w("<!-- Dependencies -->\n");
    out.w("<depends>\n");
    for (int i=0; depends != null && i<depends.length; ++i)
      depends[i].encodeXml(out);
    out.w("</depends>\n");

    out.w("\n");
    for (int i=0; i<types.length; ++i)
      if (types[i] != null) { types[i].encodeXml(out); out.w("\n"); }

    out.w("</kitManifest>\n");
  }

  /**
   * Decode this kit information from an XML document.
   */
  public void decodeXml(XElem xml)
  {
    if (!xml.name().equals("kitManifest"))
      throw new XException("Expected root to be <kitManifest>", xml);

    if (!name.equals(xml.get("name")))
      throw new XException("Mismatched name " + name + " != " + xml.get("name"), xml);

    checksum    = (int)java.lang.Long.parseLong(xml.get("checksum"), 16);
    version     = xml.getVersion("version", null);
    vendor      = xml.get("vendor", null);
    description = xml.get("description", null);
    hasNatives  = xml.getb("hasNatives", false);
    doc         = xml.getb("doc", false);
    buildTime   = xml.getAbstime("buildTime", null);
    buildHost   = xml.get("buildHost", null);

    XElem xdependsTop = xml.elem("depends");
    if (xdependsTop == null) depends = new Depend[0];
    else
    {
      XElem[] xdepends = xdependsTop.elems("depend");
      depends = new Depend[xdepends.length];
      for (int i=0; i<xdepends.length; ++i) 
        depends[i] = Depend.decodeXml(xdepends[i]);
    }

    XElem[] xtypes = xml.elems("type");
    types = new TypeManifest[xtypes.length];
    for (int i=0; i<xtypes.length; ++i)
    {
      TypeManifest tm = TypeManifest.decodeXml(this, xtypes[i]);
      if (tm.id != i) throw new XException("Misaligned type id", xtypes[i]);
      types[i] = tm;
    }
  }
  
  /**
   * Decode the XML document into a new KitManfiest instance.
   */
  public static KitManifest fromXml(XElem xml)
  {
    if (!xml.name().equals("kitManifest"))
      throw new XException("Expected root to be <kitManifest>", xml);

    String name = xml.get("name");
    if (name == null) 
      throw new XException("Missing name attribute", xml);
      
    KitManifest m = new KitManifest(name);
    m.decodeXml(xml);
    return m;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final String name;
  public int checksum;
  public Version version;
  public String vendor;
  public String description;  
  public boolean hasNatives;
  public boolean doc;
  public Abstime buildTime;
  public String buildHost;    
  public Depend[] depends;
  public TypeManifest[] types; 

}
