//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert;

import java.io.*;
import java.net.*;
import sedona.*;
import sedona.dasp.*;
import sedona.platform.*;
import sedona.sox.*;
import sedona.util.*;

/**
 * Runner is used to provide context for a run of certification tests.
 */
public class Runner
{            

//////////////////////////////////////////////////////////////////////////  
// Test Configuration
//////////////////////////////////////////////////////////////////////////  

  /**
   * Logging.
   */
  public Log log = new Log("cert");

  /**
   * IP hostname to test against.
   */
  public String host;
  
  /**
   * Sox port to connect to.
   */
  public int port = 1876;
  
  /**
   * Admin user name to use for sox authentication.
   */
  public String username;

  /**
   * Admin password to use for sox authentication.
   */
  public String password;
    
  /**
   * All the bundles to test.
   */
  public Bundle[] bundles; 
  
  /**
   * Sox connection maintained during test run.
   */
  public SoxClient sox;

  /**
   * Schema of kits installed on device.
   */
  public Schema schema;
  
  /**
   * Version of kits installed on device.
   */
  public VersionInfo version;
  
  /**
   * Platform manifest for svm running on the device.
   */
  public PlatformManifest platform;

  /**
   * Test scratch directory.
   */
  public File testDir;
  
//////////////////////////////////////////////////////////////////////////  
// Execution
//////////////////////////////////////////////////////////////////////////  
  
  /**
   * Dump test report.
   */                 
  public int run()
    throws Exception
  { 
    if (!init()) return 1;                  
    if (!connect()) return 1;
    for (int i=0; i<bundles.length; ++i)
    {                            
      Bundle b = bundles[i];
      log.info("---" + b.name + "---"); 
      for (int j=0; j<b.tests.length; ++j)              
      {
        Test t = b.tests[j];
        try
        {          
          log.info(t.qname + " ...");     
          t.run();
          log.info(t.qname + ": pass [" + t.verifies + " verifies]");     
          t.status = Test.PASS;
        }
        catch (Throwable e)
        {                  
          log.error(t.qname + ": fail", e);
          t.status = Test.FAIL;
          t.failure = e;
        }
      }
    }
    disconnect();       
    report();
    return 0;
  }      
  
  /**
   * Initialize the test environment and all test bundles.
   */
  public boolean init()          
    throws Exception
  {                 
    testDir = new File("sedona-cert");
    FileUtil.delete(testDir, log);        
    FileUtil.mkdir(testDir, log);
    
    // set the test bundles to run
    bundles = new Bundle[] {
                 new sedonacert.sox.Sox(this),
                 new sedonacert.prov.Prov(this),
    };
    
    return true;
  }
  
  /**
   * Connect with sox and gather test meta-data.
   */                 
  public boolean connect()                         
    throws Exception
  {            
    log.info("Connecting to " + host + ":" + port + " as " + username + ":" + password + "...");
    try
    {
      DaspSocket sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);
      sox = new SoxClient(sock, InetAddress.getByName(host), port, username, password);
      sox.connect();             
      schema  = sox.readSchema();
      version = sox.readVersion();
      log.info("Connected.");
    }
    catch (Exception e)
    {     
      log.error("Cannot connect via sox", e);        
      return false;
    }
    
    if ((platform = PlatformDb.db().loadExact(version.platformId)) == null)
    {
      log.error(version.platformId + " is not in the platform database.");
      return false;
    }
    
    return true;
  }

  /**
   * Connect from sox.
   */                 
  public void disconnect()
  {
    sox.close();
    sox = null;
    log.info("Disconnected.");
  }
  
  /**
   * Restart the device.
   */
  public boolean restart()
  {
    try
    {
      SoxComponent app = sox.loadApp();
      try
      {
        log.info("restart");
        sox.invoke(app, app.slot("restart"), null);
      }
      catch (SoxException e) { }
      catch (Exception e) { e.printStackTrace(); }
      disconnect();
      int attempts = 10;
      final int sleep = 1000;
      while (!connect())
      {
        if (--attempts == 0) return false;
        log.info("Try to connect again in " + sleep + " ms...");
        Thread.sleep(sleep);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  /**
   * Dump test report.
   */
  public void report()
  {                           
    // dump setup
    log.out.println();
    log.out.println("===Setup===");
    log.out.println("date:           " + Abstime.now());
    log.out.println("platformId:     " + version.platformId);
    log.out.println("scodeFlags:     0x" + Integer.toHexString(version.scodeFlags));
    log.out.println("sedona.version: " + Env.version);
    log.out.println("java.version:   " + System.getProperty("java.version"));
    
    // dump kits installed
    log.out.println();
    log.out.println("===Kits Installed===");
    for (int i=0; i<version.kits.length; ++i)
      log.out.println(version.kits[i]);
      
    // dump results
    log.out.println();
    log.out.println("===Results===");
    int totalNotRun = 0, totalPass = 0, totalFail = 0;
    for (int i=0; i<bundles.length; ++i)
    {                            
      Bundle b = bundles[i];
      log.out.println("---" + b.name + "---"); 
      for (int j=0; j<b.tests.length; ++j)              
      {
        Test t = b.tests[j];
        String status;
        switch (t.status)
        {
          case Test.NOTRUN: ++totalNotRun; status = "not-run"; break;
          case Test.PASS:   ++totalPass;   status = "pass [" + t.verifies + " verifies]"; break;
          case Test.FAIL:   ++totalFail;   status = "fail"; break;
          default: throw new IllegalStateException();
        }
        log.out.println(t.qname + ": " + status); 
      }
    }
    
    // dump results
    log.out.println();
    log.out.println("===Summary===");
    log.out.println("Total Not-Run: " + totalNotRun);
    log.out.println("Total Passes:  " + totalPass);
    log.out.println("Total Fail:    " + totalFail);
    log.out.println();
    log.out.println("**");
    log.out.println("** Certification " + (totalFail > 0 ? "Failed!" : "Passed!"));
    log.out.println("**");
  }
  
  
}
