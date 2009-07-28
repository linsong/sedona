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
  public Bundle[] bundles = new Bundle[]
  { 
    new sedonacert.sox.Sox(this),
    new sedonacert.prov.Prov(this),
  };
  
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
   * Initialize the test environment.
   */
  public boolean init()          
    throws Exception
  {                 
    testDir = new File("sedona-cert");
    FileUtil.delete(testDir, log);        
    FileUtil.mkdir(testDir, log);
    return true;
  }
  
  /**
   * Connect with sox and gather test meta-data.
   */                 
  public boolean connect()                         
    throws Exception
  {            
    log.info("Connecting to " + host + ":1876 as " + username + ":" + password + "...");
    try
    {
      DaspSocket sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);
      sox = new SoxClient(sock, InetAddress.getByName(host), 1876, username, password);
      sox.connect();             
      schema  = sox.readSchema();
      version = sox.readVersion();
      log.info("Connected.");
      return true;
    }
    catch (Exception e)
    {     
      log.error("Cannot connect via sox", e);        
      return false;
    }
  }

  /**
   * Connect from sox.
   */                 
  public void disconnect()
  {
    sox.close();
    log.info("Disconnected.");
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
