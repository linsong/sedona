//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Oct 06  Brian Frank  Creation
//

package sedona.sox;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Properties;

import sedona.dasp.DaspMsg;
import sedona.dasp.DaspSession;
import sedona.dasp.DaspSocket;


/**
 * SoxExchange manages the connection requests and responses.
 */
public class SoxExchange
    implements ISoxComm
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Create dispatcher for use by specified client.
   */
  SoxExchange(SoxClient client)
  {
    this.client = client;
    this.socket = client.socket;
    this.addr = client.addr;
    this.port = client.port;
    this.username = client.username;
    this.password = client.password;
  }

//////////////////////////////////////////////////////////////////////////
// ISoxComm
//////////////////////////////////////////////////////////////////////////

  /**
   * Send a single message and do not wait for any response.
   */
  public void send(Msg buf)
    throws Exception
  {
    checkOpen();

    if (client.traceMsg)
      System.out.println("--> [send] " + (char)buf.command() + " replyNum=" + buf.replyNum());

    session.send(buf.bytes, 0, buf.size);
  }

  /**
   * Send a single request and wait for the response.
   */
  public Msg request(Msg req)
    throws Exception
  {
    return request(new Msg[] { req })[0];
  }

  /**
   * Send the specified batch of requests and wait until
   * we receive a response for each one.  The reply numbers
   * of all the requests will be automatically set (but the caller
   * must have left a one byte spacer).  The requests may be
   * processed out of order on the other side.  There can only
   * be one batch of requests outstanding (this entire method
   * will block all but one caller).
   */
  public Msg[] request(Msg[] req)
    throws Exception
  {
    checkOpen();

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
      {
        send(req[i]);
      }

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
        checkOpen();
      }        
    }        
  }

  /**
   * Connect to the remote sedona server using the
   * parameters passed to the constructor.
   */
  public synchronized void connect(Hashtable options)
    throws Exception
  {
    // if already opened, raise exception
    if (!isClosed())
      throw new SoxException("Already open!");

    // open the dasp session
    session = socket.connect(addr, port, username, password, options);
    session.listener = new DaspSession.Listener()
    {
      public void daspSessionClosed(DaspSession s)
      {
        closeCause = s.closeCause();
        if (!closing) close();
      }
    };
    closeCause = "???";

    // launch receiver thread
    receiver = new SoxReceiver(this);
    receiver.start();
  }

  /**
   * Return the SoxClient for interpreting the object model.
   */
  public SoxClient client()
  {
    return client;
  }

  /**
   * Return the underlying DaspSession or null if closed.
   * The DaspSession should never be used directly for messaging.
   */
  public DaspSession session()
  {
    return session;
  }

  /**
   * Return the local session id or -1 if closed.
   */
  public int localId()
  {
    DaspSession s = this.session;
    return s == null ? -1 : s.id;
  }

  /**
   * Return the remote session id or -1 if closed.
   */
  public int remoteId()
  {
    DaspSession s = this.session;
    return s == null ? -1 : s.remoteId();
  }

  /**
   * Is this session currently closed.
   */
  public boolean isClosed()
  {
    DaspSession s = this.session;
    if (s == null) return true;
    if (s.isClosed()) { closeCause = s.closeCause(); return true; }
    return false;
  }

  /**
   * Close this session.
   */
  public void close()
  {
    if (this.closing) return;
    this.closing = true;

    // shut down receiver
    try
    {
      SoxReceiver r = this.receiver;
      if (r != null) r.kill();
      this.receiver = null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // close the dasp session
    try
    {
      DaspSession  s = this.session;
      if (s != null) s.close();
      this.session = null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // null out cached state
    this.session  = null;
    this.receiver = null;
    client.cache    = new SoxComponent[1024];
    client.allTreeEvents = false;
    client.util  = null;
    client.close();

    // notify listener if registered
    try
    {
      client.closed();
    }
    catch(Throwable  e)
    {
      e.printStackTrace();
    }

    // done closing
    this.closing = false;
  }

  /**
   * Does this ISoxComm have an underlying subscription for this
   * SoxComponent?
   * SoxExchange always returns false; this is only used for socket-queued
   * connections.
   * @param c
   * @return false always
   */
  public boolean isSubscribed(SoxComponent c)
  {
    return false;
  }


//////////////////////////////////////////////////////////////////////////
// Receiver
//////////////////////////////////////////////////////////////////////////

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
      close();
    }
  }

  
//////////////////////////////////////////////////////////////////////////
// File Transfer
//////////////////////////////////////////////////////////////////////////

  /**
   * Read the specified URI into the given file with the
   * specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   */
  public synchronized Properties getFile(String uri, SoxFile file,
                                         Properties headers,
                                         TransferListener listener)
    throws Exception
  {
    fileTransfer = new FileTransfer(this, uri, file, headers, listener);
    try
    {
      return fileTransfer.getFile();
    }
    finally
    {
      fileTransfer = null;
    }
  }

  /**
   * Write the file specified URI using the contents of the given SoxFile
   * with the specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   *   - staged: true to put as staged file (defaults to false)
   */
  public synchronized Properties putFile(String uri, SoxFile file,
                                         Properties headers,
                                         TransferListener listener)
    throws Exception
  {
    fileTransfer = new FileTransfer(this, uri, file, headers, listener);
    try
    {
      return fileTransfer.putFile();
    }
    finally
    {
      fileTransfer = null;
    }
  }


  /**
   * Rename a file on the remote device.
   */
  public synchronized void renameFile(String from, String to)
    throws Exception
  {
    // build request
    Msg req = Msg.prepareRequest('b');
    req.str(from);
    req.str(to);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('B');
  }

////////////////////////////////////////////////////////////////
// Networking
////////////////////////////////////////////////////////////////

  /**
   * Convenience for <code>connect(null)</code>.
   */
  public synchronized void connect()
    throws Exception
  {
    connect(null);
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

  Msg receive(long timeout)
    throws Exception
  {
    DaspMsg rec = session.receive(timeout);
    if (rec == null) return null;

    Msg msg = new Msg(rec.payload());
    if (client.traceMsg)
      System.out.println("<-- [recv] " + (char)msg.command() + " replyNum=" + msg.replyNum());
    return msg;
  }

  void checkOpen()
  {
    if (isClosed()) throw new SoxException("SoxClient closed: " + closeCause);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  SoxClient client;         // parent client
  Msg[] requests;           // current batch of requests
  Msg[] responses;          // current batch of responses

  public final DaspSocket socket;
  public final InetAddress addr;
  public final int port;
  public final String username;
  final String password;
  DaspSession session;
  String closeCause = "never opened";
  SoxReceiver receiver;
  FileTransfer fileTransfer;
  volatile boolean closing;
}
