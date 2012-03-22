//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   8 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * MountAstIntoNamespace walks the AST types and slots:
 *  - checks for duplicate type/slot names
 *  - creates by name hashmaps
 *  - mounts AST kit into Namespace
 */
public class MountAstIntoNamespace
  extends CompilerStep
{

  public MountAstIntoNamespace(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  MountAstIntoNamespace");
    mapTypes(compiler.ast);
    ns.mount(compiler.ast);
    quitIfErrors();
  }

  private void mapTypes(KitDef kit)
  {
    HashMap map = new HashMap(kit.types.length*2);
    for (int i=0; i<kit.types.length; ++i)
    {
      TypeDef t = kit.types[i];
      if (map.containsKey(t.name))
        err("Duplicate type name '" + t.name + "'", t.loc);
      else
        map.put(t.name, t);
    }
    kit.typesByName = map;
  }

}
