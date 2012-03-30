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
 * IrMethod
 */
public class IrMethod
  extends IrSlot
  implements Method
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrMethod(IrType parent, int flags, String name, 
                  Facets facets, Type ret, Type[] params)
  {
    super(parent, flags, name, facets);
    this.ret = ret;
    this.params = params;
    this.codeAddr = new IrAddressable.Impl(qname + " (code)", false);   
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public boolean isField() { return false; }
  public boolean isMethod() { return true; }
  public Type returnType() { return ret; }
  public Type[] paramTypes() { return params; }
  public int numParams() { return TypeUtil.numParams(this); }
  public boolean isInstanceInit() { return name.equals(INSTANCE_INIT); }
  public boolean isStaticInit() { return name.equals(STATIC_INIT); }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public Type ret;
  public Type[] params;
  public IrOp[] code;
  public int maxLocals;            
  public int maxStack;           // only available during kit compile
  public int vindex = -1;        // index in vtable if virtual
  public NativeId nativeId;
  public IrAddressable codeAddr; // block index of code

}
