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
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * DaspSocketInterface is used to route specific InetAddresses
 * to an alternate socket implementation.
 */
public abstract class DaspSocketInterface
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public DaspSocketInterface()
  {
    this.sendPacket = new DatagramPacket(new byte[DaspConst.ABS_MAX_VAL], DaspConst.ABS_MAX_VAL);
  }

////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  /**
   * Get the associated dasp socket for this interface.
   */
  public final DaspSocket daspSocket()
  {
    return daspSocket;
  }

  /**
   * Return if this interface is responsible for
   * sending/receiving packets to the sepcified address.
   */
  public abstract boolean routes(InetAddress addr, int port);

  /**
   * Send the specified datagram packet.
   */
  public abstract void send(DatagramPacket p)
    throws IOException;

  /**
   * Receive the specified datagram packet.  This method
   * should block until a packet has been read.
   */
  protected abstract void receive(DatagramPacket p)
    throws IOException;

  /**
   * Close down this socket interface.
   */
  public abstract void close()
    throws IOException;

////////////////////////////////////////////////////////////////
// Lifecycle
////////////////////////////////////////////////////////////////

  /**
   * Start interface - called by DaspSocket.addInterface
   */
  public void start(DaspSocket s)
  {
    daspSocket = s;

    receiver = new Receiver();
    receiver.start();

    houseKeeping = new HouseKeeping();
    houseKeeping.start();
  }

  /**
   * Stop and close down interface - called by DaspSocket.close
   */
  public void stop()
  {
    // kill background threads
    try { receiver.interrupt(); receiver = null; } catch (Exception e) {}
    try { houseKeeping.interrupt(); houseKeeping = null; } catch (Exception e) {}

    // give subclass chance to cleanup
    try { close(); } catch (Exception e) {}
  }

////////////////////////////////////////////////////////////////
// Debug
////////////////////////////////////////////////////////////////

  public int numSent() { return numSent; }
  public int numReceived() { return numReceived; }
  public int numRetries() { return numRetries; }

////////////////////////////////////////////////////////////////
// Send
////////////////////////////////////////////////////////////////

  /**
   * DaspSessions route here to send a packet.
   */
  void send(DaspSession session, DaspMsg msg)
  {
    synchronized (sendPacket)
    {
      try
      {
        sendPacket.setAddress(session.host);
        sendPacket.setPort(session.port);
        sendPacket.setLength(msg.encode(sendPacket.getData()));
        send(sendPacket);
      }
      catch (IOException e)
      {
        if (daspSocket.traceSend)
        {
          System.out.println("ERROR: DaspSocket error on send - " + e.getMessage());
          e.printStackTrace();
        }
        session.shutdown(e.getMessage());
      }
    }
  }

////////////////////////////////////////////////////////////////
// Receiver
////////////////////////////////////////////////////////////////

  /**
   * This is a background thread which reads packets from
   * the UPD socket and dispatches them for processing.
   */
  class Receiver extends Thread
  {
    Receiver() { super("DaspSocketInterface.Receiver"); }

    public void run()
    {
      // reusable datagram
      byte[] buf = new byte[DaspConst.ABS_MAX_VAL];
      DatagramPacket packet = new DatagramPacket(buf, buf.length);

      // loop forever receiving messages
      while (daspSocket.isAlive)
      {
        try
        {
          // receive packet
          packet.setLength(buf.length);
          receive(packet);

          // dispatch
          daspSocket.dispatch(DaspSocketInterface.this, packet);
        }
        catch (SocketTimeoutException e)
        {
        }
        catch (Throwable e)
        {
          if (daspSocket.isAlive) e.printStackTrace();
        }
      }
    }
  }

////////////////////////////////////////////////////////////////
// HouseKeeping
////////////////////////////////////////////////////////////////

  /**
   * This is a background thread which periodically walks all
   * the sessions and gives them a chance to do house keeping
   * chores such as retries, keep-alives, and timeouts.  We
   * try to run housekeeping every 100ms.
   */
  class HouseKeeping extends Thread
  {
    HouseKeeping() { super("DaspSocket.HouseKeeping"); }

    public void run()
    {
      // loop forever receiving messages
      while (daspSocket.isAlive)
      {
        try
        {
          long t1 = System.currentTimeMillis();

          // call house keeping callback on each session
          DaspSession[] sessions = daspSocket.sessions();
          for (int i=0; i<sessions.length; ++i)
          {
            try
            {
              DaspSession session = sessions[i];
              if (session.iface == DaspSocketInterface.this)
                sessions[i].houseKeeping();
            }
            catch (Throwable e)
            {
              if (daspSocket.isAlive) e.printStackTrace();
            }
          }

          // attempt to run every 100ms
          long t2 = System.currentTimeMillis();
          long snooze = 100 - (t2-t1);
          if (snooze > 5) Thread.sleep(snooze);
        }
        catch (Throwable e)
        {
          if (daspSocket.isAlive) e.printStackTrace();
        }
      }
    }
  }

////////////////////////////////////////////////////////////////
// Fields
///////////////////////////////////////////////////////////////

  Receiver receiver;            // receiver thread
  HouseKeeping houseKeeping;    // house keeping thread
  DaspSocket daspSocket;        // set by DaspSocket
  DatagramPacket sendPacket;    // reusable packet for sends
  int numSent;
  int numReceived;
  int numRetries;
}

