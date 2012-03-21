//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 Oct 06  Brian Frank  Creation
//

package sedona.sox;


/**
 * Base class for sox related exceptions
 */
public class SoxException
  extends RuntimeException
{  
          
  public SoxException(String msg, Throwable cause)
  {                                              
    super(msg);
    this.cause = cause;
  }

  public SoxException(String msg)
  {
    super(msg);
  }

  public SoxException()
  {
  }                      
  
  public Throwable getCause()
  {
    return cause;
  }         
  
  public Throwable cause;
  
}
