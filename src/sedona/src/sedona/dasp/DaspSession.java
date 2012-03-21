//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 Mar 08  Brian Frank  Creation
//

package sedona.dasp;

import java.net.*;
import java.io.*;
import java.security.*;
import java.util.*;

import sedona.util.TextUtil;

/**
 * DaspSession models the client/server endpoint of a session.
 */
public class DaspSession
  implements DaspConst
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Constructor.
   */
  protected DaspSession(DaspSocketInterface iface, int id, InetAddress host, int port, boolean isClient, Hashtable options)
  {
    this.socket          = iface.daspSocket;
    this.iface           = iface;
    this.id              = id;
    this.host            = host;
    this.port            = port;
    this.isClient        = isClient;
    this.isServer        = !isClient;
    this.receiveQueue    = new ReceiveQueue(option(options, "dasp.sessionQueueMax", SESSION_QUEUE_MAX));
    this.receiveWindow   = new ReceiveWindow(this);
    this.sendWindow      = new SendWindow(this);
    this.idealMax        = option(options, "dasp.idealMax",       IDEAL_MAX_DEF);
    this.absMax          = option(options, "dasp.absMax",         ABS_MAX_DEF);
    this.receiveTimeout  = option(options, "dasp.receiveTimeout", RECEIVE_TIMEOUT_DEF);
    this.connectTimeout  = option(options, "dasp.connectTimeout", CONNECT_TIMEOUT_VAL);
    sendWindow.sendRetry = option(options, "dasp.sendRetry",      SEND_RETRY_VAL);
    sendWindow.maxSend   = option(options, "dasp.maxSend",        MAX_SEND);
    this.lastReceive     = ticks();
    this.connectTime     = ticks();

    test = (DaspTestHooks)options.get("dasp.test");
    if (test != null) test.session = this;
  }

  static boolean option(Hashtable options, String key, boolean def)
  {
    String val = (String)options.get(key);
    if (val != null) return val.equals("true");
    return sedona.Env.getProperty(key, def);
  }

  static int option(Hashtable options, String key, int def)
  {
    String val = (String)options.get(key);
    if (val != null) return Integer.parseInt(val);
    return sedona.Env.getProperty(key, def);
  }

  static long option(Hashtable options, String key, long def)
  {
    String val = (String)options.get(key);
    if (val != null) return Long.parseLong(val);
    return sedona.Env.getProperty(key, def);
  }

