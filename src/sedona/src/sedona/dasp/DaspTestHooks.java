//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Mar 08  Brian Frank  Creation
//

package sedona.dasp;      

import java.util.*;

/**
 * DaspTestHooks is used to insert white-box testing hooks into
 * the protocol stack.
 */
public class DaspTestHooks
{ 
  
////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  /**
   * Get the associated session.
   */
  public DaspSession session() { return session; }
  
  /**
   * Get the next send sequence number.
   */
  public int sendSeqNum() { return session.sendWindow.curSeqNum(); }  

  /**
   * Trap receiving - return false to drop.
   */      
  public boolean receive(int msgType, int seqNum, byte[] msg) { return true; }
  
  /**
   * Trap sends - return false to drop.
   */      
  public boolean send(int msgType, int seqNum, byte[] msg) { return true; }
  
  /**
   * Run the white-box tests which are package scoped.
   */
  public static void runWhiteboxTests()
    throws Exception
  {                                                   
    SendWindow.main(null);
    ReceiveWindow.main(null);
  }    

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  DaspSession session;
                    
}
