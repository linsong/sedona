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
import sedona.Short;
import sedona.Long;
import sedona.Float;
import sedona.Double;
import sedona.sox.*;
import sedona.dasp.*;

/**
 * Test that we can add/remove links and they actually work.
 */
public class SoxLinks extends Test
{ 
  public SoxLinks() { super("links"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();               
    Type soxCertType = schema.type("sox::SoxCert");   
    SoxComponent app = c.loadApp();
    long scanPeriod = ((Int)c.readProp(app, app.slot("scanPeriod"))).val;
    
    // add two components a, b under x
    SoxComponent a = c.add(x, soxCertType, "a", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});
    SoxComponent b = c.add(x, soxCertType, "b", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});

    // create link a.i -> b.i
    Link link = new Link(a, a.slot("i"), b, b.slot("i"));
    c.link(link);  
    
    // change a.i, verify b.i modified
    verifyEq(c.readProp(a, a.slot("i")), Int.make(0xcafebabe));
    verifyEq(c.readProp(b, b.slot("i")), Int.make(0xcafebabe));
    c.write(a, a.slot("i"), Int.make(-37));
    Thread.sleep(scanPeriod*2);
    verifyEq(c.readProp(a, a.slot("i")), Int.make(-37));
    verifyEq(c.readProp(b, b.slot("i")), Int.make(-37));
    
    // unlink a.i -> b.i, verify b.i defaults to zero
    c.unlink(link);
    verifyEq(c.readProp(b, b.slot("i")), Int.make(0));
    
    // change a.i, verify b.i *not* modified
    c.write(a, a.slot("i"), Int.make(123456));
    Thread.sleep(scanPeriod*2);
    verifyEq(c.readProp(a, a.slot("i")), Int.make(123456));
    verifyEq(c.readProp(b, b.slot("i")), Int.make(0));

    // delete components we created
    c.delete(a);
    c.delete(b);
  }
  
}
