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
 * Test that we can reorder components.
 */
public class SoxReorder extends Test
{ 
  public SoxReorder() { super("reorder"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    Schema schema = c.readSchema();               
    Type soxCertType = schema.type("sox::SoxCert");
    
    // create r with 4 children components to x
    SoxComponent r = c.add(x, soxCertType, "r", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent i = c.add(r, soxCertType, "i", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent j = c.add(r, soxCertType, "j", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent k = c.add(r, soxCertType, "k", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    SoxComponent m = c.add(r, soxCertType, "m", new Value[] {Int.make(0xf), Bool.make(false), Byte.make(0)});    
    
    // verify original order
    c.update(r, SoxComponent.TREE);
    verifyEq(r.childrenIds().length, 4);
    verifyEq(r.childrenIds()[0], i.id());
    verifyEq(r.childrenIds()[1], j.id());
    verifyEq(r.childrenIds()[2], k.id());
    verifyEq(r.childrenIds()[3], m.id());
    
    // reorder and verify new order
    c.reorder(r, new int[] { m.id(), i.id(), k.id(), j.id() });
    c.update(r, SoxComponent.TREE);
    verifyEq(r.childrenIds().length, 4);
    verifyEq(r.childrenIds()[0], m.id());
    verifyEq(r.childrenIds()[1], i.id());
    verifyEq(r.childrenIds()[2], k.id());
    verifyEq(r.childrenIds()[3], j.id());
    
    // cleanup
    c.delete(r);
  }
  
}
