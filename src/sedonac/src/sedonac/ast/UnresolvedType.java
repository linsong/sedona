//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.ast;

import sedona.Facets;
import sedonac.*;
import sedonac.namespace.*;

/**
 * UnresolvedType
 */
public class UnresolvedType
  extends AstNode
  implements Type
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public UnresolvedType(Location loc, String name)
  {
    super(loc);
    this.name = name;
    if (name.indexOf(':') > 0) this.qname = name;
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public Kit kit() { throw unresolved(); }
  public String name() { throw unresolved(); }
  public String qname() { if (qname != null) return qname; throw unresolved(); }
  public Facets facets() { throw unresolved(); }

  public boolean isPrimitive()  { return false; }
  public boolean isRef()        { return true; }
  public boolean isNullable()   { return true; }
  public boolean isArray()      { return false; }
  public Type arrayOf()         { throw new UnsupportedOperationException(); }
  public ArrayType.Len arrayLength() { throw new UnsupportedOperationException(); }
  public int sizeof()           { throw unresolved(); }
  public Type base()            { throw unresolved(); }
  public boolean is(Type x)     { throw unresolved(); }
  public String signature()     { throw unresolved(); }

  public boolean isObj()        { throw unresolved(); }
  public boolean isComponent()  { throw unresolved(); }
  public boolean isaComponent() { throw unresolved(); }
  public boolean isVirtual()    { throw unresolved(); }
  public boolean isaVirtual()   { throw unresolved(); }
  public boolean isBuf()        { throw unresolved(); }
  public boolean isLog()        { throw unresolved(); }
  public boolean isStr()        { throw unresolved(); }
  public boolean isType()       { throw unresolved(); }

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

  public boolean isReflective() { throw unresolved(); }
  public int id() { throw unresolved(); }

  public Slot[] slots()          { throw unresolved(); }
  public Slot[] declared()       { throw unresolved(); }
  public Slot slot(String name)  { throw unresolved(); }
  public void addSlot(Slot slot) { throw new UnsupportedOperationException(); }

  public int flags() { throw unresolved(); }
  public boolean isAbstract() { throw unresolved(); }
  public boolean isConst()    { throw unresolved(); }
  public boolean isFinal()    { throw unresolved(); }
  public boolean isInternal() { throw unresolved(); }
  public boolean isPublic()   { throw unresolved(); }
  public boolean isTestOnly() { throw unresolved(); }

  public IllegalStateException unresolved()
  {
    return new IllegalStateException("Type is unresolved '" + this + "': " + loc);
  }

//////////////////////////////////////////////////////////////////////////
// AST Node
//////////////////////////////////////////////////////////////////////////

  public String toString()
  {
    return name;
  }

  public void write(AstWriter out)
  {
    out.print(this);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public String name;
  public String qname;

}
