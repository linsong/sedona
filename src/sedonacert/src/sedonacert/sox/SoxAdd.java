//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Jun 09  Brian Frank  Creation
//

package sedonacert.sox;

import sedonacert.*;
import sedona.*;
import sedona.Byte;
import sedona.sox.*;
import sedona.dasp.*;

/**
 * Test that we can add a component to a running app.
 */
public class SoxAdd extends Test
{ 
  public SoxAdd() { super("add"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;             
    Schema schema = c.readSchema();               
    
    // verify SoxCert is installed on device
    Type soxCertType = schema.type("sox::SoxCert");
    if (soxCertType == null) fail("sox::SoxCert type not installed");
    
    // load app
    SoxComponent app = c.loadApp();
    verifyEq(app.id(), 0); 
    int[] oldKids = app.childrenIds();
    
    // create SoxCert component
    SoxComponent x = c.add(app, soxCertType, "soxcert", new Value[] 
      {Int.make(0xf), Bool.make(false), Byte.make(0xcc)});
    ((Sox)bundle).soxCertId = x.id();

    // verify new children in app
    app = c.update(app, SoxComponent.TREE);
    int[] newKids = app.childrenIds();
    verifyEq(oldKids.length, newKids.length-1);
    verifyEq(newKids[newKids.length-1], x.id());  
    
    // update and verify initial values 
    c.update(x, SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS);
    verifyEq(x.parentId(), 0);
    verifyEq(x.childrenIds().length, 0);
    verifyEq(x.getBool("z"),  false);
    verifyEq(x.getInt("b"),  0xcc);
    verifyEq(x.getInt("s"),  0xcdef);
    verifyEq(x.getInt("i"), 0xcafebabe);
    verifyEq(x.getLong("l"),  0xcafebabedeadbeefL);
    verifyEq(x.getFloat("f"),  2.04f);
    verifyEq(x.getDouble("d"),  256d);
    verifyEq(((Buf)x.get("buf")).toString(), "0x[]");
  }
  
}
