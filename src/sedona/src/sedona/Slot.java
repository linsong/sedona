//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 May 07  Brian Frank  Creation
//

package sedona;

import sedona.manifest.*;

/**
 * Slot is used to represent a Sedona slot during runtime
 * interaction with Sedona applications.
 *
 * Don't confuse this class with sedonac.namespace.Slot which
 * deals with modeling slots at compile time.
 */
public class Slot
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  Slot(Type parent, int id, SlotManifest manifest)
    throws Exception
  {
    Type t = parent.schema.type(manifest.type);
    if (t == null)
      throw new Exception("Unresolved type '" + manifest.type + "' for slot '" + manifest.qname + "'");

    this.parent   = parent;
    this.id       = id;
    this.manifest = manifest;
    this.name     = manifest.name;
    this.qname    = manifest.qname;
    this.facets   = manifest.facets;
    this.flags    = manifest.flags;
    this.def      = manifest.def;
    this.type     = t;
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /**
   * Return qualified name for toString.
   */
  public String toString()
  {
    return qname;
  }

  /**
   * Get default value.
   */
  public Value def()
  {
    return def;
  }

////////////////////////////////////////////////////////////////
// Checking
////////////////////////////////////////////////////////////////

  /**
   * Check that the value is valid for this slot, 
   * if not then throw an exception.
   */
  public void assertValue(Value val)
  {
    String err = checkValue(val);
    if (err == null) return;
    throw new IllegalArgumentException("Invalid value for slot '" + qname + "' (" + err + ")");
  }
  
  /**
   * Check that the value is valid for the this slot.
   * If valid return null, otherwise return error code.
   */
  public String checkValue(Value val)
  {                                    
    if (val == null)
    {
      if (type.id == Type.voidId) return null;
      return "wrongType";
    }
    
    boolean typeOk = isAsStr() ? val instanceof Str : type.is(val);
    if (!typeOk) return "wrongType";
    
    int max;
            
    switch (val.typeId())
    {
      case Type.strId:
        Str str = (Str)val;
        max = facets.geti("max", -1);
        if (max > 0 && str.val.length() >= max) return "strTooLong";
        if (!str.isAscii()) return "strNotAscii";
        break;
        
      case Type.bufId:
        Buf buf = (Buf)val;
        max = facets.geti("max", -1);       
        if (max > 0 && buf.size > max) return "bufTooLong";
        break;
    }
    
    return null;
  }

//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////

  public boolean isProp() { return (flags & ACTION) == 0; }

  public boolean isAction()   { return (flags & ACTION) != 0; }
  public boolean isConfig()   { return isProp() && (flags & CONFIG) != 0; }
  public boolean isRuntime()  { return isProp() && (flags & CONFIG) == 0; }
  public boolean isAsStr()    { return (flags & AS_STR) != 0; }
  public boolean isOperator() { return (flags & OPERATOR) != 0; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  // Must be kept in sync with Slot.sedona
  public static final int ACTION   = 0x01;
  public static final int CONFIG   = 0x02;
  public static final int AS_STR   = 0x04;
  public static final int OPERATOR = 0x08;

  public final Type parent;
  public final int id;
  public final SlotManifest manifest;
  public final String name;
  public final String qname;
  public final Facets facets;
  public final int flags;
  public final Type type;
  private final Value def;

}
