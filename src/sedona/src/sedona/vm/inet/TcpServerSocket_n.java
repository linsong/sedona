//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Dec 08  Brian Frank  Creation
//                     

package sedona.vm.inet;

import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import sedona.vm.*;

/**
 * inet::TcpServerSocket native methods
 *
 * Note: we specify the @javaPeer facet on TcpServerSocket 
 *  which generates a public field called "peer" to store the
 *  java socket instance.
 */
public class TcpServerSocket_n   
  extends ReflectUtil
{                  
           
////////////////////////////////////////////////////////////////
// Natives
////////////////////////////////////////////////////////////////

  public static byte bind(Object self, int port, Context cx)
  {
    try
    {                
      ServerSocketChannel chan = ServerSocketChannel.open();
      chan.configureBlocking(false);
      chan.socket().bind(new InetSocketAddress(port));
      set(self, "peer", chan);
      set(self, "closed", new Byte((byte)0));
      return 1;
    }
    catch (Exception e)
    {
      System.out.println("TcpServerSocket_n.bind: " + e); 
      e.printStackTrace();    
      return 0;
    }
  }

  public static byte accept(Object self, Object socket, Context cx)
  {
    try
    {                 
      SocketChannel sc = toChannel(self).accept();
      if (sc == null) return 0;
      
      sc.configureBlocking(false);
      set(socket, "peer", sc);      
      set(socket, "closed", new Byte((byte)0));
      return 1;
    }
    catch (Exception e)
    {
      System.out.println("TcpServerSocket_n.close: " + e); 
      e.printStackTrace(); 
      return 0;   
    }
  }

  public static void close(Object self, Context cx)
  {
    try
    {                 
      toChannel(self).close();
      set(self, "peer", null);
      set(self, "closed", new Byte((byte)1));
    }
    catch (Exception e)
    {
      System.out.println("TcpServerSocket_n.close: " + e); 
      e.printStackTrace();    
    }
  }                  

  static ServerSocketChannel toChannel(Object x)
    throws Exception
  {                
    return (ServerSocketChannel)get(x, "peer");
  }
  
}

