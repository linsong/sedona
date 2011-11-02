//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   6 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import sedonac.*;
import sedonac.Compiler;
import sedonac.gen.*;

/**
 * Generate translates IR into a scode image.
 */
public class Generate
  extends CompilerStep
{

  public Generate(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  Generate");

    new ImageGen(compiler).generate();

    quitIfErrors();
  }

}
