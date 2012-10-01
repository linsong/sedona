//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.steps;

import sedonac.Compiler;
import sedonac.*;
import sedonac.analysis.*;
import sedonac.ast.MethodDef;
import sedonac.ast.TypeDef;

/**
 * Does additional static code checking
 * <ol>
 * <li>Unused code (methods, types)
 * <li>Dead code
 * <li>Definite Assignment
 * </ol>
 *
 * @author Matthew Giannini
 * @creation Nov 10, 2009
 *
 */
public class StaticAnalysis extends CompilerStep
{
  public StaticAnalysis(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  Static Analysis");
    new UnusedCodeAnalysis(compiler).run();
    analyzeMethods();
    quitIfErrors();
  }
  
  private void analyzeMethods()
  {
    TypeDef[] types = compiler.ast.types;
    for (int i=0; i<types.length; ++i)
    {
      MethodDef[] methods = types[i].methodDefs();
      for (int m=0; m<methods.length; ++m)
      {
        ControlFlowGraph cfg = ControlFlowGraph.make(methods[m]);
        new DeadCodeAnalysis(compiler, cfg).run();
        new DefiniteAssignmentAnalysis(compiler, cfg).run();
      }
    }
  }

}
