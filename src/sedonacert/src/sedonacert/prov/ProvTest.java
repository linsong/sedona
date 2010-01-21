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
import sedona.util.Version;
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
  
  protected void provision(File appSab, File scodeBin) throws Exception
  {
    putFile(appSab, "app.sab.writing");
    putFile(scodeBin, "kits.scode.writing");
    runner.sox.renameFile("app.sab.writing", "app.sab.stage");
    runner.sox.renameFile("kits.scode.writing", "kits.scode.stage");
    if (!runner.restart()) fail("could not restart device");
  }
  
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
    
    KitVersion[] deviceKits = runner.version.kits;
    Kit[] schemaKits = schema.kits;
    
    for (int i=0; i<schemaKits.length; ++i)
    {
      final String name = schemaKits[i].name;
      final String checksum = "0x" + TextUtil.intToHexString(schemaKits[i].checksum);
      Version version = schemaKits[i].manifest.version;
      for (int j=0; j<deviceKits.length; ++j)
      {
        // prefer the version on the device to the one determined locally.
        if (deviceKits[j].name.equals(name))
        {
          version = deviceKits[j].version;
          if (deviceKits[j].checksum != schemaKits[i].checksum)
            throw new IllegalStateException("checksum mismatch for " + name + ": " + deviceKits[j].checksum + " <> " + schemaKits[i].checksum);
          break;
        }
      }
      Depend d = Depend.parse(name + " " + version + "=, " + checksum);
      root.addContent(new XElem("depend").addAttr("on", d.toString()));
    }
   
    root.write(out);
    if (compile) compile(out);
  }

}
