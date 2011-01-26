//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import sedona.Env;
import sedona.manifest.*;
import sedona.kit.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;

/**
 * ReadKits reads a list of kits into memory as IR data structures.
 */
public class ReadKits
  extends CompilerStep
{
  
  public ReadKits(Compiler compiler)
  {
    super(compiler);
    this.autoMountKitIntoNamespace = true;
  }

  public void run()
  {
    try
    {
      log.info("  ReadKits [" +  compiler.kits.length + " kits]");
      for (int i=0; i<compiler.kits.length; ++i)
        readKit(compiler.kits[i]);
    }
    catch (XException e)
    {
      throw err(e);
    }
  }
  
  public void readKit(IrKit kit)
  {
    // find kit file  
    if (kit.file == null)
      throw new IllegalStateException("Must set IrKit.file before reading");
    if (!kit.file.exists()) 
      throw err("Cannot find kit '" + kit.file + "'", kit.loc);
    File file = kit.file.file;

    try
    {
      // open kit file as a zip file
      ZipFile zip = new ZipFile(file);

      // read manifest
      ZipEntry manifest = zip.getEntry("manifest.xml");
      if (manifest == null) throw err("Missing 'manifest.xml' manifest", new Location(file));
      readManifest(new Location(file, manifest), kit, zip.getInputStream(manifest));

      // read types
      ArrayList acc = new ArrayList();
      HashMap map = new HashMap();
      Enumeration it = zip.entries();
      while (it.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)it.nextElement();
        String name = entry.getName();
        if (name.endsWith(".ir"))
        {
          IrType type = readType(new Location(file, entry), kit, zip.getInputStream(entry));
          map.put(type.name, type);
          acc.add(type);
        }
      }
      kit.types = (IrType[])acc.toArray(new IrType[acc.size()]);
      kit.typesByName = map;

      // cleanup
      zip.close();
    }
    catch (CompilerException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot read kit file", new Location(file), e);
    }

    if (autoMountKitIntoNamespace)
      ns.mount(kit);
  }

  private void readManifest(Location loc, IrKit kit, InputStream in)
    throws Exception
  {
    try
    {
      XElem xml = XParser.make(loc.file, in).parse();
      KitManifest manifest = new KitManifest(kit.name);
      manifest.decodeXml(xml);
      kit.manifest = manifest;
      kit.version = manifest.version;
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
      e.printStackTrace();
      throw err("Cannot read kit.xml manifest", loc, e);
    }
    finally
    {
      in.close();
    }
  }

  private IrType readType(Location loc, IrKit kit, InputStream in)
    throws Exception
  {
    try
    {
      return new IrReader(compiler, loc, in).readType(kit);
    }
    catch (CompilerException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot read kit.xml manifest", loc, e);
    }
    finally
    {
      in.close();
    }
  }
  
  protected boolean autoMountKitIntoNamespace = true;
}
