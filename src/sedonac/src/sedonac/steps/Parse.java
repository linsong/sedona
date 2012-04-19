//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Aug 06  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.parser.*;

/**
 * Parse parses the sourceFiles into TypeDefs.
 */
public class Parse
  extends CompilerStep
{

  public Parse(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.info("  Parse [" + compiler.sourceFiles.length + " files]");

    SourceFile[] files = compiler.sourceFiles;
    ArrayList types = new ArrayList(Arrays.asList(compiler.ast.types));
    for (int i=0; i<files.length; ++i)
      parse(files[i], types);

    quitIfErrors();
    compiler.ast.types = (TypeDef[])types.toArray(new TypeDef[types.size()]);
  }

  void parse(SourceFile file, ArrayList types)
  {
    try
    {
      TypeDef[] astTypes = new Parser(compiler, file.file).parse();
      for (int i=0; i<astTypes.length; ++i)              
      {
        TypeDef t = astTypes[i]; 
        if (file.testOnly)
          t.addFacetDef("testonly", new Expr.Literal(t.loc, ns, Expr.TRUE_LITERAL, Boolean.TRUE));
        types.add(t);
      }

      //log.verbose("    Parse [" + file + "]");
      //  for (int i=0; i<astTypes.length; ++i) log.verbose("      " + astTypes[i] + (astTypes[i].isTestOnly()?" (testonly)":""));
    }
    catch(CompilerException e)
    {
      // just accumulate
      if (log.isDebug()) log.debug("  no log: " + e);
    }
    catch(Exception e)
    {
      err("Cannot parse", file.file);
      e.printStackTrace();
    }
  }

}
