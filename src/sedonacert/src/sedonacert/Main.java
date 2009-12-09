//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert;

import sedona.*;

/**
 * Main command line entry point for the Sedona certification test harness.
 */
public class Main
{

  public static void usage()
  {
    println("usage:");
    println("  sedonacert [options] <host[:port]> <user> <pass>");
    println("options:");
    println("  -ver         print version info and exit");
    println("  -? -help     print this usage synopsis");
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

    // process args   
    Runner runner = new Runner();
    for (int i=0; i<args.length; ++i)
    {
      String arg = args[i];
      if (arg.equals("-?") || arg.equals("-help"))
      {
        usage();
        return 1;
      }
      else if (arg.equals("-ver"))
      {
        Env.printVersion("Sedona Certification Test Harness");
        return 1;
      }    
      else if (arg.startsWith("-"))
      {
        println("WARNING: unknown option '" + arg + "'");
      }                                                  
      else if (runner.host == null)
      {
        int colon = arg.indexOf(':');
        runner.host = arg;
        runner.port = 1876;
        if (colon != -1)
        {
          try
          {
            if (colon == 0 || colon == arg.length() -1) throw new Exception();
            runner.host = arg.substring(0, colon);
            runner.port = Integer.parseInt(arg.substring(colon+1));
          }
          catch (Exception e)
          {
            println("ERROR: invalid host parameter: " + arg);
            usage();
            return 1;
          }
        }
      }
      else if (runner.username == null) runner.username = arg;
      else if (runner.password == null) runner.password = arg;
    }
    
    // ensure we got enough args
    if (runner.password == null) 
    {
      println("WARNING: not enough arguments");
      usage();
      return 1;
    } 
    
    // execute       
    try
    {
      return runner.run(); 
    }
    catch (Exception e)
    { 
      println("ERROR: Cannot run test harness");
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
