//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import sedona.Env;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.translate.*;

/**
 * InitTranslate parses the XML file to get the meta-data and kit
 * list for translation into Java/C.  Then we parse all the source
 * files in each kit into a set of KitDef AST graphs.
 */
public class InitTranslate
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public InitTranslate(Compiler compiler)
  {
    super(compiler);
    this.xmlFile = compiler.input;
    this.xmlDir  = xmlFile.getParentFile();
    this.xml     = compiler.xml;
  }

  public void run()
  {
    try
    {
      log.debug("  InitTranslate");
      parseTranslation();
      parseKits();
    }
    catch (XException e)
    {
      throw err(e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  private void parseTranslation()
  {
    Translation t = new Translation();
    t.main   = xml.get("main");
    t.target = xml.get("target");
    t.outDir = new File(xmlDir, xml.get("outDir", t.target));
    compiler.translation = t;
  }

//////////////////////////////////////////////////////////////////////////
// Parse Kits
//////////////////////////////////////////////////////////////////////////

  private void parseKits()
  {
    ArrayList kits = new ArrayList();

    XElem[] elems = xml.elems("kit");
    if (elems.length == 0)
      throw err("Must specify at least one <kit> element", new Location(xml));

    for (int i=0; i<elems.length; ++i)
    {
      String name = elems[i].get("name");
      try
      {
        kits.add(parseKit(name));
      }
      catch (IOException e)
      {
        throw err("Cannot parse kit: " + name, (Location)null, e);
      }
    }

    compiler.translation.kits = (KitDef[])kits.toArray(new KitDef[kits.size()]);
  }

  private KitDef parseKit(String kitName)
    throws IOException
  {
    // find kit file
    File file = new File(Env.home, "kits" +  File.separator + kitName + ".kit");
    if (!file.exists() || file.isDirectory())
      throw err("Cannot find kit '" + kitName + "'", new Location(file));

    // init KitDef
    KitDef kit = new KitDef(new Location(file));
    kit.name = kitName;
    compiler.ast = kit;

    // open kit file as a zip file and parse source files into AST TypeDefs
    ArrayList types = new ArrayList();
    ZipFile zip = new ZipFile(file);
    try
    {
      Enumeration e = zip.entries();
      while (e.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)e.nextElement();
        String name = entry.getName();
        if (name.startsWith("source/") && name.endsWith(".sedona"))
          parse(file, zip, entry, types);
      }
    }
    finally
    {
      zip.close();
    }
    compiler.ast.types = (TypeDef[])types.toArray(new TypeDef[types.size()]);

    return kit;
  }

  private void parse(File file, ZipFile zip, ZipEntry entry, ArrayList types)
    throws IOException
  {
    Location loc = new Location(file + "|" + entry.getName());
    log.debug("    Parse [" + loc + "]");
    InputStream in = zip.getInputStream(entry);
    TypeDef[] t = new Parser(compiler, loc, in).parse();
    for (int i=0; i<t.length; ++i) types.add(t[i]);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  File xmlDir;
  File xmlFile;
  XElem xml;

}
