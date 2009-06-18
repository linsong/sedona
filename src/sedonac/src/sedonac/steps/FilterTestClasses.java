//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.namespace.*;

/**
 * FilterTestClasses
 */
public class FilterTestClasses
  extends CompilerStep
{

  public FilterTestClasses(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  FilterTestClasses");
    for (int i=0; i<compiler.kits.length; ++i)
      filter(compiler.kits[i]);
  }

  private void filter(IrKit kit)
  {
    // skip if test turned on for this kit
    if (kit.test) return;

    ArrayList types = new ArrayList();
    HashMap typesByName = new HashMap();

    for (int i=0; i<kit.types.length; ++i)
    {
      IrType t = kit.types[i];

      // skip it
      if (TypeUtil.isTestOnly(t)) continue;

      // keep it
      types.add(t);
      typesByName.put(t.name, t);
    }

    kit.types = (IrType[])types.toArray(new IrType[types.size()]);
    kit.typesByName = typesByName;
  }

}
