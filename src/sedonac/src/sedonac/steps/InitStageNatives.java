//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   2 Apr 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import sedona.*;
import sedona.kit.*;
import sedona.platform.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.parser.*;
import sedonac.translate.*;

/**
 * InitStageNatives uses the platform manifest to load the kits
 */
public class InitStageNatives
  extends InitImageCompile
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public InitStageNatives(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    try
    {
      log.info("  InitStageNatives");
      
      // check args
      if (compiler.outDir == null)
        throw err("Must specify target directory via -outDir", new Location("compiler args"));

      // parse Platform manifest
      Platform plat = new Platform(xml.get("id"));
      plat.load(xml);
      compiler.platform = plat;

      // initialize kits                         
      IrKit[] kits = new IrKit[plat.nativeKits.length];
      for (int i=0; i<kits.length; ++i)
      {                                   
        Depend depend = plat.nativeKits[i];
        KitFile kitFile = KitDb.matchBest(depend);
        if (kitFile == null)
        {
          err("Missing kit dependency '" + depend + "'", new Location(xml));
          continue;
        }        
        kits[i] = new IrKit(new Location(xml), kitFile);
      }
      compiler.kits = kits;
      quitIfErrors();
    }
    catch (XException e)
    {
      throw err(e);
    }
  }

}
