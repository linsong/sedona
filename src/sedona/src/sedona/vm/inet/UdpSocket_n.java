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
 * inet::UdpSocket native methods
 *
 * Note: we specify the @javaPeer facet on UdpSocket which
 *  generates a public field called "peer" to store the
 *  java socket instance.
 */
public class UdpSocket_n   
  extends ReflectUtil
{                  
           
////////////////////////////////////////////////////////////////
// Natives
////////////////////////////////////////////////////////////////
          
  public static int maxPacketSize(Context cx) 
  { 
    return 512; 
  }

  public static int idealPacketSize(Context cx) 
  { 
    return 512; 
  }

  public static byte open(Object self, Context cx) 
  {
    try
    {                                       
      DatagramChannel chan = DatagramChannel.open();
      chan.configureBlocking(false);
      set(self, "peer", chan);
      set(self, "closed", new Byte((byte)0));
      return 1;
    }
    catch (Exception e)
    {                       
      System.out.println("UdpSocket_n.open: " + e); 
      e.printStackTrace();    
      return 0;
    }
  }

  public static byte bind(Object self, int port, Context cx)
  {
    try
    {                
      DatagramChannel chan = toChannel(self);
      chan.socket().bind(new InetSocketAddress(port));
      return 1;
    }
    catch (Exception e)
    {
      System.out.println("UdpSocket_n.bind: " + e); 
      e.printStackTrace();    
      return 0;
    }
  }

  public static byte send(Object self, Object datagram, Context cx)
  {
    try
    {                
      DatagramChannel chan = toChannel(self);    
      if (getb(self, "closed")) return 0;  
      
      // extract data from Sedona datagram
      byte[] buf = (byte[])get(datagram, "buf");
      int len  = geti(datagram, "len");
      int off  = geti(datagram, "off");
      int port = geti(datagram, "port");        
      
      // map to Java addr and byte buffer
      // TODO: can we do this without allocation?
      SocketAddress addr = new InetSocketAddress("localhost", port);
      ByteBuffer byteBuf = ByteBuffer.wrap(buf, off, len);

      // send the packet on its way
      int sent = chan.send(byteBuf, addr);

      // return false if we didn't send as many as we wanted
      if (len == sent) return 1;
      else return 0;
    }
    catch (Exception e)
    {
      System.out.println("UdpSocket_n.send: " + e); 
      e.printStackTrace();    
      return 0;
    }
  }

  public static byte receive(Object self, Object datagram, Context cx)
  {
    try
    {
      DatagramChannel chan = toChannel(self);
      if (getb(self, "closed")) return 0;  
      
      // receive the packet
      ByteBuffer byteBuf = ByteBuffer.allocate(512);      
      InetSocketAddress addr = (InetSocketAddress)chan.receive(byteBuf);
      
      // if no datagram ready to receive return false
      if (addr == null) 
      {
        set(datagram,  "addr", null);
        seti(datagram, "port", -1);
        seti(datagram, "len", 0);
        return 0;
      }                        
      
      // map Java buffer to Sedona datagram's buffer
      byte[] buf = (byte[])get(datagram, "buf");
      int off = geti(datagram, "off");
      int len = byteBuf.flip().remaining();
      byteBuf.get(buf, off, len);
      
      // map Java address to Sedona IpAddr
      Object receiveAddr = get(self, "receiveAddr");
      InetUtil.javaToIpAddr(addr.getAddress(), receiveAddr);
      
      // copy Java data into Sedona datagram
      seti(datagram, "port", addr.getPort());
      seti(datagram, "len",  len);
      set(datagram, "addr",  receiveAddr);
      
      // return true
      return 1;
    }
    catch (Exception e)
    {
      System.out.println("UdpSocket_n.receive: " + e); 
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
      System.out.println("UdpSocket_n.close: " + e); 
      e.printStackTrace();    
    }
  }                  

  static DatagramChannel toChannel(Object x)
    throws Exception
  {                
    return (DatagramChannel)get(x, "peer");
  }
  
}

