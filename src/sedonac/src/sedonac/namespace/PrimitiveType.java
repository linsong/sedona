//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedona.Facets;

/**
 * PrimitiveType models the built-in primitive types.
 */
public class PrimitiveType
  implements Type
{

//////////////////////////////////////////////////////////////////////////
// Package Constructor
//////////////////////////////////////////////////////////////////////////

  PrimitiveType(String name, int id, int sizeof)
  {
    this.name = name;
    this.id   = id;
    this.sizeof = sizeof;
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { throw new UnsupportedOperationException(); }
  public String name() { return name; }
  public String qname() { return name; }
  public Facets facets() { return Facets.empty; }

  public boolean isPrimitive() { return true; }
  public boolean isRef() { return false; }
  public boolean isNullable() { return isBool() || isFloat() || isDouble(); }
  public boolean isArray() { return false; }
  public Type arrayOf() { throw new UnsupportedOperationException(); }
  public ArrayType.Len arrayLength() { throw new UnsupportedOperationException(); }
  public String signature() { return name; }
  public Type base() { return null; }
  public boolean is(Type x) { return TypeUtil.is(this, x); }
  public boolean equals(Object o) { return TypeUtil.equals(this, o); }
  public int hashCode() { return signature().hashCode(); }
  public int sizeof() { return sizeof; }
  public String toString() { return signature(); }

  public boolean isObj()        { return false; }
  public boolean isComponent()  { return false; }
  public boolean isaComponent() { return false; }
  public boolean isVirtual()    { return false; }
  public boolean isaVirtual()   { return false; }
  public boolean isBuf()        { return false; }
  public boolean isLog()        { return false; }
  public boolean isStr()        { return false; }
  public boolean isType()       { return false; }

  public boolean isBool()    { return name == "bool"; }
  public boolean isByte()    { return name == "byte"; }
  public boolean isFloat()   { return name == "float"; }
  public boolean isInt()     { return name == "int"; }
  public boolean isLong()    { return name == "long"; }
  public boolean isDouble()  { return name == "double"; }
  public boolean isShort()   { return name == "short"; }
  public boolean isVoid()    { return name == "void"; }
  public boolean isInteger() { return isByte() || isShort() || isInt(); }
  public boolean isNumeric() { return isInteger() || isFloat() || isLong() || isDouble(); }
  public boolean isWide()    { return isLong() || isDouble(); }

  public boolean isReflective() { return true; }
  public int id() { return id; }

  public Slot[] slots() { return noSlots; }
  public Slot[] declared() { return noSlots; }
  public Slot slot(String name) { return null; }
  public void addSlot(Slot slot) { throw new UnsupportedOperationException(); }

  public int flags() { return 0; }
  public boolean isAbstract() { return false; }
  public boolean isConst()    { return false; }
  public boolean isFinal()    { return true; }
  public boolean isInternal() { return false; }
  public boolean isPublic()   { return true;  }
  public boolean isTestOnly() { return false;  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private static Slot[] noSlots = new Slot[0];

  public final String name;
  public final int id;
  public final int sizeof;
}
