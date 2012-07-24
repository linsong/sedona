//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   2 Apr 08  Brian Frank  Creation
//

package sedona.dasp;      

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * DefaultDaspSocketInterface uses the standard Java 
 *  DatagramSocket and the built-in TCP/IP stack.
 */
public final class DefaultDaspSocketInterface extends DaspSocketInterface  
{                                      
  public DefaultDaspSocketInterface(int port)
    throws IOException
  {
    this.sock = port < 0 ? new DatagramSocket() : new DatagramSocket(port);
  }
  
  public boolean routes(InetAddress addr, int port)
  {
    return true;
  }                                          

  public void send(DatagramPacket p) throws IOException 
  {
    sock.send(p);
  }

  protected void receive(DatagramPacket p) throws IOException 
  {
    sock.receive(p);
  }

  public void close() throws IOException 
  {
    sock.close(); 
  }

  final DatagramSocket sock;   
}

