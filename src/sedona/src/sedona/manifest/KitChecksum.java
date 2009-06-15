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
import sedona.xml.*;

/**
 * KitChecksum is used to compute the kit schema checksum.
 */
public class KitChecksum
{

////////////////////////////////////////////////////////////////
// Compute
////////////////////////////////////////////////////////////////

  public int compute(KitManifest kit)
  {
    crc.reset();
    update(kit);
    return (int)crc.getValue();
  }

////////////////////////////////////////////////////////////////
// Manifest
////////////////////////////////////////////////////////////////

  private void update(KitManifest kit)
  {
    update(kit.name);
    update(kit.types.length);
    for (int i=0; i<kit.types.length; ++i)
      update(kit.types[i]);
  }

  private void update(TypeManifest t)
  {
    update(t.id);
    update(t.name);
    update(t.facets);
    update(t.slots.length);
    update(t.base);
    for (int i=0; i<t.slots.length; ++i)
      update(t.slots[i]);
  }

  private void update(SlotManifest s)
  {
    update(s.declaredId);
    update(s.name);
    update(s.facets);
    update(s.type);
    update(s.flags);
    update(s.def);
  }

  private void update(Facets facets)
  {
    String[] keys = facets.keys();
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      String key = keys[i];
      Value val = facets.get(key);
      update(key);
      update(val);
    }
  }

  private void update(Value v)
  {                          
    if (v != null) 
      update(v.encodeString());
  }

////////////////////////////////////////////////////////////////
// Primitives
////////////////////////////////////////////////////////////////

  private void update(String x)
  {
    if (x == null) x = "<<null>>";
    try
    {
      crc.update(x.getBytes("UTF-8"));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.toString());
    }
  }

  private void update(int x)
  {
    crc.update((x >>> 24) & 0xFF);
    crc.update((x >>> 16) & 0xFF);
    crc.update((x >>>  8) & 0xFF);
    crc.update((x >>>  0) & 0xFF);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  java.util.zip.CRC32 crc = new java.util.zip.CRC32();

}
