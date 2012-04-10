//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   6 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import sedona.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;

/**
 * WriteImage writes the scode image to a file
 */
public class WriteImage
  extends CompilerStep
{

  public WriteImage(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    // input.xml -> input.scode
    String filename = compiler.input.getName();
    int dot = filename.lastIndexOf('.');
    if (dot > 0) filename = filename.substring(0, dot);
    filename += ".scode";

    File dir = compiler.outDir;
    if (dir == null) 
      dir = compiler.input.getParentFile();
      
    File file = new File(dir, filename);
    byte[] code = compiler.image.code;

    log.info("  WriteImage [" + file + "] (" + code.length + " bytes)");

    try
    {
      dir.mkdirs();

      OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
      out.write(code);
      out.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot write image file", new Location(file));
    }

    int dataSize = compiler.dataSize;
    int codeSize = compiler.image.code.length;

    log.info("  +----------------------------------"); 
    log.info("  |  Data:   " + TextUtil.kb(dataSize));
    log.info("  |  Code:   " + TextUtil.kb(codeSize));
    log.info("  |  Total:  " + TextUtil.kb(dataSize+codeSize));
    log.info("  +----------------------------------"); 
    
  }

}
