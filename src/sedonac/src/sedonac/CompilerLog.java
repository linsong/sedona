//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;     

import java.io.*;

/**
 * ComplerLog is responsible for outputing compiler messages
 */
public class CompilerLog
  extends sedona.util.Log
{

  public CompilerLog()
  {
    this(System.out);
  }
  
  public CompilerLog(PrintStream out)
  {
    super("sedonac", out);
  }

  public void error(CompilerException e)
  {
    log(ERROR, e.toLogString(), e.cause);
  }                        
  
  public void log(int severity, String msg, Throwable ex)
  {
    if (severity < this.severity) return;
    out.println(msg);  // skip severity/timestamp/name
    if (ex != null) ex.printStackTrace(out);
  } 

}
