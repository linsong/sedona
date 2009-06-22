//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert.sox;

import sedonacert.*;
import sedona.*;
import sedona.sox.*;
import sedona.dasp.*;

/**
 * Tests basic sox connectivity and login tests.
 */
public class SoxLogin extends Test
{ 
  public SoxLogin() { super("login"); }
  
  public void run()
    throws Exception
  {           
    SoxClient c;
    
    // try valid username, bad password                                       
    c = new SoxClient(runner.sox.socket, runner.sox.addr, runner.sox.port, runner.username, "__bad__");
    try
    {                                 
      c.connect();
      fail("Login with bad password");
    }
    catch (DaspException e) { verify(true); }

    // try bad username
    c = new SoxClient(runner.sox.socket, runner.sox.addr, runner.sox.port, "__bad__", "");
    try
    {                                 
      c.connect();
      fail("Login with bad user");
    }
    catch (DaspException e) { verify(true); }
            
  }
}
