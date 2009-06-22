//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import sedona.Depend;
import sedona.kit.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.ast.*;

/**
 * ResolveDepends
 */
public class ResolveDepends
  extends ReadKits
{

  public ResolveDepends(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    // if no depends set compiler kits to empty and return
    DependDef[] depends = compiler.ast.depends;
    if (depends == null || depends.length == 0)
    {
      compiler.kits = new IrKit[0];
      return;
    }

    log.debug("  ResolveDepends");

    IrKit[] kits = new IrKit[depends.length];
    for (int i=0; i<depends.length; ++i)
      kits[i] = resolveDepend(depends[i]);
    compiler.kits = kits;

    quitIfErrors();  
    
    for (int i=0; i<kits.length; ++i)
      resolveRecursive(kits[i], null);
      
    quitIfErrors();  
  }

  protected IrKit resolveDepend(DependDef d)
  {             
    KitFile kitFile = KitDb.matchBest(d.depend);
    if (kitFile == null)
    {
      err("Missing kit dependency '" + d.depend + "'", d.loc);
      return null;
    }                     
    
    log.debug("    Resolve '" + d.depend + "' -> " + kitFile);

    try
    {
      IrKit kit = new IrKit(d.loc, kitFile);
      readKit(kit);
      return kit;
    }
    catch (CompilerException e)
    {
      // ignore until we've tried all the dependencies
      return null;
    }
  }

  protected void resolveRecursive(IrKit kit, String path)
  {
    Depend[] depends = kit.manifest.depends;
    for (int i=0; i<depends.length; ++i)
    {
      Depend d = depends[i];   
      IrKit x = (IrKit)ns.resolveKit(d.name());
      String thisPath = path == null ? kit.name : path + "->" + kit.name;
      if (x == null)                  
        err("Dependency on '" + d.name() + "' required through '" + thisPath + "'", new Location(compiler.input)); 
      else
        resolveRecursive(x, thisPath);
    }
  }

}
