//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 May 07  Brian Frank  Creation
//

package sedona;

import sedona.manifest.KitManifest;
import sedona.manifest.ManifestDb;
import sedona.util.TextUtil;
import sedona.util.Version;


/**
 * KitPart is used to identify a specific version of a Kit by
 * the kit's globally unique name and a 32-bit checksum.
 */
public class KitPart
{

//////////////////////////////////////////////////////////////////////////
// For Local Kit
//////////////////////////////////////////////////////////////////////////

  /**
   * Find the local kit zip file for the specified name,
   * and return the checksum for that part.  If a local
   * kit is not found by specified name then return null.
   */
  public static KitPart forLocalKit(String name)
    throws Exception
  {
    KitManifest km = ManifestDb.loadForLocalKit(name);
    if (km == null) return null;
    return km.part();
  }

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Parse from "{name}-{checksum}" format.
   */
  public static KitPart parse(String s)
  {                   
    try
    {
      int dash = s.indexOf('-');
      String n = s.substring(0, dash);
      int c = (int)java.lang.Long.parseLong(s.substring(dash+1), 16);
      return new KitPart(n, c);
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException("Invalid KitPart format: " + s);
    }
  }

  /**
   * Construct with specified kit name and explicit checksum.
   */
  public KitPart(String name, int checksum)
  {               
    this(name, checksum, null);
  }

  /**
   * Construct with optional version.
   */
  public KitPart(String name, int checksum, Version version)
  {                          
    this.name     = name;
    this.checksum = checksum;
    this.key      = name + "-" + TextUtil.intToHexString(checksum);
    this.version  = version;
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KitPart kitPart = (KitPart)o;

    if (key != null ? !key.equals(kitPart.key) : kitPart.key != null)
      return false;

    return true;
  }

  public int hashCode()
  {
    return key != null ? key.hashCode() : 0;
  }

  /**
   * Return key which is "{name}-{checksum}".
   */
  public String toString()
  {
    return key;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final String name;
  public final int checksum;
  public final String key; 
  public final Version version;   // optional version if known

}
