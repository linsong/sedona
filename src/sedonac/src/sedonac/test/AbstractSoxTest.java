//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Jun 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.net.*;
import java.util.*;
import sedona.*;
import sedona.Byte;
import sedona.Short;
import sedona.Long;
import sedona.Float;
import sedona.Double;
import sedona.offline.*;
import sedona.dasp.*;
import sedona.sox.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.Compiler;

/**
 * SoxTest
 */
public abstract class AbstractSoxTest
  extends Test
{

  public void test()
    throws Exception
  {
    try
    {
      startServer();
      doTest();
    }
    finally
    {
      Component.testMode = false; 
      stopClient();
      stopServer();
    }
  }

  public abstract void doTest()
    throws Exception;

//////////////////////////////////////////////////////////////////////////
// Client
//////////////////////////////////////////////////////////////////////////

  private void stopClient()
    throws Exception
  {                      
    try
    {
      client.close();
    }
    catch (Exception e)
    {
    }
  }

//////////////////////////////////////////////////////////////////////////
// Server App
//////////////////////////////////////////////////////////////////////////

  private void startServer()
    throws Exception
  {                          
    sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);
    OfflineApp app = buildApp();
    File sab = writeAppFile(app);
    File scode = writeScodeFile(app.schema);
    spawnApp(scode, sab);
  }

  public abstract OfflineApp buildApp()
    throws Exception;
    
  private File writeAppFile(OfflineApp app)
    throws Exception
  {
    File file = new File(testDir(), "app.sab");
    Buf buf = app.encodeAppBinary();
    buf.flip();
    buf.writeTo(file);
    return file;
  }

  private File writeScodeFile(Schema schema)
    throws Exception
  {
    // write build file
    File file = new File(testDir(), "kits.xml");
    XWriter out = new XWriter(file);
    out.write("<sedonaCode endian='little' blockSize='4'\n");
    out.write("   refSize='4' main='sys::Sys.main' debug='true' test='true'>\n");
    for (int i=0; i<schema.kits.length; ++i)
      out.write("<depend on='" + schema.kits[i].name + " 0+'/>\n");
    out.write("</sedonaCode>\n");
    out.close();

    // compile build file
    System.out.print("-- Compile " + file);
    Compiler c = new Compiler();
    c.compile(file);
    return new File(testDir(), "kits.scode");
  }

  private void spawnApp(File scode, File sab)
    throws Exception
  { 
    //
    // First check to make sure an 'svm.exe' isn't already running;
    // abort if it is!  (No way to select which one to connect to.)
    //
    Process tlist = Runtime.getRuntime().exec("tasklist");   // Windows only!
    BufferedReader trdr = new BufferedReader(new InputStreamReader(tlist.getInputStream()));
    String line;
    while ((line = trdr.readLine()) != null)
      if (line.indexOf(getSvmName())>=0)
        throw new TestException("Cannot continue; " + getSvmName() + " already running!");


    // copy svm.exe to tempDir (so we mimic a live installation)
    File exe = new File(Env.home, "bin"+File.separator+getSvmName());
    FileUtil.copyFile(exe, new File(testDir(), exe.getName()));                       
    
    String cmd = exe.getName() + " kits.scode app.sab";
    System.out.println("###### " + cmd + " ######");    
    proc = Runtime.getRuntime().exec(cmd, null, testDir());
    new Thread("ProcOut")
    {
      public void run()
      {
        try
        {
          BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String line;
          while ((line = in.readLine()) != null)
            System.out.println("[s] " + line);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }.start();
  }

  private void stopServer()
    throws Exception
  {               
    sock.close();
    if (proc != null)
    {
      proc.destroy();
      proc.waitFor();
    }
  }

////////////////////////////////////////////////////////////////
// Simple Reads/Invokes
////////////////////////////////////////////////////////////////

  public Value read(OfflineComponent comp, String slotName)
    throws Exception
  {
    return client.readProp(comp.id(), comp.type.slot(slotName, true));
  }

  public boolean readBool(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((Bool)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public int readByte(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((sedona.Byte)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public int readShort(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((sedona.Short)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public int readInt(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((Int)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public long readLong(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((Long)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public float readFloat(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((sedona.Float)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public double readDouble(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((sedona.Double)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public Buf readBuf(OfflineComponent comp, String slotName)
    throws Exception
  {
    return (Buf)client.readProp(comp.id(), comp.type.slot(slotName, true));
  }

  public String readStr(OfflineComponent comp, String slotName)
    throws Exception
  {
    return ((Str)client.readProp(comp.id(), comp.type.slot(slotName, true))).val;
  }

  public void write(OfflineComponent comp, String slotName, int val)
    throws Exception
  {
    client.write(comp.id(), comp.type.slot(slotName, true), Int.make(val));
  }

  public void invoke(OfflineComponent comp, String slotName, Value arg)
    throws Exception
  {
    client.invoke(comp.id(), comp.type.slot(slotName, true), arg);
  }

  public void invoke(OfflineComponent comp, String slotName, int arg)
    throws Exception
  {
    invoke(comp, slotName, Int.make(arg));
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

  OfflineComponent addAdmin(OfflineComponent users, String name, String pass)
  {                                  
    Schema schema = users.type.schema;
    OfflineComponent u = users.app().add(users, new OfflineComponent(schema.type("sys::User"), name));
    u.setBuf("cred", new Buf(UserUtil.credentials(name, pass)));  
    u.setInt("perm", 0xffffffff);  
    u.setInt("prov", 0xff);     
    return u;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  static final int TREE    = SoxComponent.TREE;
  static final int CONFIG  = SoxComponent.CONFIG;
  static final int RUNTIME = SoxComponent.RUNTIME;
  static final int LINKS   = SoxComponent.LINKS;

  Process proc;
  DaspSocket sock;
  Schema schema;
  SoxClient client;
  
}
