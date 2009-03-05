//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank        Creation
//    5 Mar 09  Elizabeth McKenney Added options for timestamp & prefix
//

package sedona.util;

import java.io.*;
import java.util.Calendar;

/**
 * Log is responsible for outputting messages
 */
public class Log extends PrintWriter
{

  public Log()
  {
    super(System.out, true);
  }                    
                   
  public Log(OutputStream out)
  {
    super(out, true);
  }                    

  public Log(String pre)
  {
    super(System.out, true);
    prefix = pre;
  }                    
                   
  public Log(OutputStream out, String pre)
  {
    super(out, true);
    prefix = pre;
  }                    
                   
  public void println(String msg)
  {
    if (prefix!=null)
    {
      // Timestamp
      print("[");
      print( Abstime.now().toString() );
      print("]");    

      // Prefix
      if (prefix.length()>0)
      {
        print("[");
        print(prefix);
        print("]");    
      }
    }

    // print msg
    super.println(msg);
  }

   public void message(String msg)
  {
    println(msg);
  }

  public void warning(String msg)
  {
    message("WARNING: " + msg);
  }

  public void verbose(String msg)
  {
    if (verbose) message(msg);
  }

  public void error(String msg, Throwable e)
  {
    message("ERROR: " + msg);
    if (e != null) e.printStackTrace(this);
  }

  public void fatal(String msg, Throwable e)
  {
    message("FATAL: " + msg);
    if (e != null) e.printStackTrace(this);
  }

  public boolean verbose;
  String prefix = null;
}
