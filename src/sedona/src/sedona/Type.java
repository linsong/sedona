//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 May 07  Brian Frank  Creation
//

package sedona;

import java.util.ArrayList;
import java.util.HashMap;

import sedona.manifest.TypeManifest;

/**
 * Type is used to represent a Sedona type during runtime
 * interaction with Sedona applications.
 *
 * Don't confuse this class with sedonac.namespace.Type which
 * deals with modeling types at compile time.
 */
public class Type
 implements Comparable, Constants
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  Type(Kit kit, TypeManifest manifest)
    throws Exception
  {
    this.schema   = kit.schema;
    this.kit      = kit;
    this.manifest = manifest;
    this.id       = manifest.id;
    this.name     = manifest.name;
    this.qname    = manifest.qname;
    this.facets   = manifest.facets;
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /**
   * Compare based on string qname.
   */
  public int compareTo(Object that)
  {
    return toString().compareTo(that.toString());
  }
  
  /**
   * Equality based on string qname.
   */
  public boolean equals(Object obj)
  {
    if (obj instanceof Type)
      return ((Type)obj).toString().equals(toString());
    return false;
  }
  
  public int hashCode()
  {
    return toString().hashCode();
  }

  /**
   * Return qualified name for toString.
   */
  public String toString()
  {
    return qname;
  }

  /**
   * Return if this type is assignable to the specified
   * type - t is this or one of this type's super classes.
   */
  public boolean is(Type t)
  {
    for (Type x = this; x != null; x = x.base)
      if (x == t) return true;
    return false;
  }

  /**
   * Return if this type is a Component type.
   */
  public boolean isaComponent()
  {
    return is(schema.type("sys::Component"));
  }

  /** 
   * Return if this type was declared 'abstract'
   */
  public boolean isAbstract() { return (manifest.flags & ABSTRACT) != 0; }
  
  /**
   * Return true if this type was declared 'public'
   */
  public boolean isPublic() { return (manifest.flags & PUBLIC) != 0; }

  /** 
   * Is void bool, byte, short, int, long, float, or double 
   */
  public boolean isPrimitive()
  {  
    return isPrimitive(id); 
  }
  
  /** 
   * Is void byte, short, or int
   */
  public boolean isInteger()
  {  
    return id == byteId || id == shortId || id == intId;
  }
  
  /**
   * Return if this type matches the specified value instance.
   */
  public boolean is(Value val)
  {
    if (kit.id != 0) return false;
    if (val == null)
      return id == voidId;
    else
      return val.typeId() == id;
  }

  /**
   * Get a slot by id or return null.
   */
  public Slot slot(int id)
  {
    if (0 <= id && id < slots.length)
      return slots[id];
    return null;
  }

  /**
   * Get a slot by name or return null.
   */
  public Slot slot(String name)
  {
    return (Slot)slotsByName.get(name);
  }

  /**
   * Get a slot by name.  If not found and checked is
   * true then throw exception otherwise return null.
   */
  public Slot slot(String name, boolean checked)
  {
    Slot slot = (Slot)slotsByName.get(name);
    if (slot != null) return slot;
    if (checked) throw new RuntimeException("Unknown slot " + qname + "." + name);
    return null;
  }

  /**
   * Get a list of the properties.
   */
  public Slot[] props()
  {
    ArrayList acc = new ArrayList(slots.length);
    for (int i=0; i<slots.length; ++i)
      if (slots[i].isProp()) acc.add(slots[i]);
    return (Slot[])acc.toArray(new Slot[acc.size()]);
  }

  /**
   * Get a list of the config properties.
   */
  public Slot[] configProps()
  {
    ArrayList acc = new ArrayList(slots.length);
    for (int i=0; i<slots.length; ++i)
      if (slots[i].isConfig()) acc.add(slots[i]);
    return (Slot[])acc.toArray(new Slot[acc.size()]);
  }

  /**
   * Get a list of the runtime properties.
   */
  public Slot[] runtimeProps()
  {
    ArrayList acc = new ArrayList(slots.length);
    for (int i=0; i<slots.length; ++i)
      if (slots[i].isRuntime()) acc.add(slots[i]);
    return (Slot[])acc.toArray(new Slot[acc.size()]);
  }              
  
  /**
   * Convenience for <code>facet(name, false)</code>. 
   */
  public Value facet(String name) 
  { 
    return facet(name, false); 
  }
  
  /**
   * Lookup a facet by name or return null. If inherit
   * is true then walk up the inheritance change looking
   * for the facet.
   */
  public Value facet(String name, boolean inherit)
  {                                                     
    Value val = facets.get(name, null);
    if (val != null) return val;
    if (inherit && base != null) return base.facet(name, true);
    return null;
  }  

//////////////////////////////////////////////////////////////////////////
// Resolve
//////////////////////////////////////////////////////////////////////////

  /**
   * Resolve the base type.
   */
  void resolveBase()
    throws Exception
  {
    if (manifest.base != null)
    {
      base = schema.type(manifest.base);
      if (base == null)
        throw new Exception("Missing base '" + manifest.base + "' for '" + qname + "'");
    }
  }

  /**
   * Resolve the slot tables from inherited slots and declared.
   */
  void resolveSlots()
    throws Exception
  {
    // only resolve once
    if (slots != null) return;

    // check that base has its slots resolved
    Slot[] inherited;
    if (base != null)
    {
      base.resolveSlots();
      inherited = base.slots;
    }
    else
    {
      inherited = new Slot[0];
    }

    // inherit base type slots
    ArrayList working = new ArrayList();
    slotsByName = new HashMap(inherited.length * 2);
    for (int i=0; i<inherited.length; ++i)
      addSlot(working, inherited[i]);

    // map manifest's declared slots to my slots
    for (int i=0; i<manifest.slots.length; ++i)
    {
      int id = working.size();
      addSlot(working, new Slot(this, id, manifest.slots[i]));
    }
    slots = (Slot[])working.toArray(new Slot[working.size()]);
  }

  private void addSlot(ArrayList working, Slot slot)
    throws Exception
  {
    if (slot.id < working.size())
      throw new IllegalStateException();

    Slot sameName = (Slot)slotsByName.get(slot.name);
    if (sameName != null)
    {
      if (sameName.isAction() && slot.isAction())
      {
        // Action override - It should have same id as the slot it is
        // overriding, but it should reference the SlotManifest for the
        // this Type.
        slot = new Slot(this, sameName.id, slot.manifest);
        working.set(sameName.id, slot);
      }
      else
        throw new Exception("Duplicate slot name '" + slot.name + "' in '" + qname + "'");
    }
    else
      working.add(slot);
    
    slotsByName.put(slot.name, slot);
  }

//////////////////////////////////////////////////////////////////////////
// Type Id
//////////////////////////////////////////////////////////////////////////

  /** Is void bool, byte, short, int, long, float, or double */
  public static boolean isPrimitive(int id)
  {
    return id <= doubleId;
  }

  public static int predefinedId(String qname)
  {
    if (qname.equals("void"))     return voidId;
    if (qname.equals("bool"))     return boolId;
    if (qname.equals("byte"))     return byteId;
    if (qname.equals("short"))    return shortId;
    if (qname.equals("int"))      return intId;
    if (qname.equals("long"))     return longId;
    if (qname.equals("float"))    return floatId;
    if (qname.equals("double"))   return doubleId;
    if (qname.equals("sys::Buf")) return bufId;
    if (qname.equals("str"))      return strId; // special case for facets/Buf
    throw new IllegalStateException("Unknown primitive type: " + qname);
  }

  public static String predefinedName(int id)
  {
    switch (id)
    {
      case voidId:   return "void";
      case boolId:   return "bool";
      case byteId:   return "byte";
      case shortId:  return "short";
      case intId:    return "int";
      case longId:   return "long";
      case floatId:  return "float";
      case doubleId: return "double";
      case bufId:    return "sys::Buf";
      case strId:    return "str";       // special case for facets/Buf
      default: throw new IllegalStateException("Unknown primitive type id: " + id);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  public static final int ABSTRACT  = 0x01;
  public static final int CONST     = 0x02;
  public static final int FINAL     = 0x04;
  public static final int INTERNAL  = 0x08;
  public static final int PUBLIC    = 0x10;

  public final Schema schema;
  public final Kit kit;
  public final TypeManifest manifest;
  public final int id;
  public final String name;
  public final String qname;
  public final Facets facets;
  public Type base;             // base class type (treat as readonly)
  public Slot[] slots;          // by id; includes inheritance (treat as readonly)
  HashMap slotsByName;          // by name; includes inheritance

}
