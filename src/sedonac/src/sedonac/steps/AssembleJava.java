//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Oct 08  Brian Frank  Creation
//

package sedonac.steps;

import sedonac.*;
import sedonac.Compiler;
import sedonac.jasm.*;

/**
 * AssembleJava translates the AST into Java bytecode.
 */
public class AssembleJava
  extends CompilerStep
{

  public AssembleJava(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  AssembleJava");
    compiler.java = new JavaKitAsm(compiler).assemble();
    quitIfErrors();
  }

}
