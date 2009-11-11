//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;

import java.io.*;
import java.text.*;
import java.lang.reflect.*;
import java.util.*;
import sedona.Env;
import sedona.util.*;
import sedonac.test.*;

/**
 * Main command line entry point for the Sedona compiler.
 */
public class Main
{

  public static void usage()
  {
    println("usage:");
    println("  sedonac [options] <input file>");
    println("inputs:");
    println("  dir          directory containing kit.xml file");
    println("  kit.xml      compile Sedona source files into kit file");
    println("  scode.xml    compile Sedona kits into a scode image");
    println("  *.sax        convert sax to sab");
    println("  *.sab        convert sab to sax");
    println("options:");
    println("  -doc         generate HTML Sedona docs for kit");
    println("  -outDir      output directory");
    println("  -v           verbose logging");
    println("  -ver         print version info and exit");
    println("  -? -help     print this usage synopsis");
    println("  -test        run test suite");
    println("  -layout      dump field layout (when compiling image)");
    println("  -kitVersion  force output kit to have specified version");
    println("  -noOptimize  skip const folding and optimization steps");
    println("  -noChecksum  exclude checksums from sax if input is sab file");
  }

  public static void println(String msg)
  {
    System.out.println(msg);
  }

  public static int doMain(String args[])
  {
    // check vm version
    if (!Env.checkJavaVersion())
      return 1;

    // no args
    if (args.length == 0)
    {
      usage();
      return 1;
    }

    // if first arg is class name, then run that main (so
    // launcher can always use this entry point)
    if (args[0].startsWith("sedona.") || args[0].startsWith("sedonac."))
    {
      try
      {
        Class cls = Class.forName(args[0]);
        String[] a = new String[args.length-1];
        System.arraycopy(args, 1, a, 0, a.length);
        cls.getMethod("main", new Class[] { String[].class })
          .invoke(null, new Object[] { a });
      }
      catch (InvocationTargetException e)
      {
        e.getTargetException().printStackTrace();
      }
      catch (Exception e)
      {
        System.out.println("ERROR: Cannot run main: " + args[0]);
        e.printStackTrace();
      }
      return 0;
    }

    // init compiler
    Compiler compiler = new Compiler();
    String input = null;

    // process args
    for (int i=0; i<args.length; ++i)
    {
      String arg = args[i];
      if (arg.equals("-?") || arg.equals("-help"))
      {
        usage();
        return 1;
      }
      else if (arg.equals("-doc"))
      {
        compiler.doc = true;
      }
      else if (arg.equals("-outDir"))
      {
        if (i+1 >= args.length) 
          println("WARNING: Invalid outDir option");
        else
          compiler.outDir = new File(args[++i]);          
      }
      else if (arg.equals("-ver"))
      {
        Env.printVersion("Sedona Compiler");
        return 1;
      }
      else if (arg.equals("-v"))
      {
        compiler.log.severity = Log.DEBUG;
      }
      else if (arg.equals("-noOptimize"))
      {
        compiler.optimize = false;
      }
      else if (arg.equals("-www"))
      {
        compiler.www = true;
      }
      else if (arg.equals("-layout"))
      {
        compiler.dumpLayout = true;
      }
      else if (arg.equals("-test"))
      {
        String testName = null;
        if (i+1 < args.length) testName = args[i+1];
        return Test.run(testName);
      }
      else if (arg.equals("-kitVersion"))
      {
        if (i+1 >= args.length) 
          println("WARNING: Invalid kitVersion option");
        else
          compiler.kitVersion = new Version(args[++i]);          
      }
      else if (arg.equals("-noChecksum"))
      {
        compiler.nochk = true;
      }
      else if (arg.startsWith("-"))
      {
        println("WARNING: Unknown option " + arg);
      }
      else
      {
        if (input == null)
          input = arg;
        else
          println("WARNING: Ignoring argument " + arg);
      }
    }

    if (input == null)
    {
      System.out.println("ERROR: No input specified");
      return 1;
    }

    // run compiler as setup by arguments
    try
    {
      compiler.compile(new File(input));
      System.out.println("*** Success! ***");
      return 0;
    }
    catch(CompilerException e)
    {
      int num = compiler.logErrors();
      if (num == 0)
        e.printStackTrace();
      else if (num == 1)
        System.out.println("*** FAILED with 1 error ***");
      else
        System.out.println("*** FAILED with " + compiler.errors().length + " errors ***");
      return 1;
    }
    catch(Throwable e)
    {
      compiler.log.error("Internal compiler error", e);
      e.printStackTrace();
      return 1;
    }
  }

  public static void main(String args[])
  {
    try
    {
      int r = doMain(args);
      System.exit(r);
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
  }

}
