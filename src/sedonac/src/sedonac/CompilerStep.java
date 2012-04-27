//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 06  Brian Frank  Creation
//

package sedonac;

import sedonac.ast.*;

/**
 * CompilerSupport implements one step in the compiler pipeline.
 */
public abstract class CompilerStep
  extends CompilerSupport
{

////////////////////////////////////////////////////////////////
// Cosntructor
////////////////////////////////////////////////////////////////

  public CompilerStep(Compiler compiler)
  {
    super(compiler);
  }

////////////////////////////////////////////////////////////////
// Overrides
////////////////////////////////////////////////////////////////

  public abstract void run();

////////////////////////////////////////////////////////////////
// AstVisitor
////////////////////////////////////////////////////////////////

  public void walkAst(int depth)
  {
    TypeDef[] types = compiler.ast.types;
    for (int i=0; i<types.length; ++i)
      types[i].walk(this, depth);
  }

  public void enterType(TypeDef t)
  {
    super.enterType(t);
    curType = t;
  }

  public void exitType(TypeDef t)
  {
    super.exitType(t);
    curType = null;
  }

  public void enterField(FieldDef f)
  {
    super.enterField(f);
    inStatic = f.isStatic();
  }

  public void exitField(FieldDef f)
  {
    super.exitField(f);
  }

  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    curMethod = m;
    inStatic = m.isStatic();
  }

  public void exitMethod(MethodDef m)
  {
    super.exitMethod(m);
    curMethod = null;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public TypeDef curType;
  public MethodDef curMethod;
  public boolean inStatic;

}
