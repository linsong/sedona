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
 * SlotManifest represents the Slot information stored
 * in a specific check summed manifest.
 */
public class SlotManifest
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public SlotManifest(TypeManifest parent, int declaredId, String name,
                      Facets facets, String type, int flags, Value def)
  {
    this.parent     = parent;
    this.declaredId = declaredId;
    this.name       = name;
    this.qname      = parent.qname + "." + name;
    this.facets     = facets == null ? Facets.empty : facets.ro();
    this.type       = type;
    this.flags      = flags;
    this.def        = def;
  }

//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this slot into an XML document.
   */
  public void encodeXml(XWriter out)
  {
    out.w("  <slot ")
      .attr("id", ""+declaredId).w(" ")
      .attr("name", name).w(" ")
      .attr("type", type);
    String f = flagsToString(flags);
    if (f.length() > 0) out.w(" ").attr("flags", f);
    if (def != null) out.w(" ").attr("default", def.encodeString());
    if (facets.isEmpty()) out.w("/>\n");
    else
    {
      out.w(">\n");
      facets.encodeXml(out, 4);
      out.w("  </slot>\n");
    }
  }

  /**
   * Decode this slot information from an XML document.
   */
  public static SlotManifest decodeXml(TypeManifest parent, XElem xml)
  {
    int id      = xml.geti("id");
    String name = xml.get("name");
    String type = xml.get("type");
    int flags   = stringToFlags(xml.get("flags", ""));

    Value def = null;
    if ((flags & Slot.AS_STR) != 0)
    {
      def = Str.make(xml.get("default", ""));
    }
    else if ((flags & Slot.ACTION) == 0)
    {
      def = Value.defaultForType(Type.predefinedId(type));
      String defStr = xml.get("default", null);
      if (defStr != null) def = def.decodeString(defStr);
    }

    Facets facets = Facets.decodeXml(xml.elem("facets"));

    return new SlotManifest(parent, id, name, facets, type, flags, def);
  }

  /**
   * Convert bitmask flags to string format;
   * we usea single ASCII char per bit flag.
   */
  public static String flagsToString(int flags)
  {
    StringBuffer s = new StringBuffer();
    if ((flags & Slot.ACTION)   != 0) s.append('a');
    if ((flags & Slot.CONFIG)   != 0) s.append('c');
    if ((flags & Slot.AS_STR)   != 0) s.append('s');
    if ((flags & Slot.OPERATOR) != 0) s.append('o');
    return s.toString();
  }

  /**
   * Convert string flag format back to bit flags.
   */
  public static int stringToFlags(String s)
  {
    int flags = 0;
    for (int i=0; i<s.length(); ++i)
    {
      int ch = s.charAt(i);
      switch (s.charAt(i))
      {
        case 'a': flags |= Slot.ACTION;   break;
        case 'c': flags |= Slot.CONFIG;   break;
        case 's': flags |= Slot.AS_STR;   break;
        case 'o': flags |= Slot.OPERATOR; break;
        default:  System.out.println("WARNING: unknown flag: " + (char)ch);
      }
    }
    return flags;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final TypeManifest parent;
  public final int declaredId;
  public final String name;
  public final String qname;
  public final String type;   // type qname
  public final Facets facets; // facets metadata
  public final int flags;     // runtime reflective flags
  public final Value def;     // default value

}
