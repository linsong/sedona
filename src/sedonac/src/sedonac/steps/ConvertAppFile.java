//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import sedona.Env;
import sedona.Buf;
import sedona.offline.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;

/**
 * ConvertAppFile translate application files between XML and binary format.
 */
public class ConvertAppFile
  extends CompilerStep
{

  public ConvertAppFile(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    XElem xml    = compiler.xml;
    File from    = compiler.input;
    File dir     = compiler.outDir;
    if (dir == null)
      dir = from.getParentFile();
    String base  = FileUtil.getBase(from.getName());
    String toExt = xml == null ? "sax" : "sab";
    File to      = new File(dir, base + "." + toExt);

    log.info("  ConvertAppFile [" + from + " -> " + to + "]");

    // load
    OfflineApp app = null;
    try
    {
      if (xml == null)
        app = OfflineApp.decodeAppBinary(Buf.readFrom(from));
      else
        app = OfflineApp.decodeAppXml(xml);
    }
    catch (XException e)
    {
      throw err(e);
    }
    catch (Exception e)
    {
      throw err(new CompilerException("Cannot load app file", new Location(from), e));
    }

    // save
    try
    {
      if (xml == null)
        app.encodeAppXml(to,compiler.nochk);
      else
        app.encodeAppBinary().writeTo(to);
    }
    catch (XException e)
    {
      throw err(e);
    }
    catch (Exception e)
    {
      throw err(new CompilerException("Cannot save app file", new Location(to), e));
    }             
    
    // report                        
    log.info("  +----------------------------------"); 
    log.info("  |  RAM:   " + TextUtil.kb(app.ramSize()));
    log.info("  |  FLASH: " + TextUtil.kb(app.flashSize()));
    log.info("  +----------------------------------"); 
  }                         
  
}
