//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import java.util.ArrayList;
import java.util.HashMap;

import sedona.Facets;
import sedonac.Location;
import sedonac.namespace.ArrayType;
import sedonac.namespace.Kit;
import sedonac.namespace.Slot;
import sedonac.namespace.Type;
import sedonac.namespace.TypeUtil;

/**
 * IrType
 */
public class IrType
  implements Type, IrAddressable
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrType(IrKit kit, int flags, String name, Facets facets)
  {
    this.kit    = kit;
    this.flags  = flags;
    this.name   = name;
    this.qname  = kit.name + "::" + name;
    this.id     = TypeUtil.predefinedId(qname);
    this.facets = facets;
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { return kit; }
  public String name() { return name; }
  public String qname() { return qname; }
  public Facets facets() { return facets; }

  public boolean isPrimitive() { return false; }
  public boolean isRef() { return true; }
  public boolean isNullable() { return true; }
  public boolean isArray() { return false; }
  public Type arrayOf() { throw new UnsupportedOperationException(); }
  public ArrayType.Len arrayLength() { throw new UnsupportedOperationException(); }
  public String signature() { return qname; }
  public int sizeof() { return sizeof; }
  public Type base() { return base; }
  public boolean is(Type x) { return TypeUtil.is(this, x); }
  public boolean equals(Object o) { return TypeUtil.equals(this, o); }
  public int hashCode() { return signature().hashCode(); }
  public String toString() { return signature(); }

  public boolean isObj()        { return qname.equals("sys::Obj"); }
  public boolean isComponent()  { return qname.equals("sys::Component"); }
  public boolean isaComponent() { return TypeUtil.isaComponent(this); }
  public boolean isVirtual()    { return qname.equals("sys::Virtual"); }
  public boolean isaVirtual()   { return TypeUtil.isaVirtual(this); }
  public boolean isBuf()        { return qname.equals("sys::Buf"); }
  public boolean isLog()        { return qname.equals("sys::Log"); }
  public boolean isStr()        { return qname.equals("sys::Str"); }
  public boolean isType()       { return qname.equals("sys::Type"); }

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
  public int id() { if (id < 0) throw new IllegalStateException(qname); return id; }

  public Slot[] slots() { return (Slot[])slots.toArray(new Slot[slots.size()]); }
  public Slot[] declared() { return declared; }
  public Slot slot(String name) { return (Slot)slotsByName.get(name); }

  public void addSlot(Slot slot)
  {
    String name = slot.name();
    if (slotsByName.containsKey(name))
      throw new IllegalStateException(slot.qname());

    slots.add(slot);
    slotsByName.put(name, slot);
  }

  public int flags() { return flags; }
  public boolean isAbstract() { return (flags & ABSTRACT) != 0; }
  public boolean isFinal()    { return (flags & FINAL) != 0; }
  public boolean isConst()    { return (flags & CONST) != 0; }
  public boolean isInternal() { return (flags & INTERNAL) != 0; }
  public boolean isPublic()   { return (flags & PUBLIC)   != 0; }

//////////////////////////////////////////////////////////////////////////
// Slots
//////////////////////////////////////////////////////////////////////////

  public IrField[] instanceFields()
  {
    ArrayList acc = new ArrayList();
    for (int i=0; i<declared.length; ++i)
    {
      IrSlot slot = declared[i];
      if (slot.isField() && !slot.isStatic())
        acc.add(slot);
    }
    return (IrField[])acc.toArray(new IrField[acc.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// IrAddressable
//////////////////////////////////////////////////////////////////////////

  public int getBlockIndex() { return blockIndex; }
  public void setBlockIndex(int i) { blockIndex = i; }
  
  public boolean alignBlockIndex() { return true; }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dump()
  {
    IrWriter out = new IrWriter(System.out);
    out.writeType(this);
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final IrKit kit;
  public final int flags;
  public final String name;
  public final String qname;
  public final Facets facets;
  public int id;
  public int blockIndex;
  public Location loc;
  public Type base;
  public IrSlot[] declared;
  public IrSlot[] reflectiveSlots;
  public ArrayList slots;
  public HashMap slotsByName;
  public int sizeof = -1;
  public IrVTable vtable;

}
