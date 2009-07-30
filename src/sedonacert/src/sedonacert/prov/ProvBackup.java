//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Jun 09  Brian Frank  Creation
//

package sedonacert.prov;

import java.io.*;
import java.util.*;
import sedonacert.*;
import sedona.*;
import sedona.offline.*;
import sedona.sox.*;

/**
 * Test that we can add backup the app.sab file.
 */
public class ProvBackup extends Test
{ 
  public ProvBackup() { super("backup"); }
  
  public void run()
    throws Exception
  {                              
    SoxClient c = runner.sox;           
    
    // transfer app.sab file to local file
    File file = new File(runner.testDir, "app.sab");
    Properties props = c.getFile("app.sab", SoxFile.make(file), null, null);
    
    // verify we can open                                             
    OfflineApp app = OfflineApp.decodeAppBinary(file);                
    
    // verify schema's match
    verifyEq(app.schema.key, c.readSchema().key);
  }
  
}
