//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.util.*;

import sedonac.Compiler;
import sedonac.CompilerException;

/**
 * CompileTest compiles a single Sedona source file.
 */
public abstract class CompileTest
  extends Test
{

  public void compile(String src)
  {
    File xml = new File("compile-test.xml");
    writeKitXml(xml);
    File source = new File("sedona-test.sedona");
    try
    {
      FileWriter out = new FileWriter(source);
      out.write(src);
      out.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      fail();
    }

    try
    {
      compiler = new TestCompiler();
      compiler.compile(xml);
    }
    catch (CompilerException e)
    {
    }

    xml.delete();
    source.delete();
  }
  
  public void writeKitXml(File xml)
  {
    try
    {
      FileWriter out = null;

      // Goofy loop to avoid failure due to transient OS issue (file in use, etc)
      // Let it fail up to N times w/o throwing exception; break out & continue
      // if it succeeds
      for (int t=0; t<4; t++)
      {
        try { out = new FileWriter(xml); } catch (IOException e) { }
        if (out!=null) break;
      }

      // Try once more if necessary, this time catch exception if any & fail
      if (out==null) out = new FileWriter(xml); 

      out.write("<sedonaKit name='sedonacCompileTest' vendor='Tridium' description=''><depend on='sys 1.0+' /><source dir='.' /></sedonaKit>");
      out.close();
    }
    catch (IOException e)
    {
      System.out.println();
      e.printStackTrace();
      fail();
    }
  }
  
  /**
   * Test Compiler. Turns off logging, and captures warnings for testing 
   * purposes.
   */
  protected class TestCompiler extends Compiler
  {
    public TestCompiler()
    {
      super();
      this.log.severity = 1000;
      this.warnings = new ArrayList();
    }
    
    public void warn(String msg)
    {
      super.warn(msg);
      warnings.add(msg);
    }
    public ArrayList warnings;
  }

  protected TestCompiler compiler;
  byte[] image;
}
