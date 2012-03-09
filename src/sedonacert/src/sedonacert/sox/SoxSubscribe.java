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
 * Test that we can subscribe to the 4 major component 
 * topics: tree, config, runtime, links.
 */
public class SoxSubscribe extends Test
{ 
  public SoxSubscribe() { super("subscribe"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();               
    Type soxCertType = schema.type("sox::SoxCert");         
    long pause = 500L; // ms
    
    // start with fresh component, with link and one child
    SoxComponent u = c.add(x, soxCertType, "update", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(99)});    
    c.write(u, x.slot("b"), Byte.make(0));
    c.write(u, x.slot("i"), Int.make(0));
    Link link = new Link(u, u.slot("i"), u, u.slot("i"));
    c.link(link);
    SoxComponent kidA = c.add(u, soxCertType, "kidA", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});
    
    // verify initial subscribe of tree/links
    c.subscribe(u, SoxComponent.TREE|SoxComponent.LINKS);
    verifyEq(u.parentId(), x.id());
    verifyEq(u.childrenIds().length, 1);
    verifyEq(u.childrenIds()[0], kidA.id());
    verifyEq(u.getInt("b"), 99);
    verifyEq(u.getInt("i"), 0xcafebabe);
    verifyEq(u.links().length, 1);
    verifyEq(u.links()[0], link);
    
    // verify initial subscribe of runtime/config
    c.subscribe(u, SoxComponent.RUNTIME|SoxComponent.CONFIG);
    verifyEq(u.childrenIds()[0], kidA.id());
    verifyEq(u.getInt("b"), 0);
    verifyEq(u.getInt("i"), 0);
    verifyEq(u.links()[0], link);     
    
    // make change to runtime and config prop, verify event
    c.write(u, u.slot("b"), Byte.make(21));
    c.write(u, u.slot("f"), Float.make(1066f));
    Thread.sleep(pause);  
    verifyEq(u.get("f"), Float.make(1066f));
    verifyEq(u.get("b"), Byte.make(21));
        
    // remove link, verify event
    c.unlink(link);
    Thread.sleep(pause);  
    verifyEq(u.links().length, 0);     
        
    // remove child & add link, verify event
    c.delete(kidA);
    c.link(link);
    Thread.sleep(pause);  
    verifyEq(u.childrenIds().length, 0);
    verifyEq(u.links().length, 1);     
    verifyEq(u.links()[0], link);     
    
    // add child
    SoxComponent kidB = c.add(u, soxCertType, "kidB", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});
    Thread.sleep(pause);  
    verifyEq(u.childrenIds().length, 1);
    verifyEq(u.childrenIds()[0], kidB.id());
    
    // cleanup
    c.delete(u);
  }
  
}
