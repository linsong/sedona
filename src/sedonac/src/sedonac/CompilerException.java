//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;

import sedona.xml.*;

/**
 * Main command line entry point for the Sedona compiler.
 */
public class CompilerException
  extends RuntimeException
{

  public CompilerException(String msg, Location location)
  {
    super(msg);
    this.location = location;
  }

  public CompilerException(String msg, Location location, Throwable cause)
  {
    super(msg);
    this.location = location;
    this.cause = cause;
  }

  public CompilerException(XException e)
  {
    super(e.getMessage());
    this.location = new Location(e.location);
    this.cause = e.cause;
  }

  public String toLogString()
  {
    String s = getMessage();
    if (location != null) s = location + ": " + s;
    return s;
  }

  public Location location;
  public Throwable cause;

}
