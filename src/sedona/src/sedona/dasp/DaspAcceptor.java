//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 Mar 08  Brian Frank  Creation
//

package sedona.dasp;      

import java.util.*;
import sedona.*;

/**
 * DaspAcceptor is responsible for authenticating server sessions.
 */
public abstract class DaspAcceptor
{               
  
  /**
   * Return the user credentials digest which is the the SHA-1 
   * hash of "user:pass".  If user doesn't exist return null.
   */
  public abstract byte[] credentials(String user);

  /**
   * Get the options to use for server sessions.  The default 
   * implementation returns <code>Env.getProperties()</code>
   * which maps "/lib/sedona.properties".  Option property keys:
   *   - dasp.idealMax: ideal packet size in bytes
   *   - dasp.absMax: absolute packet size in bytes
   *   - dasp.receiveTimeout: receive timeout in ms
   *   - dasp.socketQueueMax: max size of per socket queue
   *   - dasp.sessionQueueMax: max size of per session queue
   */
  public Hashtable options()
  {
    return Env.getProperties();
  }                     
                  
}
