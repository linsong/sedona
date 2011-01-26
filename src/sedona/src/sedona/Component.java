//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 07  Brian Frank  Creation
//

package sedona;

import java.util.ArrayList;

/**
 * Component is the abstract base class of
 * OfflineComponent and SoxComponent.
 */
public abstract class Component
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public Component(Type type)
  {
    if (type == null)
      throw new NullPointerException("null type");

    this.type  = type;
    this.slots = new Value[type.slots.length];
    for (int i=0; i<slots.length; ++i)
      this.slots[i] = type.slots[i].def();
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////


  /**
   * Get the id which identifies this component in its app.
   */
  public abstract int id();

  /**
   * Get the component name which is unique with its parent.
   */
  public abstract String name();

  /**
   * Get my parent Component.
   */
  public abstract Component getParent();

  /**
   * Get list of this component's children.
   */
  public abstract Component[] getChildren();

  /**
   * Lookup a child by its simple name.
   */
  public abstract Component getChild(String name);

  /**
   * Get the list of links into and out of this component.
   */
  public abstract Link[] links();

  /**
   * Get my path string as a series of names
   * separated by a forward slash.
   */
  public String path()
  {
    if (getParent() == null) return "/";

    // walk up my ancestor tree putting names into temp
    ArrayList acc = new ArrayList();
    for (Component p = this; p != null; p = p.getParent())
      acc.add(p.name());

    // now the names in temp are in reverse order, so we
    // need to order them so that my simple name is last
    StringBuffer s = new StringBuffer();
    for(int i=acc.size()-2; i>=0; --i)
      s.append('/').append(acc.get(i));

    return s.toString();
  }

  /**
   * To string representation.
   */
  public String toString()
  {
    return type.qname + "[" + id() + " " + name() + "]";
  }

  /**
   * Equals is always == reference equality.
   */
  public final boolean equals(Object obj)
  {
    return this == obj;
  }
  
  /**
   * Return built-in <code>java.lang.Object.hashCode()</code>.
   */
  public final int hashCode()
  {
    return super.hashCode();
  }

//////////////////////////////////////////////////////////////////////////
// Slot Getters
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for <code>type.slot(name)</code>.
   */
  public Slot slot(String name)
  {
    return type.slot(name);
  }

  /**
   * Convenience for <code>type.slot(name, checked)</code>.
   */
  public Slot slot(String name, boolean checked)
  {
    return type.slot(name, checked);
  }

  /**
   * Get a property.
   */
  public Value get(Slot slot)
  {
    return slots[slot.id];
  }

  ////// by slot //////

  /** Get a bool property. */
  public boolean getBool(Slot slot) { return ((Bool)get(slot)).val; }

  /** Get an integer (byte, short, or int) property. */
  public int getInt(Slot slot)
  {
    switch (slot.type.id)
    {
      case Type.byteId:  return ((Byte)get(slot)).val;
      case Type.shortId: return ((Short)get(slot)).val;
      default:           return ((Int)get(slot)).val;
    }
  }

  /** Get a long property. */
  public long getLong(Slot slot) { return ((Long)get(slot)).val; }

  /** Get a float property. */
  public float getFloat(Slot slot) { return ((Float)get(slot)).val; }

  /** Get a double property. */
  public double getDouble(Slot slot) { return ((Double)get(slot)).val; }

  /** Get a Buf property. */
  public Buf getBuf(Slot slot) { return (Buf)get(slot); }

  /** Get a Buf asStr property. */
  public String getStr(Slot slot) { return ((Str)get(slot)).val; }

  ////// by name //////

  /** Get a property by name. */
  public Value get(String name) { return get(slot(name, true)); }

  /** Get a bool property by name. */
  public boolean getBool(String name) { return getBool(slot(name, true)); }

  /** Get an integer (byte, short, or int) property by name. */
  public int getInt(String name) { return getInt(slot(name, true)); }

  /** Get a long property by name. */
  public long getLong(String name) { return getLong(slot(name, true)); }

  /** Get a float property by name. */
  public float getFloat(String name) { return getFloat(slot(name, true)); }
  
  /** Get a double property by name. */
  public double getDouble(String name) { return getDouble(slot(name, true)); }

  /** Get a Buf property by name. */
  public Buf getBuf(String name) { return getBuf(slot(name, true)); }

  /** Get a Buf asStr property by name. */
  public String getStr(String name) { return getStr(slot(name, true)); }

//////////////////////////////////////////////////////////////////////////
// Slot Setters
//////////////////////////////////////////////////////////////////////////

  /**
   * Set a property.
   */
  public void set(Slot slot, Value value)
  {
    if (!testMode) slot.assertValue(value);
    slots[slot.id] = value;
  }
  
  /**
   * Set a property by its string name.
   */
  public void set(String name, Value value) { set(slot(name, true), value); }

  ////// by slot //////

  /** Set a bool property. */
  public void setBool(Slot slot, boolean x) { set(slot, Bool.make(x)); }

  /** Set an integer (byte, short, or int) property. */
  public void setInt(Slot slot, int x)
  {
    switch (slot.type.id)
    {
      case Type.byteId:  set(slot, Byte.make(x)); break;
      case Type.shortId: set(slot, Short.make(x)); break;
      default:           set(slot, Int.make(x)); break;
    }
  }

  /** Set a long property. */
  public void setLong(Slot slot, long x) { set(slot, Long.make(x)); }

  /** Set a float property. */
  public void setFloat(Slot slot, float x) { set(slot, Float.make(x)); }

  /** Set a double property. */
  public void setDouble(Slot slot, double x) { set(slot, Double.make(x)); }

  /** Set a Buf property. */
  public void setBuf(Slot slot, Buf x) { set(slot, x); }

  /** Set a Buf asStr property. */
  public void setStr(Slot slot, String x) { set(slot, Str.make(x)); }

  ////// by name //////

  /** Set a bool property by name. */
  public void setBool(String name, boolean x) { setBool(slot(name, true), x); }

  /** Set an int (byte, short, or int) property by name. */
  public void setInt(String name, int x) { setInt(slot(name, true), x); }

  /** Set a long property by name. */
  public void setLong(String name, long x) { setLong(slot(name, true), x); }

  /** Set a float property by name. */
  public void setFloat(String name, float x) { setFloat(slot(name, true), x); }

  /** Set a double property by name. */
  public void setDouble(String name, double x) { setDouble(slot(name, true), x); }

  /** Set a Buf property by name. */
  public void setBuf(String name, Buf x) { setBuf(slot(name, true), x); }

  /** Set a Buf asStr property by name. */
  public void setStr(String name, String x) { setStr(slot(name, true), x); }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////
  
  /**
   * Check that the name is valid, if not then
   * throw an exception.
   */
  public static void assertName(String name)
  {
    String err = checkName(name);
    if (err == null) return;
    throw new IllegalArgumentException("Invalid name \"" + name + "\" (" + err + ")");
  }
  
  /**
   * Currently names are restricted to ASCII alphanumerics
   * and a total length of 7 (plus 1 char for null terminator).
   * Return error string if invalid, null if valid.
   */
  public static String checkName(String name)
  {                               
    if (name.length() > 7) return "nameTooLong";
    if (name.length() == 0) return "nameEmpty";
    for (int i=0; i<name.length(); ++i)
    {
      int c = name.charAt(i);
      if ('A' <= c && c <= 'Z') continue;
      if ('a' <= c && c <= 'z') continue;
      if (i == 0) return "invalidFirstChar";
      if ('0' <= c && c <= '9') continue;
      if (c == '_') continue;
      return "invalidChar";
    }          
    return null;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  /** The null component id is 0xffff */
  public static final int nullId = 0xffff;

  /** Sedona limits the number of children under one parent to 200 */
  public static final int maxChildren = 200;
  
  /** Set to false to disable error checking */
  public static boolean testMode = false;

  public final Type type;
  Value[] slots;

}

