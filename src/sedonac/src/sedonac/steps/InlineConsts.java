//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.asm.*;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * InlineConsts walks all the IrOp operations and replaces operations
 * which reference constants with their literal representation.  This
 * step is also used to predefine standard sys kit definitions such as
 * Sys.kitsLen.
 */
public class InlineConsts
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public InlineConsts(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  InlineConsts");

    predefines();

    for (int i=0; i<flat.methods.length; ++i)
      inline(flat.methods[i]);

    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Predefines
//////////////////////////////////////////////////////////////////////////

  private void predefines()
  {
    predefine("sys::Sys.sizeofRef", compiler.image.refSize);
    predefine("sys::Sys.kitsLen",   flat.kits.length);
    predefine("sys::Sys.logsLen",   flat.logDefines.length);
  }

  private void predefine(String qname, int value)
  {
    IrField f = (IrField)ns.resolveSlot(qname);
    f.define = new Expr.Literal(new Location("synthetic"), ns, Expr.INT_LITERAL, new Integer(value));
  }

//////////////////////////////////////////////////////////////////////////
// Inlines
//////////////////////////////////////////////////////////////////////////

  private void inline(IrMethod m)
  {
    if (m.code == null) return;
    IrOp[] ops = m.code;
    for (int i=0; i<ops.length; ++i)
      ops[i] = inline(ops[i]);
  }

  private IrOp inline(IrOp op)
  {
    switch (op.opcode)
    {
      case SCode.SizeOf:     return sizeOf(op);
      case SCode.LoadDefine: return loadDefine(op);
      default:               return op;
    }
  }

  private IrOp sizeOf(IrOp op)
  {
    int size = op.argToType().sizeof();
    return CodeAsm.loadIntOp(size);
  }

  private IrOp loadDefine(IrOp op)
  {
    IrField field = op.argToField();
    
    // in the end a Log definition becomes a
    // normal static field
    if (field.type.isLog())         
    {                                  
      op.opcode = SCode.LoadConstStatic;
      return op;
    }                          
    
    // array literals use an internal bytecode which actually
    // gets written as LoadBuf (to maintain compatibility with
    // older SVMs); we stash the literal instance on the op itself
    if (field.type.isArray())
    {                          
      op.opcode = SCode.LoadArrayLiteral;   
      op.resolvedArg = field.define();
      return op;
    }
    
    return CodeAsm.loadLiteral(field.define);
  }

}
