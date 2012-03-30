//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import sedona.Facets;
import sedonac.namespace.*;

/**
 * IrSlot
 */
public abstract class IrSlot
  implements Slot
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrSlot(IrType parent, int flags, String name, Facets facets)
  {
    this.parent  = parent;
    this.flags   = flags;
    this.name    = name;
    this.qname   = parent.qname + "." + name;
    this.rtFlags = TypeUtil.rtFlags(this, facets);
    this.facets  = facets.ro();
    this.reflect = new IrAddressable.Impl(qname + " (reflect)");
  }

//////////////////////////////////////////////////////////////////////////
// Slot
//////////////////////////////////////////////////////////////////////////

  public Type parent()  { return parent; }
  public String name()  { return name; }
  public String qname() { return qname; }
  public String toString() { return qname; }
  public Facets facets() { return facets; }

  public boolean isInherited(Type into) { return TypeUtil.isInherited(this, into); }
  public boolean isReflective() { return isAction() || isProperty(); }

  public int flags() { return flags; }
  public boolean isAbstract()  { return (flags & ABSTRACT)  != 0; }
  public boolean isAction()    { return (flags & ACTION)    != 0; }
  public boolean isConst()     { return (flags & CONST)     != 0; }
  public boolean isDefine()    { return (flags & DEFINE)    != 0; }
  public boolean isInline()    { return (flags & INLINE)    != 0; }
  public boolean isInternal()  { return (flags & INTERNAL)  != 0; }
  public boolean isNative()    { return (flags & NATIVE)    != 0; }
  public boolean isOverride()  { return (flags & OVERRIDE)  != 0; }
  public boolean isPrivate()   { return (flags & PRIVATE)   != 0; }
  public boolean isProperty()  { return (flags & PROPERTY)  != 0; }
  public boolean isProtected() { return (flags & PROTECTED) != 0; }
  public boolean isPublic()    { return (flags & PUBLIC)    != 0; }
  public boolean isStatic()    { return (flags & STATIC)    != 0; }
  public boolean isVirtual()   { return (flags & VIRTUAL)   != 0; }

//////////////////////////////////////////////////////////////////////////
// Runtime Reflection
//////////////////////////////////////////////////////////////////////////

  public int rtFlags() { return rtFlags; }
  public boolean isRtAction() { return (rtFlags & RT_ACTION) != 0; }
  public boolean isRtConfig() { return (rtFlags & RT_CONFIG) != 0; }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dump()
  {
    IrWriter out = new IrWriter(System.out);
    out.writeSlot(this);
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final IrType parent;
  public final int flags;
  public final int rtFlags;
  public final String name;
  public final String qname;
  public final Facets facets;
  public final IrAddressable reflect;
  public int id = -1;

}
