//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   14 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedona.Facets;

/**
 * StubType is models key bootstrap types so that we can compile sys.
 */
public class StubType
  implements Type
{

//////////////////////////////////////////////////////////////////////////
// Package Constructor
//////////////////////////////////////////////////////////////////////////

  StubType(Namespace ns, Type base, String name)
  {
    this.ns = ns;
    this.base = base;
    this.name = name;
    this.qname = "sys::" + name;
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { return ns.resolveKit("sys"); }
  public String name() { return name; }
  public String qname() { return qname; }
  public Facets facets() { return Facets.empty; }

  public boolean isPrimitive() { return false; }
  public boolean isRef() { return true; }
  public boolean isNullable() { return true; }
  public boolean isArray() { return false; }
  public Type arrayOf() { throw new UnsupportedOperationException(); }
  public ArrayType.Len arrayLength() { throw new UnsupportedOperationException(); }
  public String signature() { return qname; }
  public Type base() { return base; }
  public boolean is(Type x) { return TypeUtil.is(this, x); }
  public boolean equals(Object o) { return TypeUtil.equals(this, o); }
  public int hashCode() { return signature().hashCode(); }
  public int sizeof() { return 0; }
  public String toString() { return signature(); }

  public boolean isObj()     { return qname.equals("sys::Obj"); }
  public boolean isComponent()  { return false; }
  public boolean isaComponent() { return false; }
  public boolean isVirtual()    { return false; }
  public boolean isaVirtual()   { return false; }
  public boolean isBuf()     { return qname.equals("sys::Buf"); }
  public boolean isLog()     { return qname.equals("sys::Log"); }
  public boolean isStr()     { return qname.equals("sys::Str"); }
  public boolean isType()    { return qname.equals("sys::Type"); }

  public boolean isBool()    { return false; }
  public boolean isByte()    { return false; }
  public boolean isFloat()   { return false; }
  public boolean isInt()     { return false; }
  public boolean isLong()    { return false; }
  public boolean isDouble()  { return false; }
  public boolean isShort()   { return false; }
  public boolean isVoid()    { return false; }
  public boolean isInteger() { return false; }
  public boolean isNumeric() { return false; }
  public boolean isWide()    { return false; }

  public boolean isReflective() { return TypeUtil.isReflective(this); }
  public int id() { throw new UnsupportedOperationException(); }

  public Slot[] slots() { return noSlots; }
  public Slot[] declared() { return noSlots; }
  public Slot slot(String name) { return null; }
  public void addSlot(Slot slot) { throw new UnsupportedOperationException(); }

  public int flags() { return 0; }
  public boolean isAbstract() { return false; }
  public boolean isConst()    { return false; }
  public boolean isFinal()    { return false; }
  public boolean isInternal() { return false; }
  public boolean isPublic()   { return true;  }
  public boolean isTestOnly() { return false; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private static Slot[] noSlots = new Slot[0];

  public final Namespace ns;
  public final Type base;
  public final String name;
  public final String qname;

}
