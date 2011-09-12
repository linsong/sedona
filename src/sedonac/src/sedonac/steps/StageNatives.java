//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   2 Apr 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import sedona.Env;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;

/**
 * StageNatives
 */
public class StageNatives
  extends InitImageCompile
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public StageNatives(Compiler compiler)
  {
    super(compiler);
    this.xmlFile  = compiler.input;
    this.xmlDir   = xmlFile.getParentFile();
    this.xml      = compiler.xml;
    this.stageDir = compiler.outDir;
  }

  public void run()
  {
    try
    {
      log.info("  StageVM [" + stageDir + "]");
      copySourceFiles();
    }
    catch (XException e)
    {
      throw err(e);
    }
    catch (CompilerException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw err("Cannot stage VM", new Location(stageDir), e);
    }
  }


//////////////////////////////////////////////////////////////////////////
// Copy Source Files
//////////////////////////////////////////////////////////////////////////

  public void copySourceFiles()
  { 
    if (compiler.platform.nativeFiles==null)
      throw err("Must have at least one <nativeSource> element", new Location(xml));

    String[] files = compiler.platform.nativeFiles;
    for (int i=0; i<files.length; ++i)
      copySourceFile(files[i]);
  }

  public void copySourceFile(String nativeFile)
  {                                                                    
    Location loc = new Location(xml);

    if (nativeFile==null) return;

    File file = new File(nativeFile);
    if (!file.exists())
    {
      warn("Source file not found '" + file + "'");
      return;
    }

    if (file.isDirectory()) return;    // skip directories

    try
    {
      FileUtil.copyFile(file, new File(stageDir, file.getName()));
    }
    catch (IOException e)
    {
      throw err("Cannot copy file", new Location(file), e);
    }

  }


  /*
//////////////////////////////////////////////////////////////////////////
// Copy Source Dirs
//////////////////////////////////////////////////////////////////////////

  public void copySourceDirs()
  { 
    String[] paths = compiler.platform.nativePaths;
    for (int i=0; i<paths.length; ++i)
      copySourceDir(paths[i]);
  }

  public void copySourceDir(String path)
  {                                                                    
    Location loc = new Location(xml);
    
    if (path==null) return;

    if (!path.startsWith("/"))
      throw err("Paths must start with / and be relative to sedona home: " + path, loc);
    File dir = new File(Env.home, path.substring(1));
    if (!dir.exists() || !dir.isDirectory())
    {
      warn("Source path not found '" + dir + "'");
      return;
    }

    File[] files = dir.listFiles();
    log.debug("    Copy '" + dir + "' [" + files.length + " files]");
    for (int i=0; i<files.length; ++i)
    {
      File f = files[i];
      if (f.isDirectory()) continue;
      try
      {
        FileUtil.copyFile(f, new File(stageDir, f.getName()));
      }
      catch (IOException e)
      {
        throw err("Cannot copy file", new Location(f), e);
      }
    }
  }

  */

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  File stageDir;
}
