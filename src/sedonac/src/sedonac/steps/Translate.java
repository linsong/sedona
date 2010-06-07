//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.translate.*;

/**
 * The Translate step translate Sedona AST into Java or C code.
 */
public class Translate
  extends CompilerStep
{

  public Translate(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    Translation t = compiler.translation;
    log.info("  Translate [" + t.outDir + "]");

    try
    {
      t.outDir.mkdirs();

      for (int i=0; i<t.kits.length; ++i)
      {
        KitDef kit = t.kits[i];
        for (int j=0; j<kit.types.length; ++j)
        {
          TypeDef type = kit.types[j];
          translate(type);
        }
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      throw err("Cannot translate", new Location(t.outDir));
    }
  }

  public void translate(TypeDef type)
    throws IOException
  {
    Translation t = compiler.translation;
    if (t.target.equals("c"))
    {
      new HTranslator(compiler, type).translate();
      new CTranslator(compiler, type).translate();
    }
    else
    {
      throw err("Unknown translation target language '" + t.target + "'");
    }
  }

}