////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  /**
   * Get the remote endpoint's session id.
   */
  public int remoteId()
  {
    return remoteId;
  }

  /**
   * Get the session tuned ideal max number of
   * bytes per packet (including DASP headers).
   */
  public int idealMax()
  {
    return idealMax;
  }

  /**
   * Get the session tuned absolute max number of
   * bytes per packet (including DASP headers).
   */
  public int absMax()
  {
    return absMax;
  }

  /**
   * Get the local endpoint's max receiving
   * window (number of messages).
   */
  public int localReceiveMax()
  {
    // hardcoded to match 32-bit mask in ReceiveWindow
    return RECEIVE_MAX_VAL;
  }

  /**
   * Get the session remote endpoint's max receiving
   * window (number of messages).
   */
  public int remoteReceiveMax()
  {
    return remoteReceiveMax;
  }

  /**
   * Get elapsed time in milliseconds that will be
   * tolerated without receiving any messages before the
   * session times out.
   */
  public long receiveTimeout()
  {
    return receiveTimeout;
  }

  /**
   * String format.
   */
  public String toString()
  {
    return (isClient ? "Client [" : "Server [") + TextUtil.intToHexString(id,4) + " -> " + TextUtil.intToHexString(remoteId,4) + "]";
  }

  /**
   * Convenience for <code>send(buf, 0, buf.length)</code>.
   */
  public void send(byte[] buf)
    throws Exception
  {
    send(buf, 0, buf.length);
  }

  /**
   * Send the datagram to the remote endpoint.  This method
   * will block the calling thread if the sending window if full.
   */
  public void send(byte[] buf, int off, int len)
    throws Exception
  {
    if (isClosed) throw new DaspException("DaspSession is closed: " + closeCause);
    byte[] payload = new byte[len];
    System.arraycopy(buf, off, payload, 0, len);
    sendWindow.send(payload);
  }

  /**
   * Receive a datagram from the remote endpoint.  This
   * method blocks until a datagram has been received or
   * a timeout occurs.  Return null on timeout.  This
   * method can only be called if using session queuing.
   *
   * @param timeout number of milliseconds to wait
   *    before timing out or -1 to wait forever.
   */
  public DaspMessage receive(long timeout)
    throws Exception
  {
    if (socket.qMode != DaspSocket.SESSION_QUEUING)
      throw new IllegalStateException("not using session queuing mode");

    DaspMessage msg = receiveQueue.dequeue(timeout);
    if (isClosed) throw new DaspException("DaspSession is closed: " + closeCause);
    if (msg == null) return null;
    if (msg.msgType != DATAGRAM) throw new DaspException("Invalid message received: " + msg.msgType);
    return msg;
  }

  /**
   * Receive any type of message from the remote endpoint. This method blocks
   * until a dasp message has been received or a timeout occurs. This method is
   * only applicable if using session queueing.
   *
   * @param timeout
   *          number of milliseconds to wait before timing out, or -1 to wait
   *          forever.
   */
  protected final DaspMessage receiveAny(final long timeout) throws Exception
  {
    if (socket.qMode != DaspSocket.SESSION_QUEUING)
      throw new IllegalStateException("not using session queueing mode");
    return receiveQueue.dequeue(timeout);
  }

  /**
   * Return true if this session has been closed.
   */
  public boolean isClosed()
  {
    return isClosed;
  }

  /**
   * Close the session.
   */
  public void close()
  {
    close(-1, "close() api");
  }

  /**
   * Return short message describing why connection was closed.
   */
  public String closeCause()
  {
    return closeCause;
  }

  /**
   * Close the session with the specified error code.
   * If errorCode is -1 then send twice.  If maxValue then
   * don't include errorCode field, but only send once.
   */
  void close(int errorCode, String cause)
  {
    // if already closed short circuit
    if (isClosed) return;

    // build close message
    DaspMsg close = new DaspMsg();
    close.msgType   = CLOSE;
    close.sessionId = remoteId;
    close.seqNum    = 0xffff;
    if (errorCode != Integer.MAX_VALUE)
    {
      close.errorCode = errorCode;
      if (errorCode == INCOMPATIBLE_VERSION)
        close.version = VERSION_VAL;
    }

    // send close message
    send(close);

    // send normal close twice
    if (errorCode == -1)
    {
      try { Thread.sleep(50); } catch (InterruptedException e) {}
      send(close);
    }

      // shutdown the session
    shutdown(cause);
  }

  /**
   * Shutdown closes down the session and frees it from the socket
   * tables - but doesn't send the remote endpoint a close message.
   */
  void shutdown(String cause)
  {
    synchronized (this)
    {
      // if already closed short circuit
      if (isClosed) return;
      isClosed = true;
      closeCause = cause;

      // Notify listener that session is closing
      try { if (listener != null) listener.daspSessionClosed(this); }
      catch (Exception e) { e.printStackTrace(); }

      // remove from socket table
      socket.free(this);

      // kill send and receive queues
      sendWindow.kill();
      receiveQueue.kill();
    }
  }

