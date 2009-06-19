//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 June 2009  Matthew Giannin  Creation
//
package sedona.platform;

public class DbException extends RuntimeException
{
  public DbException()
  {
  }
  public DbException(String message)
  {
    super(message);
  }

  public DbException(Throwable cause)
  {
    super(cause);
  }

  public DbException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
