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
 * Test that we can delete a component with children and links.
 */
public class SoxDelete extends Test
{ 
  public SoxDelete() { super("delete"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();               
    Type soxCertType = schema.type("sox::SoxCert");
    int[] oldKids = x.childrenIds();
    
    // build component tree with children and links:
    //   +-x
    //     +- p
    //     +- a
    //     |   +- k
    //     +- b 
    //   a.i -> b.i
    //   b.i -> k.i
    SoxComponent p = c.add(x, soxCertType, "p", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent a = c.add(x, soxCertType, "a", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent b = c.add(x, soxCertType, "b", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent k = c.add(a, soxCertType, "k", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    Link a2b = new Link(a, a.slot("i"), b, b.slot("i"));
    Link b2k = new Link(b, b.slot("i"), k, k.slot("i"));
    c.link(a2b);
    c.link(b2k);
    
    // verify before delete    
    c.update(a, SoxComponent.TREE|SoxComponent.LINKS);
    c.update(b, SoxComponent.TREE|SoxComponent.LINKS);
    c.update(k, SoxComponent.TREE|SoxComponent.LINKS);
    verifyEq(a.parentId(), x.id());
    verifyEq(b.parentId(), x.id());
    verifyEq(k.parentId(), a.id());
    int[] newKids = x.childrenIds();
    verifyEq(newKids.length, oldKids.length+3);
    verifyEq(newKids[newKids.length-3], p.id());
    verifyEq(newKids[newKids.length-2], a.id());
    verifyEq(newKids[newKids.length-1], b.id());
    verifyEq(b.links().length, 2);
    verifyEq(b.links()[0], a2b);
    verifyEq(b.links()[1], b2k);      
    
    // delete a, verify k deleted and links removed
    c.delete(a);
    c.update(b, SoxComponent.TREE|SoxComponent.LINKS);
    verifyEq(b.parentId(), x.id());
    newKids = x.childrenIds();
    verifyEq(newKids.length, oldKids.length+2);
    verifyEq(newKids[newKids.length-2], p.id());
    verifyEq(newKids[newKids.length-1], b.id());
    verifyEq(b.links().length, 0);     
    try { c.update(a, SoxComponent.TREE|SoxComponent.LINKS); fail(); } catch (SoxException e) { verify(true); }
    try { c.update(k, SoxComponent.TREE|SoxComponent.LINKS); fail(); } catch (SoxException e) { verify(true); }
        
    // cleanup
    c.delete(b);
  }
  
}
