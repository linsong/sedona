//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Oct 06  Brian Frank  Creation
//

package sedona.sox;

/**
 * SoxExchange manages the connection requests and responses.
 */
class SoxExchange
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Create dispatcher for use by specified client.
   */
  SoxExchange(SoxClient client)
  {
    this.client = client;
  }

////////////////////////////////////////////////////////////////
// Send
////////////////////////////////////////////////////////////////

  /**
   * Send the specified batch of requests and wait until
   * we receive a response for each one.  The reply numbers
   * of all the requests will be automatically set (but the caller
   * must have left a one byte spacer).  The requests may be
   * processed out of order on the other side.  There can only
   * be one batch of requests outstanding (this entire method
   * will block all but one caller).
   */
  Msg[] request(Msg[] req)
    throws Exception
  {                                        
    // if we have more than 255, break it up into chunks of 255
    if (req.length > 0xff) 
      return chunkRequests(req);
        
    synchronized (this)
    {
      // assign reply numbers, all req/res messages look like:
      //   [0] u1 command
      //   [1] u1 replyNum
      for (int i=0; i<req.length; ++i)
        req[i].setReplyNum(i);  

      // set req/res fields and init times
      this.requests = req;
      this.responses = new Msg[req.length];
      
      // send them on their way; this call will block if we fill
      // up our send window, but that is ok because the SoxReceiver
      // should still be pulling off the responses and sticking
      // them into our responses array
      for (int i=0; i<req.length; ++i)
        client.send(req[i]);
    
      // wait until we receive all the responses or timeout
      while (true)
      {    
        // check if we've gotten all our responses
        boolean gotAllResponses = true;
        for (int i=responses.length-1; i>=0; --i)
          if (responses[i] == null) { gotAllResponses = false; break; }
    
        // if we've gotten them all then we're done!
        if (gotAllResponses)
        {
          Msg[] res = this.responses;
          this.requests  = null;
          this.responses = null;
          return res;
        }
    
        // wait a bit - dispatcher thread should wake us up
        try { wait(500); } catch(InterruptedException e) {}
        client.checkOpen();
      }        
    }        
  }
  
  /**
   * Break a big request into 255 request chunks since 
   * we only have a one byte reply number.
   */
  private Msg[] chunkRequests(Msg[] req)
    throws Exception
  {                       
    int total = req.length;
    Msg[] res = new Msg[total];
    for (int i=0; i < total;)
    {
      Msg[] chunkReq = new Msg[Math.min(0xff, total-i)];
      System.arraycopy(req, i, chunkReq, 0, chunkReq.length);
      Msg[] chunkRes = request(chunkReq);
      System.arraycopy(chunkRes, 0, res, i, chunkRes.length);      
      i += chunkReq.length;
    } 
    return res;
  }                   
  
////////////////////////////////////////////////////////////////
// Receiver
////////////////////////////////////////////////////////////////

  /**
   * Receive a response message.
   */
  void receive(Msg msg)
  {
    try
    {             
      synchronized (this)
      {
        // parse command and reply number
        int cmd = msg.bytes[0];
        int replyNum = msg.bytes[1] & 0xFF;
        int index = replyNum;
  
        // check if replyNum is inside my current window
        if (requests == null || responses == null ||
            index < 0 || index >= requests.length)
          return;
  
        // verify response command code (capital of req command)
        Msg req = requests[index];       
        if (cmd != '!' && cmd != (req.bytes[0] & ~0x20))
          throw new SoxException("Invalid response code " + cmd + " for " + req.bytes[0]);
  
        // store response and notify requestor thread
        this.responses[index] = msg;
        notify();           
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      client.close();
    }
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  SoxClient client;         // parent client
  Msg[] requests;           // current batch of requests
  Msg[] responses;          // current batch of responses

}
