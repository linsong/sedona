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
 * Test that we can update the 4 major component 
 * topics: tree, config, runtime, links.
 */
public class SoxUpdate extends Test
{ 
  public SoxUpdate() { super("update"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();               
    Type soxCertType = schema.type("sox::SoxCert");
    
    // start with fresh component
    SoxComponent u = c.add(x, soxCertType, "update", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    c.write(u, x.slot("b"), Byte.make(0));
    c.write(u, x.slot("i"), Int.make(0));
    c.update(u, SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS);
    
    // verify fresh state
    verifyEq(u.parentId(), x.id());
    verifyEq(u.childrenIds().length, 0);
    verifyEq(u.getInt("b"), 0);
    verifyEq(u.getInt("i"), 0);
    verifyEq(u.links().length, 0);
    
    // make tree, config, runtime, and link changes
    SoxComponent foo = c.add(u, soxCertType, "foo", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});
    Link link = new Link(u, u.slot("i"), u, u.slot("i"));
    c.link(link);
    c.write(u, u.slot("b"), Byte.make(79));
    c.write(u, u.slot("i"), Int.make(1876));
    
    // update tree
    c.update(u, SoxComponent.TREE);
    verifyEq(u.childrenIds().length, 1);
    verifyEq(u.childrenIds()[0], foo.id());
    verifyEq(u.getInt("b"), 0);
    verifyEq(u.getInt("i"), 0);
    verifyEq(u.links().length, 0);
    
    // update config
    c.update(u, SoxComponent.CONFIG);
    verifyEq(u.childrenIds()[0], foo.id());
    verifyEq(u.getInt("b"), 79);
    verifyEq(u.getInt("i"), 0);
    verifyEq(u.links().length, 0);
    
    // update runtime
    c.update(u, SoxComponent.RUNTIME);
    verifyEq(u.childrenIds()[0], foo.id());
    verifyEq(u.getInt("b"), 79);
    verifyEq(u.getInt("i"), 1876);
    verifyEq(u.links().length, 0);
    
    // update links
    c.update(u, SoxComponent.LINKS);
    verifyEq(u.childrenIds()[0], foo.id());
    verifyEq(u.getInt("b"), 79);
    verifyEq(u.getInt("i"), 1876);
    verifyEq(u.links().length, 1);
    verifyEq(u.links()[0], link);     
    
    // cleanup
    c.delete(u);
  }
  
}
