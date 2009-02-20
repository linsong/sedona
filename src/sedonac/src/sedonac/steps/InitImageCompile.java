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
import sedona.Depend;
import sedona.kit.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.scode.*;

/**
 * InitImageCompile initializes the compiler to run the pipeline
 * which compiles a set of kits into a scode image.
 */
public class InitImageCompile
  extends CompilerStep
{

  public InitImageCompile(Compiler compiler)
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
      log.verbose("  InitImageCompile");
      initImage();
      initKits();
    }
    catch (XException e)
    {
      throw err(e);
    }        
  }

  private void initImage()
  {
    SCodeImage image = new SCodeImage();
    image.name        = xml.get("name");
    image.endian      = toEndian(xml);
    image.blockSize   = xml.geti("blockSize");
    image.refSize     = xml.geti("refSize");
    image.main        = xml.get("main", "sys::Sys.main");
    image.unhibernate = xml.get("unhibernate", "sys::Sys.unhibernate");
    image.debug       = xml.getb("debug", false);
    image.test        = xml.getb("test", false);
    compiler.image    = image;
  }

  private int toEndian(XElem xml)
  {
    String val = xml.get("endian");
    if (val.equals("big")) return SCode.vmBigEndian;
    if (val.equals("little")) return SCode.vmLittleEndian;
    throw new XException("Endian attribute must be 'big' or 'little'", xml);
  }

  protected void initKits()
  {
    XElem[] elems = xml.elems("depend");
    if (elems.length == 0)
      throw err("Must specify at least one <depend> element", new Location(xml));

    boolean testDefault = compiler.image != null ? compiler.image.test : false;
    IrKit[] kits = new IrKit[elems.length];
    for (int i=0; i<kits.length; ++i)
    {   
      XElem elem = elems[i];
      Location loc = new Location(elem);                             
      Depend depend = elem.getDepend("on");
      
      KitFile kitFile = KitDb.matchBest(depend);
      if (kitFile == null)
      {
        err("Missing kit dependency '" + depend + "'", loc);
        continue;
      }
      log.verbose("    "+kitFile.toString());   
      
      IrKit kit = kits[i] = new IrKit(loc, kitFile);
      String t = elem.get("test", null);
      if (t != null)
        kit.test = t.equals("true");   
      else
        kit.test = testDefault;
    }
    compiler.kits = kits;
    quitIfErrors();
  }

  File xmlFile;
  File xmlDir;
  XElem xml;

}