////////////////////////////////////////////////////////////////
// Client Handshake
////////////////////////////////////////////////////////////////

  protected void connect()
    throws Exception
  {
    // send hello, receive response
    DaspMsg res = hello();

    // process challenge (or close, welcome)
    DaspMsg welcome = null;
    switch (res.msgType)
    {
      case CLOSE:
        throw new DaspException("Connection denied", res.errorCode);
      case CHALLENGE:
        remoteId = res.remoteId();
        welcome  = authenticate(res);
        break;
      case WELCOME:
        remoteId = res.remoteId();
        welcome  = res;
        break;
      default:
        throw new DaspException("Unexpected response to hello " + res.msgType);
    }

    // process welcome
    switch (welcome.msgType)
    {
      case CLOSE:
        throw new DaspException("Connection denied ", welcome.errorCode);
      case WELCOME:
        tune(welcome);
        break;
      default:
        throw new DaspException("Expected welcome, not " + welcome.msgType);
    }
  }


  DaspMsg hello()
    throws Exception
  {
    // build hello
    DaspMsg hello = new DaspMsg();
    hello.msgType        = HELLO;
    hello.sessionId      = 0xffff;
    hello.seqNum         = sendWindow.curSeqNum();
    hello.remoteId       = this.id;
    hello.version        = VERSION_VAL;
    hello.idealMax       = this.idealMax;
    hello.absMax         = this.absMax;
    hello.receiveMax     = localReceiveMax();
    hello.receiveTimeout = this.receiveTimeout;

    // wait for response
    long timeout = connectTimeout/3;
    if (timeout < 1000) timeout = 1000;
    for (int i=0; i<3; ++i)
    {
      send(hello);
      DaspMsg res = receiveQueue.dequeue(timeout);
      if (res != null) return res;
    }
    throw new DaspException("No response from hello");
  }

  DaspMsg authenticate(DaspMsg challenge)
    throws Exception
  {
    // process challenge
    int err = -1;
    String algorithm = challenge.digestAlgorithm();
    if (algorithm != "SHA-1")
    {
      close(DIGEST_NOT_SUPPORTED, "digest not supported");
      throw new DaspException("Unsupported algorithm", DIGEST_NOT_SUPPORTED);
    }

    // compute digest
    MessageDigest md = MessageDigest.getInstance("SHA");
    byte[] cred = md.digest((user + ":" + pass).getBytes("UTF-8"));
    md.reset();
    md.update(cred);
    md.update(challenge.nonce());
    byte[] digest = md.digest();

    // build authenticate message
    DaspMsg auth = new DaspMsg();
    auth.msgType   = AUTHENTICATE;
    auth.sessionId = remoteId;
    auth.seqNum    = sendWindow.curSeqNum();
    auth.username  = user;
    auth.digest    = digest;

    // wait for response
    long timeout = connectTimeout/3;
    if (timeout < 1000) timeout = 1000;
    for (int i=0; i<3; ++i)
    {
      send(auth);
      DaspMsg res = receiveQueue.dequeue(timeout);
      // If multiple hello messages were sent, we might receive a second
      // challenge response. Drop it.
      if (res != null && res.msgType() != DaspSocket.CHALLENGE) return res;
    }
    throw new DaspException("No response from authenticate");
  }


////////////////////////////////////////////////////////////////
// Server Handshake
////////////////////////////////////////////////////////////////

  void challenge(DaspMsg hello)
  {
    // process hello
    this.remoteId = hello.remoteId();
    if (hello.version() != VERSION_VAL)
    {
      close(INCOMPATIBLE_VERSION, "incompatible version");
      return;
    }
    tune(hello);

    // generate a nonce
    this.nonce = new byte[10];
    for (int i=0; i<nonce.length; ++i)
     this.nonce[i] = (byte)socket.rand.nextInt();

    // build challenge message
    DaspMsg chal = new DaspMsg();
    chal.msgType   = CHALLENGE;
    chal.sessionId = remoteId;
    chal.seqNum    = sendWindow.curSeqNum();
    chal.remoteId  = this.id;
    chal.nonce     = nonce;

    // send it once
    send(chal);
  }

  void welcome(DaspMsg auth)
  {
     // authenticate
     boolean ok = false;
     try
     {
       byte[] cred = socket.acceptor.credentials(auth.username());
       if (cred != null)
       {
         MessageDigest md = MessageDigest.getInstance("SHA");
         md.update(cred);
         md.update(this.nonce);
         byte[] expected = md.digest();
         byte[] actual = auth.digest();
         ok = equals(actual, expected);
       }
     }
     catch (Exception e)
     {
       e.printStackTrace();
     }

     // if not authenticated
     if (!ok)
     {
       close(NOT_AUTHENTICATED, "not authenticated");
       return;
     }

     // build welcome
     DaspMsg welcome = new DaspMsg();
     welcome.msgType        = WELCOME;
     welcome.sessionId      = remoteId;
     welcome.seqNum         = sendWindow.curSeqNum();
     welcome.remoteId       = this.id;
     welcome.idealMax       = this.idealMax;
     welcome.absMax         = this.absMax;
     welcome.receiveMax     = localReceiveMax();
     welcome.receiveTimeout = this.receiveTimeout;

     // send welcome
     send(welcome);
  }

