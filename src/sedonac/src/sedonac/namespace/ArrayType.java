//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedona.Facets;
import sedonac.Location;

/**
 * ArrayType models an array of another Type.
 */
public class ArrayType
  implements Type
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public ArrayType(Location loc, Type of, Len len, boolean isConst)
  {
    this.loc = loc;
    this.of = of;
    this.len = len;
    this.isConst = isConst;
  }

  public ArrayType(Location loc, Type of, Len len)
  {
    this(loc, of, len, false);
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { throw new UnsupportedOperationException(); }
  public String name() { throw new UnsupportedOperationException(); }
  public String qname() { return of.qname() + "[]"; }
  public Facets facets() { return Facets.empty; }

  public boolean isPrimitive() { return false; }
  public boolean isRef() { return true; }
  public boolean isNullable() { return true; }
  public boolean isArray() { return true; }
  public Type arrayOf() { return of; }
  public ArrayType.Len arrayLength() { return len; }
  public Type base() { return null; }
  public boolean is(Type x) { return TypeUtil.is(this, x); }
  public boolean equals(Object o) { return TypeUtil.equals(this, o); }
  public int hashCode() { return signature().hashCode(); }
  public String toString() { return signature(); }

  public String signature()
  {
    String prefix = isConst ? "const " : "";
    if (len == null)
      return prefix + of + "[]";
    else
      return prefix + of + "[" + len + "]";
  }

  public int sizeof()
  {
    if (len == null) throw new IllegalStateException("sizeof unbounded array: " + this);
    if (of.isRef()) throw new IllegalStateException("array of refs is variable size: " + this);
    return of.sizeof() * len.val();
  }

  public boolean isObj()        { return false; }
  public boolean isComponent()  { return false; }
  public boolean isaComponent() { return false; }
  public boolean isVirtual()    { return false; }
  public boolean isaVirtual()   { return false; }
  public boolean isBuf()        { return false; }
  public boolean isLog()        { return false; }
  public boolean isStr()        { return false; }
  public boolean isType()       { return false; }

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

  public boolean isReflective() { return false; }
  public int id() { throw new UnsupportedOperationException(); }

  public Slot[] slots() { return noSlots; }
  public Slot[] declared() { return noSlots; }
  public Slot slot(String name) { return null; }
  public void addSlot(Slot slot) { throw new UnsupportedOperationException(); }

  public int flags() { return 0; }
  public boolean isAbstract() { return false; }
  public boolean isConst()    { return isConst; }
  public boolean isFinal()    { return true; }
  public boolean isInternal() { return of.isInternal(); }
  public boolean isPublic()   { return of.isPublic();  }
  public boolean isTestOnly() { return false;  }

//////////////////////////////////////////////////////////////////////////
// Len
//////////////////////////////////////////////////////////////////////////

  public static abstract class Len
  {
    public final boolean equals(Object that)
    {
      if (this == that) return true;
      if (that == null) return false;
      if (getClass() == that.getClass())
        return eq((Len)that);
      else
        return false;
    }
    public final int hashCode() { return toString().hashCode(); }
    
    public abstract String toString();
    public abstract int val();
    protected abstract boolean eq(Len len);
  }

  public static class LiteralLen extends Len
  {
    public LiteralLen(int val) { this.val = val; }
    public String toString() { return ""+val; }
    public int val() { return val; }
    protected boolean eq(Len that) { return val == ((LiteralLen)that).val; }
    public int val;
  }

  public static class UnresolvedLen extends Len
  {
    public UnresolvedLen(String id) { this.id = id; }
    public String toString() { return id; }
    public int val() { throw new UnsupportedOperationException(); }
    protected boolean eq(Len that) { throw new UnsupportedOperationException(); }
    public String id;
  }

  public static class DefineLen extends Len
  {
    public DefineLen(Field field) { this.field = field; }
    public String toString() { return field.qname(); }
    public int val() { return field.define().asInt(); }
    protected boolean eq(Len that) { return field.qname().equals(((DefineLen)that).field.qname()); }
    public Field field;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private static Slot[] noSlots = new Slot[0];

  public Type of;
  public Len len;
  public boolean isConst;
  public Location loc;

}
