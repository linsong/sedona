//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedona.util;

import java.io.*;

/**
 * Log is responsible for outputing messages
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

}