////////////////////////////////////////////////////////////////
// Handshake Common
////////////////////////////////////////////////////////////////

  static boolean equals(byte[] a, byte[] b)
  {
    if (a.length != b.length) return false;
    for (int i=0; i<a.length; ++i)
      if (a[i] != b[i]) return false;
    return true;
  }

  void tune(DaspMsg x)
  {
    this.idealMax         = Math.min(x.idealMax(), this.idealMax);
    this.absMax           = Math.min(x.absMax(), this.absMax);
    this.remoteReceiveMax = x.receiveMax();
    this.receiveTimeout   = Math.max(x.receiveTimeout(), this.receiveTimeout);
    sendWindow.sendSize   = this.remoteReceiveMax;
    receiveWindow.init(x.seqNum);
  }

////////////////////////////////////////////////////////////////
// Dispatch
////////////////////////////////////////////////////////////////

  /**
   * Dispatch a message for this session - this callback
   * occurs on the DaspSocket Receiver thread.
   */
  void dispatch(DaspMessage msg)
  {
    // test hooks to drop received packets
    if (test != null && !test.receive(msg.msgType, msg.seqNum, msg.payload)) return;

    // increment counter
    numReceived++;

    // keep track of last time we message is received
    lastReceive = ticks();

    // authenticate is always immediately
    // processed on the receiver thread
    if (msg.msgType == AUTHENTICATE) { welcome(msg); return; }

    // let send window check for acks
    sendWindow.checkAckHeaders(msg);

    // if not within the receiving window then toss it
    if (msg.msgType == DATAGRAM && !receiveWindow.receive(msg.seqNum))
      return;

    // these message types just get stuck onto the queue
    switch (msg.msgType)
    {
      // these get stuck on the queue during handshake
      // so the connecting thread can do the processing
      case CHALLENGE:
      case WELCOME:
        enqueue(msg);
        return;

      // close gets stuck on the queue during handshake
      // so the connecting thread can do the processing,
      // but we also shutdown the session (we don't use
      // close b/c we don't need to send the close msg)
      case CLOSE:
        enqueue(msg);
        shutdown("remote endpoint sent close (" + msg.errorCode + ")");
        return;

      // datagrams always stuck on the queue to
      // be dequeued in the receive method
      case DATAGRAM:
        if (socket.qMode == DaspSocket.SOCKET_QUEUING)
          socket.enqueue(msg);
        else
          enqueue(msg);
        return;

      // keep alives don't need further processing
      case KEEPALIVE:
        return;

      // huh?
      default:
        System.out.println("DaspSession unexpected msgType=" + msg.msgType);
    }
  }

  /**
   * Enqueue a message for the session - if the queue is full then
   * we assume something bad has happened to the application processing
   * this queue, so we kill the session (we never want to block the
   * socket receiver thread).
   */
  void enqueue(DaspMessage msg)
  {
    try
    {
      receiveQueue.enqueue(msg);
    }
    catch (ReceiveQueue.FullException e)
    {
      System.out.println("ERROR: DaspSession queue full!");
      close(TIMEOUT, "receive queue full");
    }
  }

