/**
 * Copyright 2009 Tridium, Inc. All Rights Reserved.
 */
package sedonacert.prov;

import java.io.*;
import java.util.*;

import sedona.*;
import sedona.platform.*;
import sedona.sox.*;
import sedona.util.TextUtil;
import sedona.xml.*;

import sedonac.Compiler;

import sedonacert.*;

/**
 *
 * @author Matthew Giannini
 * @creation Dec 4, 2009
 *
 */
public abstract class ProvTest extends Test
{
  public ProvTest(String name)
  {
    super(name);
  }
  
  protected final Prov prov() { return (Prov)bundle; }
  
  protected Properties getApp(File saveFile) throws Exception
  {
    return runner.sox.getFile("app.sab", SoxFile.make(saveFile), null, null);
  }
  
  protected Properties getScode(File saveFile) throws Exception
  {
    return runner.sox.getFile("kits.scode", SoxFile.make(saveFile), null, null);
  }
  
  protected Properties putFile(File toPut, String name) throws Exception
  {
    return runner.sox.putFile(name, SoxFile.make(toPut), null, null);
  }
  
  protected void compile(File src)
  {
    Compiler c = new Compiler();
    c.compile(src);
  }
  
  protected void makeScode(Schema schema, File out, boolean compile) throws Exception
  {
    PlatformManifest plat = runner.platform;
    
    XElem root = new XElem("sedonaCode")
    .addAttr("name", "addKit-cert")
    .addAttr("endian", plat.endian)
    .addAttr("armDouble", Boolean.toString(plat.armDouble))
    .addAttr("blockSize", Integer.toString(plat.blockSize))
    .addAttr("refSize", Integer.toString(plat.refSize))
    .addAttr("main", "sys::Sys.main")
    .addAttr("debug", Boolean.toString(plat.debug))
    .addAttr("test", Boolean.toString(plat.test));
    
    Kit[] kits = schema.kits;
    for (int i=0; i<kits.length; ++i)
    {
      // use an exact match
      Depend d = Depend.parse(kits[i].name + " " + 
        kits[i].manifest.version + "=, " + 
        "0x" + TextUtil.intToHexString(kits[i].checksum));
      root.addContent(new XElem("depend").addAttr("on", d.toString()));
    }
   
    root.write(out);
    if (compile) compile(out);
  }

}
