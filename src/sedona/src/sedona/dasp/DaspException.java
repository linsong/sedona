//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Mar 08  Brian Frank  Creation
//

package sedona.dasp;


/**
 * Base class for sox related exceptions
 */
public class DaspException
  extends java.io.IOException
{  
  
  /**
   * Constructor with message, root cause, and error code.
   */        
  public DaspException(String msg, Throwable cause, int errorCode)
  {                                              
    super(msg);
    this.cause = cause;                                 
    this.errorCode = errorCode;
  }

  /**
   * Constructor with message and error code.
   */        
  public DaspException(String msg, int errorCode)
  {
    super(msg);
    this.errorCode = errorCode;
  }

  /**
   * Constructor with just message.
   */        
  public DaspException(String msg)
  {
    super(msg);
  }

  /**
   * No argument constructor.
   */        
  public DaspException()
  {
  }                      
  
  /**
   * Get the root cause (for older VMs).
   */        
  public Throwable getCause()
  {
    return cause;
  }                  
  
  /**
   * Append the error code to the string if not -1.
   */        
  public String toString()
  {
    String s = super.toString();
    if (errorCode != -1) s += " (err=0x" + Integer.toHexString(errorCode) + ")";
    return s;
  }
  
  public Throwable cause;
  public int errorCode = -1;
  
}
