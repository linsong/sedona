//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;

/**
 * ComplerLog is responsible for outputing compiler messages
 */
public class CompilerLog
  extends sedona.util.Log
{

  public CompilerLog()
  {
  }
  
  public CompilerLog(java.io.OutputStream out)
  {
    super(out);
  }

  public void error(CompilerException e)
  {
    message(e.toLogString());
    if (e.cause != null)
    {
      if (verbose)
        e.cause.printStackTrace(this);
      else
        message("  " + e.cause);
    }
  }

}
