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
 * Test that we can rename a component.
 */
public class SoxRename extends Test
{ 
  public SoxRename() { super("rename"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    
    // verify name we used in SoxAdd
    verifyEq(x.name(), "soxcert");
    
    // rename
    c.rename(x, "brian");       
    c.invoke(x, x.slot("name2buf"), null);
    verifyEq(c.readProp(x, x.slot("buf")), new Buf(new byte[] { 'b', 'r', 'i', 0 }));
    
    // cleanup
    c.rename(x, "soxcert");
  }
  
}
