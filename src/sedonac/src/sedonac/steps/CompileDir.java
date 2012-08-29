//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Nov 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.scode.*;

/**
 * CompileDir is used to group a set of compiler targets 
 * together - such as a directory containing a bunch of kits.
 */
public class CompileDir
  extends CompilerStep
{

  public CompileDir(Compiler compiler)
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
      XElem[] targets = xml.elems("target");
      for (int i=0; i<targets.length; ++i)
        compileTarget(targets[i].get("name"));
    }
    catch (XException e)
    {
      throw err(e);
    }
  }                   
  
  public void compileTarget(String name)
  {
    log.info("Compile [" + name + "]");  
    Compiler c = compiler.spawn();
    c.compile(new File(xmlDir, name));
    System.out.println();
  }

  File xmlFile;
  File xmlDir;
  XElem xml;

}
