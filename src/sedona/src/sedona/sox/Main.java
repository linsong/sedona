//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   8 Jan 08  Brian Frank  Creation
//

package sedona.sox;

import java.io.File;
import java.net.InetAddress;

import sedona.dasp.DaspSocket;
import sedona.sox.ISoxComm.TransferListener;

/**
 * Main command line for sox client stuff.
 */
public class Main
{ 

  public static void main(String[] args)
    throws Exception
  {            
    if (args.length < 4) { usage(); return; }
    
    String host = args[0];        
    int    port = 1876;
    String user = args[1];
    String pass = args[2];
    String cmd  = args[3].toLowerCase();             
    
    String[] cmdArgs = new String[args.length-4];
    System.arraycopy(args, 4, cmdArgs, 0, args.length-4);
    
    int colon = host.indexOf(':');
    if (colon > 0) 
    { 
      port = Integer.parseInt(host.substring(colon+1));
      host = host.substring(0, colon);
    }               

    DaspSocket sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);
    SoxClient c = new SoxClient(sock, InetAddress.getByName(host), port, user, pass);
    try
    {                 
      println("Connecting...");
      c.connect();             
      println("Connected.");
      command(c, cmd, cmdArgs);
    }
    catch (Exception e)
    { 
      println("Connection failed: " + e);
      e.printStackTrace();
    }           
    finally
    {
      try { c.close(); } catch (Exception x) {}
    }
  }
  
  public static void usage()
  {
    println();
    println("usage:");
    println("  soxclient <host>[:<port>] <user> <pass> <command>");
    println("commands:");
    println("  version     Print installed kit versions");
    println("  get <file>  Tranfer a file from the device");
    println("  put <file>  Tranfer a file to the device");
    println();
  }

  public static int command(SoxClient c, String cmd, String[] args)
    throws Exception
  {               
    if (cmd.equals("version"))  return version(c, args);
    if (cmd.equals("get"))      return get(c, args);
    if (cmd.equals("put"))      return put(c, args);
    println("Unknown command '" + cmd + "'");
    return 1;
  }

  public static int version(SoxClient c, String[] args)
    throws Exception
  {
    c.readVersion().dump();
    return 0;
  }

  public static int get(SoxClient c, String[] args)
    throws Exception
  {                    
    if (args.length == 0) { usage(); return 1; }
    println("TODO GET " + args[0]);
    return 0;
  }

  public static int put(SoxClient c, String[] args)
    throws Exception
  {                    
    if (args.length == 0) { usage(); return 1; } 
    
    File f = new File(args[0]);    
    if (!f.exists()) { println("File does not exist: " + f); return 1; }
    
    println("Put " + f + "...");
    
    c.putFile(f.getName(), SoxFile.make(f), null, null);
    
    println("Complete.");
    
    return 0;
  }

  public static void println() { System.out.println(); }
  public static void println(String s) { System.out.println(s); }
  
  static class Progress implements TransferListener
  {
    public void progress(int transfered, int total)
    {                                                  
      double percent = (double)transfered / (double)total * 100.0;
      System.out.println("  " + (int)percent + "% [" + transfered + " of " + total + "]");
    }
  }
           
}