////////////////////////////////////////////////////////////////
// HouseKeeping
////////////////////////////////////////////////////////////////

  /**
   * House keeping is called every 100ms on a background thread
   * shared by all sessions on a given socket.  House keeping is
   * used to check for retries, acks, keep-alives, and timeouts.
   */
  void houseKeeping()
  {
    // if we haven't heard from the remote endpoint in a
    // while, then its curtains for this session
    if (ticks() - lastReceive > receiveTimeout)
    {
      close(TIMEOUT, "receive timeout");
      return;
    }

    // check for retries, if we've had something enqueued
    // longer than receive time out, then the remote endpoint
    // isn't acking anymore and we close the session
    long oldestPacket = sendWindow.sendRetries();
    if (oldestPacket > receiveTimeout)
    {
      close(TIMEOUT, "unacked send timeout");
      return;
    }

    // send keep-alive if we have pending acks or
    // just haven't sent anything in a while
    if (receiveWindow.unacked() || ticks() - lastSend > receiveTimeout/3)
      keepAlive();
  }

  /**
   * Send the keep alive message.
   */
  void keepAlive()
  {
    DaspMsg keepAlive = new DaspMsg();
    keepAlive.msgType    = KEEPALIVE;
    keepAlive.sessionId  = remoteId;
    keepAlive.seqNum     = 0xffff;
    receiveWindow.setAckHeaders(keepAlive);
    send(keepAlive);
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  protected void send(DaspMsg msg)
  {
    lastSend = ticks();
    if (test != null && !test.send(msg.msgType, msg.seqNum, msg.payload)) return;
    socket.send(this, msg);
  }

  static long ticks() { return sedona.Env.ticks(); }

////////////////////////////////////////////////////////////////
// Debug
////////////////////////////////////////////////////////////////

  public long uptime()          { return ticks() - connectTime; }
  public long lastSend()        { return lastSend; }
  public long lastReceive()     { return lastReceive; }
  public int numSent()          { return numSent; }
  public int numReceived()      { return numReceived; }
  public int numRetries()       { return numRetries; }
  public int sendWindowSize()   { return sendWindow.sendSize; }
  public long sendWindowRetry() { return sendWindow.sendRetry; }
  public int[] ackTimes()       { return (int[])sendWindow.ackTimes.clone(); }

////////////////////////////////////////////////////////////////
// Listeners
////////////////////////////////////////////////////////////////

  public static interface Listener
  {
    public void daspSessionClosed(DaspSession session);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static final boolean CLIENT = true;
  static final boolean SERVER = false;

  public final int id;             // local session id
  public final boolean isClient;   // true for client, false for server
  public final boolean isServer;   // false for client, true for server
  public final DaspSocket socket;  // associated socket
  public final InetAddress host;   // remote IP host address
  public final int port;           // remote IP port
  public final DaspTestHooks test; // white-box testing hooks
  public final DaspSocketInterface iface; // socket interface for session
  public Object userData;          // whatever you want!
  public Listener listener;        // listen for close
  public boolean traceSend;        // trace messages for session
  public boolean traceReceive;     // trace messages for session
  int remoteId;                    // remote session id
  volatile boolean isClosed;       // has the session been closed
  int numSent;                     // number packets sent
  int numReceived;                 // number packets sent
  int numRetries;                  // number of retries
  String closeCause = "???";       // why was the session closed
  String user;                     // username
  String pass;                     // password client side only
  byte[] nonce;                    // one-time nonce server side only
  ReceiveQueue receiveQueue;       // receiving queue
  ReceiveWindow receiveWindow;     // receiving window
  SendWindow sendWindow;           // sending window
  long connectTimeout;             // ms
  int remoteReceiveMax;            // session tuned
  int idealMax;                    // session tuned
  int absMax;                      // session tuned
  long receiveTimeout;             // session tuned (ms)
  long connectTime;                // ticks we connected
  long lastSend;                   // ms ticks
  long lastReceive;                // ms ticks

}
