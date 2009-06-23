//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Nov 08  Brian Frank  Creation
//                     

package sedona.vm.inet;

import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import sedona.vm.*;

/**
 * inet::TcpSocket native methods
 *
 * Note: we specify the @javaPeer facet on TcpSocket which
 *  generates a public field called "peer" to store the
 *  java socket instance.
 */
public class TcpSocket_n   
  extends ReflectUtil
{                  
           
////////////////////////////////////////////////////////////////
// Natives
////////////////////////////////////////////////////////////////


  public static byte connect(Object self, Object addr, int port, Context cx) 
  {
    try
    {                                       
      SocketChannel chan = SocketChannel.open();
      chan.configureBlocking(false);
      chan.connect(new InetSocketAddress(InetUtil.javaFromIpAddr(addr), port));
      set(self, "peer", chan);
      return 1;
    }
    catch (Exception e)
    {                       
      System.out.println("UdpSocket_n.open: " + e); 
      e.printStackTrace();    
      return 0;
    }
  }

  public static byte finishConnect(Object self, Context cx)
  {
    try
    {                
      SocketChannel chan = toChannel(self);
      if (!chan.finishConnect()) return 0;
      set(self, "closed", new Byte((byte)0));
      return 1;
    }
    catch (Exception e)
    {
      System.out.println("TcpSocket_n.finishConnect: " + e); 
      close(self, cx);
      // e.printStackTrace();    
      return 1;
    }
  }

  public static int write(Object self, byte[] b , int off, int len, Context cx)
  {
    try
    {                      
      SocketChannel chan = toChannel(self);
      if (chan == null) return -1;
      ByteBuffer buf = ByteBuffer.wrap(b, off, len);
      return chan.write(buf);
    }
    catch (Exception e)
    {
      System.out.println("TcpSocket_n.write: " + e); 
      e.printStackTrace();    
      close(self, cx);
      return 0;
    }
  }

  public static int read(Object self, byte[] b , int off, int len, Context cx)
  {
    try
    {                
      SocketChannel chan = toChannel(self);
      if (chan == null) return -1;
      ByteBuffer buf = ByteBuffer.wrap(b, off, len);
      int n = chan.read(buf);
      if (n < 0) close(self, cx);
      return n;
    }
    catch (Exception e)
    {  
      System.out.println("TcpSocket_n.read: " + e); 
      e.printStackTrace();    
      close(self, cx);
      return 0;
    }
  }

  public static void close(Object self, Context cx)
  {
    try
    {                       
      SocketChannel chan = toChannel(self);
      if (chan != null) chan.close();
      set(self, "peer", null);
      set(self, "closed", new Byte((byte)1));   
    }
    catch (Exception e)
    {
      System.out.println("TcpSocket_n.close: " + e); 
      e.printStackTrace();    
    }
  }                  

  static SocketChannel toChannel(Object x)
    throws Exception
  {                
    return (SocketChannel)get(x, "peer");
  }
  
}

