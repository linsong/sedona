//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.asm;

import java.util.*;
import sedonac.*;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.scode.*;
import sedonac.namespace.*;
import sedonac.util.*;

/**
 * TypeAsm translates an AST TypeDef into an IR IrType.
 */
public class TypeAsm
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public TypeAsm(KitAsm parent)
  {
    super(parent.compiler);
    this.parent = parent;
  }

//////////////////////////////////////////////////////////////////////////
// Assemble
//////////////////////////////////////////////////////////////////////////

  public IrType assemble(TypeDef ast)
  {
    init(ast);
    asmSlots();
    return ir;
  }

  private void init(TypeDef ast)
  {
    this.ast = ast;
    this.ir  = new IrType(parent.ir, ast.flags, ast.name, ast.facets());
    ir.loc   = ast.loc;           
    ir.base  = TypeUtil.ir(ast.base);
  }

  private void asmSlots()
  {
    // don't waste time mapping slots or slotsByName (unless
    // at some point we need a full mapping)
    SlotDef[] slots = ast.slotDefs();
    ir.declared = new IrSlot[slots.length];
    for (int i=0; i<slots.length; ++i)
      ir.declared[i] = asmSlot(slots[i]);
  }

  private IrSlot asmSlot(SlotDef ast)
  {
    if (ast instanceof MethodDef)
      return asmMethod((MethodDef)ast);
    else
      return asmField((FieldDef)ast);
  }

  private IrField asmField(FieldDef ast)
  {
    IrField f = new IrField(ir, ast.flags, ast.name, ast.facets(), ast.type);
    f.arrayInit = ast.init != null && ast.init.id == Expr.INIT_ARRAY;
    f.ctorLengthParam = ast.ctorLengthParam;
    if (ast.isDefine())
      f.define = (Expr.Literal)ast.init;

    // if this field calls a constructor which sizes a an
    // array inside the field's type, then store that size
    if (ast.ctorArgs != null && ast.ctorArgs.length > 0)
    {
      Field unsizedField = TypeUtil.getUnsizedArrayField(f.type);
      if (unsizedField != null)
        f.ctorLengthArg = ast.ctorArgs[unsizedField.ctorLengthParam()-1];
    }

    return f;
  }

  private IrMethod asmMethod(MethodDef ast)
  {
    IrMethod m = new IrMethod(ir, ast.flags, ast.name, ast.facets(), ast.ret, ast.paramTypes());
    m.maxLocals = ast.maxLocals;    
    m.nativeId  = ast.nativeId;
    if (ast.code != null)
    {
      m.code = new CodeAsm(this).assemble(ast.code);
      m.maxStack = ast.code.maxStack();
    }
    return m;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  KitAsm parent;
  TypeDef ast;
  IrType ir;
}
