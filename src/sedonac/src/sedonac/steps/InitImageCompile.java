//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.File;

import sedona.Depend;
import sedona.kit.KitDb;
import sedona.kit.KitFile;
import sedona.util.Version;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedonac.Compiler;
import sedonac.CompilerStep;
import sedonac.Location;
import sedonac.ir.IrKit;
import sedonac.scode.SCode;
import sedonac.scode.SCodeImage;


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
      log.debug("  InitImageCompile");
      initImage();
      initKits();
      initResume();
    }
    catch (XException e)
    {
      throw err(e);
    }        
  }

  private void initImage()
  {
    SCodeImage image = new SCodeImage();
    
    // the 'name' attribute has never been used. Scode image name is based
    // on the scode xml input file name.  The attribute is now deprecated -
    // display a warning if present, but still ignore it (as we always have).
    if (xml.get("name", null) != null)
      compiler.warn("'name' attribute is deprecated and will be ignored.", new Location(xml));

    image.endian      = toEndian(xml);
    image.blockSize   = xml.geti("blockSize");
    image.refSize     = xml.geti("refSize");
    image.main        = xml.get("main", "sys::Sys.main");
    image.debug       = xml.getb("debug", false);
    image.test        = xml.getb("test", false);
    image.armDouble   = xml.getb("armDouble", false);
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

    boolean scodeTest = compiler.image != null ? compiler.image.test : false;

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
      log.debug("    "+kitFile.toString());   
      
      IrKit kit = kits[i] = new IrKit(loc, kitFile);
      if (kit.name.equals("sys")) sysKit = kitFile;
      
      // If scodeTest==false, then kit.test=false
      if (!scodeTest)
        kit.test = false;
      else
      {
        // If test attrib exists, use it; o/w use scodeTest
        String t = elem.get("test", null);
        if (t != null)
          kit.test = t.equals("true");   
        else
          kit.test = scodeTest;
      }
    }
    compiler.kits = kits;
    quitIfErrors();
  }
  
  private void initResume()
  {  
    final String unhibernate = xml.get("unhibernate", null);
    final String resume = xml.get("resume", null);
    final boolean hasUnhibernate = (unhibernate != null);
    final boolean hasResume = (resume != null);
    
    if (hasUnhibernate && hasResume)
      throw new XException("Cannot specify both 'unhibernate' and 'resume' attributes", xml);
    
    if (hasResume)
      compiler.image.resume = resume;
    else if (hasUnhibernate)
    {
      warn("'unhibernate' attribute is deprecated. Use 'resume' instead.", new Location(xml));
      compiler.image.resume = unhibernate;
    }
    else
    {
      // In build 1.0.47 we renamed sys::Sys.unhibernate to sys::Sys.resume.
      // In order to support scode generation for older sys kits, we have
      // to check the sys version to determine what the default "resume"
      // entry point method should be.
      compiler.image.resume = (sysKit.version.compareTo(new Version("1.0.47")) < 0)
        ? "sys::Sys.unhibernate"
        : "sys::Sys.resume";
    }    
  }

  File xmlFile;
  File xmlDir;
  XElem xml;
  KitFile sysKit;

}
