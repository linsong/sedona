//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Mar 08  Brian Frank  Creation
//

package sedona.dasp;      

import java.io.*;
import java.net.*;
import java.util.*;
import sedona.*;

/**
 * DaspSocket manages a UDP socket which is used by multiple
 * client and/or server sessions.
 */
public class DaspSocket
  implements DaspConst
{ 

////////////////////////////////////////////////////////////////
// Construction
////////////////////////////////////////////////////////////////
  
  /**
   * Open a socket to use for DASP communication.  If port
   * is -1 then an ephemeral port is allocated.  In order to
   * accept new incoming server sessions, then an acceptor
   * must be passed in to process authentications.  If acceptor 
   * is null, then this socket may only be used for client 
   * side sessions.                       
   *
   * DaspSockets are configured in either socket or session queuing
   * mode.  In session queuing mode datagrams received are placed
   * onto a queue per session and received via DaspSession.receive().
   * Session queuing mode is required by SoxClient.  In socket queuing
   * mode datagrams are placed onto a single queue shared by all 
   * sessions and received via DaspSocket.receive().
   */
  public static DaspSocket open(int port, DaspAcceptor acceptor, int queuingMode)
    throws Exception                           
  {                                                              
    if (queuingMode != SOCKET_QUEUING && queuingMode != SESSION_QUEUING)
      throw new IllegalArgumentException("invalid queueingMode");
      
    DaspSocket s = new DaspSocket(acceptor, queuingMode);
    s.addInterface(new DefaultDaspSocketInterface(port));
    return s;                                                                        
  }                
  
  protected DaspSocket(DaspAcceptor a, int qMode)
  {
    Hashtable options = Env.getProperties();
    if (a != null) options = a.options();
    
    this.acceptor       = a;  
    this.isAlive        = true;
    this.sessions       = new HashMap(300);      
    this.rand           = new Random();     
    this.interfacesLock = new Object();  
    this.interfaces     = new DaspSocketInterface[0]; 
    this.qMode          = qMode;      
    this.queue          = new ReceiveQueue(DaspSession.option(options, "dasp.socketQueueMax", SOCKET_QUEUE_MAX));   
    this.traceSend      = DaspSession.option(options, "dasp.traceSend", false);   
    this.traceReceive   = DaspSession.option(options, "dasp.traceReceive", false);   
  }
    
////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  /**
   * Return if this socket is setup for session based 
   * queuing or socket based queuing.
   */
  public int queuingMode()
  {         
    return qMode;
  }
  
  /**
   * Get the acceptor configured to authenticate server sessions.
   */
  public DaspAcceptor acceptor()
  {
    return acceptor;
  }

  /**
   * Get the list interaces for this socket.
   */
  public DaspSocketInterface[] interfaces()
  {                    
    synchronized (interfacesLock)
    {
      return (DaspSocketInterface[])interfaces.clone(); 
    }
  }
  
  /**
   * Get the network interface used to route packets
   * to the specified address.
   */
  public DaspSocketInterface route(InetAddress addr, int port)
  {                           
    DaspSocketInterface[] interfaces = interfaces();
    for (int i=interfaces.length-1; i>=0; --i)
    {                  
      DaspSocketInterface iface = interfaces[i];   
      if (iface.routes(addr, port)) return iface;
    }
    throw new IllegalStateException(); 
  }

  /**
   * Bind the specified interface to this socket.
   */
  public void addInterface(DaspSocketInterface iface)
  {
    synchronized (interfacesLock)
    {
      DaspSocketInterface[] temp = new DaspSocketInterface[interfaces.length+1];
      System.arraycopy(interfaces, 0, temp, 0, interfaces.length);
      temp[interfaces.length] = iface;
      interfaces = temp;
      iface.start(this);
    }
  }

  /**
   * Unbind the specified interface to this socket.
   */
  public void removeInterface(DaspSocketInterface iface)
  {
    synchronized (interfacesLock)
    {                                
      ArrayList temp = new ArrayList(interfaces.length);
      for (int i=0; i<interfaces.length; ++i)
        if (interfaces[i] != iface) temp.add(interfaces[i]);
      interfaces = (DaspSocketInterface[])temp.toArray(new DaspSocketInterface[temp.size()]);
    }
  }

  /**
   * Get the list of all open sessions associated with this socket.
   */
  public DaspSession[] sessions()
  {                    
    synchronized (sessions)
    {
      return (DaspSession[])sessions.values().toArray(new DaspSession[sessions.size()]); 
    }
  }

  /**
   * Get a session by sessionId or return null.
   */
  public DaspSession session(int id)
  {
    synchronized (sessions)
    {
      return (DaspSession)sessions.get(new Integer(id));
    }
  }     

  /**
   * Get the local port this socket is bound to.
   */
  public int port()                             
  {
    return ((DefaultDaspSocketInterface)interfaces[0]).sock.getLocalPort();
  }
  
  /**
   * Return if this socke has been closed.
   */
  public boolean isClosed()
  {                                       
    return !isAlive;
  }
  
  /**
   * Close the socket and all its associated sessions.
   */
  public void close()
  {                     
    // close all sessions                    
    DaspSession[] sessions = sessions();
    for (int i=0; i<sessions.length; ++i)
    {
      try 
      {
        sessions[i].close(Integer.MAX_VALUE, "socket closed");
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }     
    } 
        
    // close down interfaces
    this.isAlive = false;
    DaspSocketInterface[] interfaces = interfaces();
    for (int i=0; i<interfaces.length; ++i)
    {
      try
      {                     
        interfaces[i].stop();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }                        
  }

  /**
   * Convenience for <code>connect(addr, port, user, pass, System.getProperties())</code>.
   */
  public DaspSession connect(InetAddress addr, int port, String user, String pass)
    throws Exception
  {
    return connect(addr, port, user, pass, null);             
  }
  
  /**
   * Open a client session to the specified address with 
   * the given credentials.  The options properties may be used
   * to change default settings - see DaspAcceptor.option() for
   * the list of options.
   */
  public DaspSession connect(InetAddress addr, int port, String user, String pass, Hashtable options)
    throws Exception
  {             
    if (!isAlive) throw new IllegalStateException("socket is closed");
    if (options == null) options = System.getProperties();
    DaspSocketInterface iface = route(addr, port);
    DaspSession s = alloc(iface, addr, port, DaspSession.CLIENT, options);
    s.user = user;
    s.pass = pass;
    s.connect();
    return s;
  }   

////////////////////////////////////////////////////////////////
// Socket Queuing Mode
////////////////////////////////////////////////////////////////

  /**
   * Receive a datagram from one of the sessions.  This
   * method blocks until a datagram has been received or
   * a timeout occurs.  Return null on timeout.  This
   * method can only be called if using socket queuing.
   *
   * @param timeout number of milliseconds to wait
   *    before timing out or -1 to wait forever.
   */
  public DaspSessionMessage receive(long timeout)
    throws Exception
  {                     
    if (qMode != DaspSocket.SOCKET_QUEUING)
      throw new IllegalStateException("not using socket queuing mode");
              
    DaspSessionMessage msg = queue.dequeue(timeout);
    if (msg == null) return null;
    if (msg.msgType != DATAGRAM) throw new DaspException("Invalid message received: " + msg.msgType);
    return msg;
  }                           

  /**
   * Enqueue a message for the socket - if the 
   * queue is full then we have big problems.
   */
  void enqueue(DaspSessionMessage msg)
  {
    try
    {                    
      queue.enqueue(msg);
    }
    catch (ReceiveQueue.FullException e)
    { 
      System.out.println("ERROR: DaspSocket queue full!");
    }
  }
    
////////////////////////////////////////////////////////////////
// Dispatch
////////////////////////////////////////////////////////////////
    
  /**
   * Dispatch a packet to its session.  If this is a hello
   * message, then allocate a new server session to handle.
   */
  void dispatch(DaspSocketInterface iface, DatagramPacket packet)
    throws Exception
  {                                  
    // extract packet info                          
    InetAddress host = packet.getAddress();
    int port         = packet.getPort();
    byte[] buf       = packet.getData();
    int len          = packet.getLength();  

    // parse into Msg
    DaspSessionMessage msg = new DaspSessionMessage(buf, len);
    
    // lookup session
    DaspSession session = session(msg.sessionId);
    
    // IO hook
    received(iface, session, msg);

    // if hello, then we need to need to allocate a session
    if (msg.msgType == HELLO)
    {
      // ensure we have an acceptor
      if (acceptor == null) 
        throw new IllegalStateException("Received hello with no acceptor");

      // allocate a new server session
      DaspSession s = alloc(iface, host, port, DaspSession.SERVER, acceptor.options());
      s.challenge(msg);
      return;
    }
  
    // otherwise ensure host/port are valid
    if (session == null || 
        !session.host.equals(host) || 
        session.port != port) return;
    
    // dispatch to session for handling  
    msg.session = session;
    session.dispatch(msg);    
  }                           
    
////////////////////////////////////////////////////////////////
// Session Management
////////////////////////////////////////////////////////////////

  /**
   * Create new session and add to session tables.
   */
  DaspSession alloc(DaspSocketInterface iface, InetAddress host, int port, boolean isClient, Hashtable options)
    throws Exception
  {         
    synchronized (sessions)
    {                      
      // if busy
      if (sessions.size() >= MAX_SESSIONS_VAL)
        throw new Exception("busy - too many sessions");
      
      // pick random key until we find free one
      Integer id = null;
      while (true)
      {
        int r = rand.nextInt() & 0xffff;
        if (r == 0xffff) continue;
        if (sessions.get(id = new Integer(r)) == null) break;
      }       
      
      // create new session
      DaspSession s = createDaspSession(iface, id.intValue(), host, port, isClient, options);
      if (s.id != id.intValue()) throw new IllegalStateException("session not created with required id: " + id);
      
      // put into tables
      sessions.put(id, s);
      
      return s;
    }
  }
  
  protected DaspSession createDaspSession(DaspSocketInterface iface, int id, InetAddress host, int port, boolean isClient, Hashtable options)
  {
    return new DaspSession(iface, id, host, port, isClient, options);
  }
  
  /**
   * Remove from session tables.
   */
  void free(DaspSession s)
  {
    synchronized (sessions)
    {                     
      sessions.remove(new Integer(s.id));
    }
  }


////////////////////////////////////////////////////////////////
// IO Hooks
////////////////////////////////////////////////////////////////
  
  /**
   * All sends route thru here
   */                         
  void send(DaspSession session, DaspMessage msg)
  {
    if (traceSend || session.traceSend) trace("send", msg);   
    session.iface.send(session, msg);    
    session.numSent++;
    session.iface.numSent++;
  }

  /**
   * All receives route thru here
   */                         
  void received(DaspSocketInterface iface, DaspSession session, DaspMessage msg)
  {
    if (traceReceive || (session != null && session.traceReceive)) trace("recv", msg);
    iface.numReceived++;
  }                                 
  
  /**
   * Trace a message received or sent.
   */
  void trace(String mode, DaspMessage msg)
  {                 
    System.out.print("-- " + mode + " s=" + Integer.toHexString(msg.sessionId) + 
                     " seq=" + Integer.toHexString(msg.seqNum));

    if (msg.ack >= 0)                  
    {
      System.out.print(" ackNum=" + Integer.toHexString(msg.ack));
      if (msg.ackMore != null)
      System.out.print(" ackMore=" + new Buf(msg.ackMore));
    }
    
    if (msg.msgType == DaspConst.DATAGRAM)
    {                  
      System.out.print(" cmd=" + (char)msg.payload[0] + " reply=" + msg.payload[1] + " ");
      System.out.print(new Buf(msg.payload).toString());
    }                          
    else
    {
      System.out.print(" msgType=" + msg.msgType);
    }
    System.out.println();
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final int SOCKET_QUEUING  = 13;
  public static final int SESSION_QUEUING = 14;

  public boolean traceSend;         // debug option
  public boolean traceReceive;      // debug option
  DaspAcceptor acceptor;            // for server side authentication  
  volatile boolean isAlive;         // to keep receiver alive
  DaspSocketInterface[] interfaces; // network interfaces
  Object interfacesLock;            // lock for accessing interfaces
  HashMap sessions;                 // Integer -> DaspSession  
  Random rand;                      // randomizer
  int qMode;                        // session or socket queueing mode
  ReceiveQueue queue;               // used for socket queuing

}
