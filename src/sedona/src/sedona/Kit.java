//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 May 07  Brian Frank  Creation
//

package sedona;

import java.util.*;
import sedona.manifest.*;

/**
 * Kit is used to represent a Sedona kit during runtime
 * interaction with Sedona applications.
 *
 * Don't confuse this class with sedonac.namespace.Kit which
 * deals with modeling kits at compile time.
 */
public class Kit
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Constructor.
   */
  Kit(Schema schema, int id, KitManifest manifest)
    throws Exception
  {
    this.schema   = schema;
    this.id       = id;
    this.manifest = manifest;
    this.name     = manifest.name;
    this.checksum = manifest.checksum;

    // map TypeManifests to my schema Types
    this.types = new Type[manifest.types.length];
    this.typesByName = new HashMap(types.length*3);
    for (int i=0; i<types.length; ++i)
    {
      // map TypeManifest to schema Type
      String qname = manifest.types[i].qname;
      Type t = new Type(this, manifest.types[i]);

      // sanity checking
      if (t.id != i) throw new Exception("Mismatched type id: " + qname);
      if (typesByName.get(t.name) != null) throw new Exception("Duplicate type name: " + qname);

      // add to my lookup tables
      types[i] = t;
      typesByName.put(t.name, t);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /**
   * Get a type by it's unique integer id within this kit.
   */
  public Type type(int id)
  {
    if (0 <= id & id < types.length)
      return types[id];
    else
      return null;
  }

  /**
   * Get a type by it's unique name within this kit.
   */
  public Type type(String name)
  {
    return (Type)typesByName.get(name);
  }

  /**
   * Return kit name for toString.
   */
  public String toString()
  {
    return name;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final Schema schema;         // containing schema
  public final int id;                // schema specific id
  public final KitManifest manifest;  // specific manifest for checksum
  public final String name;           // kit name
  public final int checksum;          // kit schema checksum
  public final Type[] types;          // by id (treat as readonly)
  final HashMap typesByName;          // by simple name (not qname)

}
