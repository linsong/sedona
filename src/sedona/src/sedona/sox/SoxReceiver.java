//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Jan 07  Brian Frank  Creation
//

package sedona.sox;

import java.io.*;
import sedona.*;
import sedona.util.*;

/**
 * SoxReceiver is responsible for pulling messages of the
 * network and routing them as either responses back to a
 * caller via the SoxExchange or handling events.
 */
class SoxReceiver
  extends Thread
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Create dispatcher for use by specified client.
   */
  SoxReceiver(SoxExchange exchange)
  {
    super("SoxReceiver");
    this.exchange = exchange;
  }

////////////////////////////////////////////////////////////////
// Lifecycle
////////////////////////////////////////////////////////////////

  public void kill()
  {
    interrupt();
  }

  public void run()
  {
    while (!exchange.isClosed())
    {
      try
      {
        // check for pending message
        Msg msg = exchange.receive(1000);
        
        // dispatch received message
        if (msg != null) dispatch(msg);
      }
      catch(Exception e)
      {
        if (!exchange.closing && !exchange.isClosed()) e.printStackTrace();
      }
    }
  }

////////////////////////////////////////////////////////////////
// Dispatch
////////////////////////////////////////////////////////////////

  void dispatch(Msg msg)
    throws Exception
  {
    int cmd = msg.command();     
    switch (cmd)
    {
      case 'e':  dispatchEvent(msg); return;
      case 'k':  exchange.fileTransfer.receiveChunk(msg); return;
      case 'z':  exchange.fileTransfer.receiveClose(msg); return;
      default:   exchange.receive(msg); return;
    }
  }

  void dispatchEvent(Msg msg)
  {
    int cmd = '?';
    try
    {
      // parse event into Component
      cmd = msg.u1();
      msg.u1();         // replyNum

      // apply the event
      exchange.client.applyToCache(msg);
    }
    catch(Exception e)
    {
      System.out.println("ERROR: dispatching event cmd=" + (char)cmd + " 0x" + Integer.toHexString(cmd));
      e.printStackTrace();
    }
  }

 ////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  SoxExchange exchange;  // parent client

}
