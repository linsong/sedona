//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import sedona.Facets;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * IrField
 */
public class IrField
  extends IrSlot
  implements Field
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrField(IrType parent, int flags, String name,
                 Facets facets, Type type)
  {
    super(parent, flags, name, facets);
    this.type = type;
    this.storage = new IrAddressable.Impl(qname + " (storage)");
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public boolean isField() { return true; }
  public boolean isMethod() { return false; }
  public Type type() { return type; }
  public Expr.Literal define() { return define; }
  public int ctorLengthParam() { return ctorLengthParam; }

//////////////////////////////////////////////////////////////////////////
// Memory
//////////////////////////////////////////////////////////////////////////

  public int offsetWidth()
  {
    if (isConst() && isStatic()) return 2;
    if (isDefine()) return 2;
    if (offset <= 0xff) return 1;
    if (offset <= 0xffff) return 2;
    return 4;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public Type type;
  public int offset = -1;          // bytes from start of static or instance mem
  public boolean arrayInit;        // {...} array initialization
  public Expr.Literal define;      // if define
  public IrAddressable storage;    // block index of storage only if const static
  public int ctorLengthParam = -1; // see sedonac.ast.FieldDef
  public Expr ctorLengthArg;       // argument to ctorLengthParam (Literal or define Field)

}
