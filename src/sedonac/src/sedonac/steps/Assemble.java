//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.steps;

import sedonac.*;
import sedonac.Compiler;
import sedonac.asm.*;

/**
 * Assemble translates the AST into IR.
 */
public class Assemble
  extends CompilerStep
{

  public Assemble(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  Assemble");
    compiler.ir = new KitAsm(compiler).assemble();
    quitIfErrors();
  }

}
