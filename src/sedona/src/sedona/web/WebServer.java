//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Apr 09  Brian Frank  Creation
//

package sedona.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * WebServer implements a simple HTTP server.
 *
 * NOTE: this code uses a simplistic I/O and threading modeling
 *  which is not suitable for production systems.  This class is
 *  intended for simple "out-of-the-box" testing and development.
 */
public class WebServer
{

////////////////////////////////////////////////////////////////
// Configuration
////////////////////////////////////////////////////////////////

  /** Well-known HTTP port */
  public int port = 80;

////////////////////////////////////////////////////////////////
// Lifecycle
////////////////////////////////////////////////////////////////

  /**
   * Start the web server on its well-known port.
   */
  public void start()
  {
    alive = true;
    thread = new Thread(new Runnable() { public void run() { loop(); } });
    thread.start();
  }

  /**
   * Stop the web server on its well-known port.
   */
  public void stop()
  {
    alive = false;
    thread.interrupt();
    thread = null;
  }

  /**
   * Main loop.
   */
  void loop()
  {
    ServerSocket ss = null;
    try {ss = new ServerSocket(port); }
    catch (Exception e)
    {
      System.out.println("WebServer: cannot open port " + port + ": " + e);
      return;
    }
    System.out.println("WebServer: opened on port " + port);

    while (alive)
    {
      try
      {
        final Socket s = ss.accept();
        new Thread(new Runnable() { public void run() { process(s); } }).start();
      }
      catch (Throwable e)
      {
        if (alive) e.printStackTrace();
      }
    }
  }

  /**
   * Process a server request.
   */
  void process(Socket s)
  {
    InputStream in = null;
    OutputStream out = null;
    try
    {
      // wrap raw socket streams with buffers for performance
      in = new BufferedInputStream(s.getInputStream());
      out = new BufferedOutputStream(s.getOutputStream());

      // read request
      WebReq req = new WebReq();
      req.readText(in);

      // write response
      WebRes res = new WebRes();
      res.set("Content-Type", "text/plain");
      res.writeText(out);
      WebMsg.writeLine(out, "Here was original request:");
      req.writeText(out);  // use request as content body
    }
    catch (Throwable e)
    {
      if (alive) e.printStackTrace();
    }
    finally
    {
      try { s.close(); } catch (Throwable e) {}
      try { in.close(); } catch (Throwable e) {}
      try { out.close(); } catch (Throwable e) {}
    }
  }

////////////////////////////////////////////////////////////////
// Main
////////////////////////////////////////////////////////////////

  public static void main(String[] args) throws Exception
  {
    new WebServer().start();
    Thread.sleep(Long.MAX_VALUE);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  volatile boolean alive = true;
  volatile Thread thread;

}
