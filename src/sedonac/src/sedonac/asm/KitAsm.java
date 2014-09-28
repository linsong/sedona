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
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.scode.*;
import sedonac.util.*;

/**
 * KitAsm translates an AST KitDef into an IR IrKit.
 */
public class KitAsm
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public KitAsm(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Assemble
//////////////////////////////////////////////////////////////////////////

  public IrKit assemble()
  {
    init();
    asmTypes();
    return ir;
  }

  private void init()
  {
    this.ast = compiler.ast;
    this.ir  = new IrKit();

    ir.name    = ast.name;
    ir.version = ast.version;
    ir.types   = new IrType[ast.types.length];
  }

  private void asmTypes()
  {
    for (int i=0; i<ast.types.length; ++i)    
    {
      TypeDef astType = ast.types[i];
      IrType irType = new TypeAsm(this).assemble(astType);
      ir.types[i] = astType.ir = irType;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  KitDef ast;
  IrKit ir;
}
