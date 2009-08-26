//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Jun 09  Brian Frank  Creation
//

package sedonacert.sox;

import sedonacert.*;
import sedona.*;
import sedona.sox.*;
import sedona.dasp.*;

/**
 * Test that we can use the query request.
 */
public class SoxQuery extends Test
{ 
  public SoxQuery() { super("query"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();

    // sox service                                                                 
    int[] ids = c.queryService(schema.type("sox::SoxService"));
    verifyEq(ids.length, 1);
    int sox = ids[0];
    verifyEq(c.load(sox).type.qname, "sox::SoxService");
    
    // user service
    ids = c.queryService(schema.type("sys::UserService"));
    verifyEq(ids.length, 1);
    int user = ids[0];
    verifyEq(c.load(user).type.qname, "sys::UserService");
        
    // all services
    ids = c.queryService(schema.type("sys::Service"));
    verify(ids.length >= 2);
    
    // verify SoxService and UserService found in list of all services
    boolean soxFound = false, userFound = false;
    for (int i=0; i<ids.length; ++i) 
    {
      if (ids[i] == sox)  soxFound = true;
      if (ids[i] == user) userFound = true;
    }
    verify(soxFound);
    verify(userFound);
    
    // bad services
    ids = c.queryService(schema.type("sys::User"));
    verifyEq(ids.length, 0);    
  }
  
}
