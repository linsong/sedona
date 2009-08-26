//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 June 2009  Matthew Giannin  Creation
//
package sedona.platform;

/**
 * Exceptions of this type are thrown by the PlatformDb when its operations fail.
 */
public class PlatformDbException extends RuntimeException
{
  public PlatformDbException()
  {
  }
  public PlatformDbException(String message)
  {
    super(message);
  }

  public PlatformDbException(Throwable cause)
  {
    super(cause);
  }

  public PlatformDbException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
