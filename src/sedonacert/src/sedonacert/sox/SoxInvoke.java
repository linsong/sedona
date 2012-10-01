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
 * Test that we can invoke actions.
 */
public class SoxInvoke extends Test
{ 
  public SoxInvoke() { super("invoke"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    
    // set properties to know value
    c.write(x, x.slot("z"), Bool.make(false));
    c.write(x, x.slot("i"), Int.make(100));
    c.write(x, x.slot("d"), Double.make(1000d));
    c.write(x, x.slot("buf"), new Buf(new byte[] { 0, 0, 0, 0}));

    // invokes
    c.invoke(x, x.slot("av"), null);
    c.invoke(x, x.slot("ai"), Int.make(23));
    c.invoke(x, x.slot("ad"), Double.make(234d));
    c.invoke(x, x.slot("abuf"), new Buf(new byte[] { 0x34, 0x56, (byte)0xab }));
    
    // verify actions modified props as expected
    verifyEq(c.readProp(x, x.slot("z")), Bool.make(true));   
    verifyEq(c.readProp(x, x.slot("i")), Int.make(123));
    verifyEq(c.readProp(x, x.slot("d")), Double.make(1234d));
    verifyEq(c.readProp(x, x.slot("buf")).toString(), "0x[3456ab]");        
  }
  
}
