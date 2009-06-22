//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import sedonac.Compiler;

/**
 * CompileTest is an end-to-end test from source to VM execution
 */
public abstract class CompileTest
  extends Test
{

  public void compile(String src)
  {
    File f = new File("sedona-test.tmp");
    try
    {
      FileWriter out = new FileWriter(f);
      out.write(src);
      out.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      fail();
    }

    compiler = new Compiler();
    compiler.compile(f);
  }

  Compiler compiler;
  byte[] image;
}
