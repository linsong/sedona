//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Mar 08  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.net.*;
import java.util.*;
import sedona.*;
import sedona.dasp.*;
import sedona.util.*;

/**
 * DaspTest            
 */
public class DaspTest
  extends Test
{

  public void test()
    throws Exception
  {                               
    // signficant white-box testing is in the 
    // SendWindow and ReceiveWindow classes themselves. 
    System.out.println();
    DaspTestHooks.runWhiteboxTests();
             
    // run public API black-box tests         
    System.out.println();
    local = InetAddress.getLocalHost();
    try
    {
      startServer(DaspSocket.SESSION_QUEUING);
      verifyConnect();
      verifyDatagrams();
      verifyClose();
      
      startServer(DaspSocket.SOCKET_QUEUING);
      verifyConnect();
      verifyDatagrams();
      verifyClose();
    }
    finally
    {
      stopServer();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Connect
//////////////////////////////////////////////////////////////////////////

  private void verifyConnect()
    throws Exception
  {
    // connect to bad address   
    DaspException ex = null;
    /*
    trace("Connect to bad address...");
    try { socket.connect(local, 3333, "x", "y"); } catch (DaspException e) { ex = e; }
    verifyEq(ex.getMessage(), "No response from hello");
    */
    
    // connect with bad username
    trace("Connect with bad username...");
    user = "bob";
    pass = "secret";
    ex = null;
    try { socket.connect(local, socket.port(), "x", "y"); } catch (DaspException e) { ex = e; }
    verifyEq(ex.errorCode, DaspConst.NOT_AUTHENTICATED);

    // connect with bad password
    trace("Connect with bad password...");
    ex = null;
    try { socket.connect(local, socket.port(), "bob", "bad"); } catch (DaspException e) { ex = e; }
    verifyEq(ex.errorCode, DaspConst.NOT_AUTHENTICATED);  

    Hashtable options = new Hashtable();
    options.put("dasp.idealMax",       "100");
    options.put("dasp.absMax",         "500");
    options.put("dasp.receiveTimeout", "45000");
    options.put("dasp.test", new TestHooks());

    // connect
    trace("Connect...");
    client = socket.connect(local, socket.port(), "bob", "secret", options);
    verify(!client.isClosed());
    verify(0 < client.id && client.id < 0xffff); 
    verifyEq(client.host, local);    
    verifyEq(client.port, socket.port());    
    
    // server
    server = socket.session(client.remoteId());
    verify(server != null);    
    verifyEq(server.remoteId(), client.id);    
    verifyEq(server.host, local);    
    verifyEq(server.port, socket.port());    
    
    // verify client tuning
    verifyEq(client.idealMax(),         100);
    verifyEq(client.absMax(),           500);
    verifyEq(client.localReceiveMax(),  31);
    verifyEq(client.remoteReceiveMax(), 31);
    verifyEq(client.receiveTimeout(),   45000);
    
    // verify server tuning
    verifyEq(server.idealMax(),         100);
    verifyEq(server.absMax(),           500);
    verifyEq(server.localReceiveMax(),  31);
    verifyEq(server.remoteReceiveMax(), 31);
    verifyEq(server.receiveTimeout(),   45000);
  }

//////////////////////////////////////////////////////////////////////////
// Datagrams
//////////////////////////////////////////////////////////////////////////

  private void verifyDatagrams()
    throws Exception
  {   
    // just try some straight messages                                   
    for (int i=0; i<23; ++i)
    {
      byte[] msg = ("msg " + i).getBytes("UTF-8"); 
      client.send(msg);                    
      verifyEq(receive(100), msg); 
    }                         
    
    // send some messages with drops
    ArrayList toSend = new ArrayList();
    for (int i=0; i<23; ++i) toSend.add("msg " + i);
    send((String[])toSend.toArray(new String[toSend.size()]));
    
    // wait to receive them all with retries
    while (toSend.size() > 0)
    {
      byte[] msg = receive(10000); 
      verify(msg != null);
      String s = new String(msg, "UTF-8");
      verify(toSend.contains(s));
      toSend.remove(s);
    }             
    verify(true);            
  }                    
  
  private byte[] receive(long timeout)
    throws Exception
  {   
    DaspMessage msg;                        
    if (socket.queuingMode() == DaspSocket.SESSION_QUEUING)
    {
      msg = server.receive(timeout);
    }
    else
    {
      msg = socket.receive(timeout);
    }       
    verify(msg.session() == server);
    return msg.payload();
  }
  
  private void send(final String[] toSend)
  {
    Thread t = new Thread("toSend async") 
    {
      public void run()
      {                                                          
        try
        {
          TestHooks ctest = (TestHooks)client.test; 
          for (int i=0; i<toSend.length; ++i)
          {                    
            if (i % 3 == 0) ctest.addDrop(ctest.sendSeqNum());
            client.send(toSend[i].getBytes("UTF-8"));
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
          fail();
        }
      }
    };
    t.start();
  }            

//////////////////////////////////////////////////////////////////////////
// Close
//////////////////////////////////////////////////////////////////////////

  private void verifyClose()
    throws Exception
  {
    trace("Close...");
    
    verify(!client.isClosed());
    verify(!server.isClosed());
    
    client.close();           
    
    verify(client.isClosed());    
    pause();
    verify(server.isClosed());
  }

//////////////////////////////////////////////////////////////////////////
// Server App
//////////////////////////////////////////////////////////////////////////

  void startServer(int qMode)
    throws Exception
  {                  
    socket = DaspSocket.open(-1, new DaspAcceptor() 
    {  
      public byte[] credentials(String u)
      {                                
        if (!u.equals(user)) return null;
        return UserUtil.credentials(user, pass);
      }                                      
      
      public Hashtable options()
      {                       
        Hashtable options = new Hashtable();    
        options.put("dasp.test", new TestHooks());
        return options;
      }            
    }, qMode);                 
    
    verifyEq(socket.isClosed(), false);
    verifyEq(socket.sessions().length, 0);
    verifyEq(socket.queuingMode(), qMode);
  }

  void stopServer()
    throws Exception
  {      
    socket.close();
    
    verifyEq(socket.isClosed(), true);
    verifyEq(socket.sessions().length, 0);
    
    Exception ex = null;
    try { socket.connect(local, 99, "x", "y"); } catch (IllegalStateException e) { ex = e; }
    verifyEq(ex.getMessage(), "socket is closed");
  }


//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public void trace(String msg)
  {
    System.out.println("[>] " + msg);
  }

  public void pause()
    throws Exception
  {
    Thread.sleep(100);
  }

//////////////////////////////////////////////////////////////////////////
// TestHooks
//////////////////////////////////////////////////////////////////////////

  static class TestHooks extends DaspTestHooks
  {              
    TestHooks() 
    { 
      clearDrop();
    }
    
    public void clearDrop() { for (int i=0; i<drop.length; ++i) drop[i] = -1; }
    
    public void addDrop(int seqNum)
    {                              
      for (int i=0; i<drop.length; ++i)
        if (drop[i] == -1) { drop[i] = seqNum; return; }
      throw new IllegalStateException("drop list full!");
    }             
    
    public boolean receive(int msgType, int seqNum, byte[] msg)
    {
      //System.out.println("  receive " + seqNum + " " + msgStr(msg));
      return true;
    }
    
    public boolean send(int msgType, int seqNum, byte[] msg)
    {                 
      if (msgType == dropType)
      {
        System.out.println("  DROPPING msgType=" + msgType + " " + msgStr(msg));
        dropType = -1;
        return false;
      }
                 
      for (int i=0; i<drop.length; ++i)
        if (drop[i] == seqNum) 
        { 
          drop[i] = -1;                                           
          System.out.println("  DROPPING " + seqNum + " " + msgStr(msg));
          return false; 
        }
      //System.out.println("  sending " + seqNum + " " + msgStr(msg));
      return true;
    }                                   
    
    String msgStr(byte[] msg)
    {
      try
      {
        Buf buf = new Buf(msg);           
        String s = (char)buf.u1() + " " + buf.u1();
        if (buf.bytes[0] == 'k') s += " " + buf.u2() + " " + buf.u2();
        return s;
      }
      catch (Exception e)
      {
        return "";
      }
    }
    
    int dropType = -1;
    int[] drop = new int[32];
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  DaspSocket socket;   // socket
  DaspSession client;  // client
  DaspSession server;  // server
  String user;         // for acceptor 
  String pass;         // for acceptor                               
  InetAddress local;   // localhost                                       
  TestHooks clientHooks;
  TestHooks serverHooks;

}
