//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import java.util.ArrayList;
import java.util.HashMap;

import sedonac.Location;
import sedonac.ir.IrType;
import sedonac.namespace.ArrayType;
import sedonac.namespace.Kit;
import sedonac.namespace.Method;
import sedonac.namespace.Namespace;
import sedonac.namespace.Slot;
import sedonac.namespace.Type;
import sedonac.namespace.TypeUtil;

/**
 * TypeDef
 */
public class TypeDef
  extends FacetsNode
  implements Type
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public TypeDef(Location loc, KitDef kit, int flags, String name, FacetDef[] facets)
  {
    super(loc, facets);
    this.kit    = kit;
    this.flags  = flags;
    this.name   = name;
    this.qname  = kit.name + "::" + name;
    this.id     = TypeUtil.predefinedId(qname);
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { return kit; }
  public String name() { return name; }
  public String qname() { return qname; }
  
  public boolean isPrimitive() { return false; }
  public boolean isRef() { return true; }
  public boolean isNullable() { return true; }
  public boolean isArray() { return false; }
  public Type arrayOf() { throw new UnsupportedOperationException(); }
  public ArrayType.Len arrayLength() { throw new UnsupportedOperationException(); }
  public String signature() { return qname; }
  public int sizeof() { return TypeUtil.ir(this).sizeof(); }
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
  public Slot[] declared() { return slotDefs(); }
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
  public boolean isConst()    { return (flags & CONST) != 0; }
  public boolean isFinal()    { return (flags & FINAL) != 0; }
  public boolean isInternal() { return (flags & INTERNAL) != 0; }
  public boolean isPublic()   { return (flags & PUBLIC)   != 0; }

//////////////////////////////////////////////////////////////////////////
// SlotDefs
//////////////////////////////////////////////////////////////////////////

  public SlotDef[] slotDefs()
  {
    // these are my definitions only (not inherited slots)
    ArrayList acc = new ArrayList();
    for (int i=0; i<slots.size(); ++i)
    {
      Slot slot = (Slot)slots.get(i);
      if (slot.parent() == this)
        acc.add(slots.get(i));
    }
    return (SlotDef[])acc.toArray(new SlotDef[acc.size()]);
  }

  public MethodDef[] methodDefs()
  {
    // these are my definitions only (not inherited slots)
    ArrayList acc = new ArrayList();
    for (int i=0; i<slots.size(); ++i)
      if (slots.get(i) instanceof MethodDef)
        acc.add(slots.get(i));
    return (MethodDef[])acc.toArray(new MethodDef[acc.size()]);
  }

  public FieldDef[] fieldDefs()
  {
    // these are my definitions only (not inherited slots)
    ArrayList acc = new ArrayList();
    for (int i=0; i<slots.size(); ++i)
      if (slots.get(i) instanceof FieldDef)
        acc.add(slots.get(i));
    return (FieldDef[])acc.toArray(new FieldDef[acc.size()]);
  }

  public MethodDef makeInstanceInit(Namespace ns)
  {
    String name = Method.INSTANCE_INIT;
    MethodDef m = (MethodDef)slotsByName.get(name);
    if (m == null)
    {
      int flags = 0;
      m = new MethodDef(loc, this, flags, name, FacetDef.empty, ns.voidType, new ParamDef[0], new Block(loc));
      m.synthetic = true;
      addSlot(m);
    }
    return m;
  }

  public MethodDef makeStaticInit(Namespace ns)
  {
    String name = Method.STATIC_INIT;
    MethodDef m = (MethodDef)slotsByName.get(name);
    if (m == null)
    {
      int flags = Slot.STATIC;
      m = new MethodDef(loc, this, flags, name, FacetDef.empty, ns.voidType, new ParamDef[0], new Block(loc));
      m.synthetic = true;
      addSlot(m);
    }
    return m;
  }

//////////////////////////////////////////////////////////////////////////
// AST Node
//////////////////////////////////////////////////////////////////////////

  public void walk(AstVisitor visitor, int depth)
  {
    visitor.enterType(this);    

    // types
    if (base != null) base = visitor.type(base);

    // facets
    walkFacets(visitor, depth);

    // slots
    if (depth >= AstVisitor.WALK_TO_SLOTS)
    {
      for (int i=0; i<slots.size(); ++i)
      {
        Slot slot = (Slot)slots.get(i);
        if (slot.parent() == this)
          ((SlotDef)slots.get(i)).walk(visitor, depth);
      }
    }

    visitor.exitType(this);
  }

  public void write(AstWriter out)
  {
    out.w("class ").w(qname).nl();
    out.w("{").nl();
    for (int i=0; i<slots.size(); ++i)
      if (slots.get(i) instanceof SlotDef)
      {
        if (i > 0) out.nl();
        out.indent++;
        ((SlotDef)slots.get(i)).write(out);
        out.indent--;
      }
    out.w("}").nl();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public KitDef kit;
  public int flags;
  public String name;
  public String qname;
  public Type base;
  public String doc;
  public FacetDef[] facets;
  public int id = -1;                 // if reflective
  public SlotDef[] reflectiveSlots;   // declared only
  public IrType ir;                   // once assembled
  private ArrayList slots = new ArrayList();
  private HashMap slotsByName = new HashMap();


}
