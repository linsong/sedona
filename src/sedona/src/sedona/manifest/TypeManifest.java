//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 May 07  Brian Frank  Creation
//

package sedona.manifest;

import sedona.*;
import sedona.xml.*;

/**
 * TypeManifest represents the Slot information stored
 * in a specific check summed manifest.
 */
public class TypeManifest
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public TypeManifest(KitManifest kit, int id, String name, Facets facets, String base, int sizeof, int flags)
  {
    this.kit    = kit;
    this.id     = id;
    this.name   = name;
    this.qname  = kit.name + "::" + name;
    this.facets = facets == null ? Facets.empty : facets.ro();
    this.base   = base;
    this.sizeof = sizeof;
    this.flags  = flags;
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  public boolean isPrimitive()
  {
    return kit.name.equals("sys") && Type.isPrimitive(id);
  }

//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this type into a XML document.
   */
  public void encodeXml(XWriter out)
  {
    out.w("<!-- " + qname + " -->\n");
    out.w("<type ")
     .attr("id", ""+id).w(" ")
     .attr("name", name).w(" ")
     .attr("sizeof", sizeof);
    
    final String flagStr = flagsToString(flags);
    if (flagStr.length() > 0) out.w(" ").attr("flags", flagStr);
     
    if (base != null) out.w(" ").attr("base", base);
    out.w(">\n");

    if (!facets.isEmpty()) facets.encodeXml(out, 2);

    for (int i=0; i<slots.length; ++i)
      slots[i].encodeXml(out);
    out.w("</type>\n");
  }

  /**
   * Decode this type information from a XML document.
   */
  public static TypeManifest decodeXml(KitManifest kit, XElem xml)
  {
    int id = xml.geti("id");
    String name = xml.get("name");
    String base = xml.get("base", null);
    Facets facets = Facets.decodeXml(xml.elem("facets"));
    int size = xml.geti("sizeof", -1);
    int flags = stringToFlags(xml.get("flags", ""));

    TypeManifest t = new TypeManifest(kit, id, name, facets, base, size, flags);

    XElem[] xslots = xml.elems("slot");
    t.slots = new SlotManifest[xslots.length];
    for (int i=0; i<xslots.length; ++i)
    {
      SlotManifest s = SlotManifest.decodeXml(t, xslots[i]);
      if (s.declaredId != i) throw new XException("Misaligned declared slot ids", xslots[i]);
      t.slots[i] = s;
    }

    return t;
  }

//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Convert bitmask for type flags to ASCII string.
   * @see #flags
   */
  public static String flagsToString(final int flags)
  {
    StringBuffer sb = new StringBuffer();
    if ((flags & Type.ABSTRACT) != 0) sb.append('a');
    if ((flags & Type.INTERNAL) != 0) sb.append('i');
    return sb.toString();
  }
  
  /**
   * Convert a string representation of the flags bitmask back to an int.
   * @see #flags
   */
  public static int stringToFlags(final String s)
  {
    int flags = 0;
    boolean isPublic = true;
    for (int i=0; i<s.length(); ++i)
    {
      final char ch = s.charAt(i);
      switch (ch)
      {
        case 'a': flags |= Type.ABSTRACT; break;
        case 'i': 
          flags |= Type.INTERNAL; 
          isPublic = false;
          break;
      }
    }
    
    if (isPublic) flags |= Type.PUBLIC;

    return flags;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final KitManifest kit;  // parent kit
  public final int id;           // for this checksummed kit
  public final String name;      // simple name
  public final String qname;     // qualified name
  public final Facets facets;    // facets metadata
  public final String base;      // base type qname
  /** Only flags currently encoded are ABSTRACT, and PUBLIC/INTERNAL */
  public final int flags;        // type flags
  public final int sizeof;       // size on a 32-bit platform
  public SlotManifest[] slots;   // declared slots

}
