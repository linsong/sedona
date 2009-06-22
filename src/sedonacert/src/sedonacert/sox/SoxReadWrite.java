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
 * Test that we can read/write each property.
 */
public class SoxReadWrite extends Test
{ 
  public SoxReadWrite() { super("read-write"); }
  
  public void run()
    throws Exception
  {        
    SoxClient c = runner.sox;           
    SoxComponent x = ((Sox)bundle).soxCertComp();
    
    // write each SoxCert property
    c.write(x, x.slot("z"), Bool.make(false));
    c.write(x, x.slot("b"), Byte.make(200));
    c.write(x, x.slot("s"), Short.make(64000));
    c.write(x, x.slot("i"), Int.make(-123456789));
    c.write(x, x.slot("l"), Long.make(-135557L));
    c.write(x, x.slot("f"), Float.make(-567.0f));
    c.write(x, x.slot("d"), Double.make(-1972.0));
    c.write(x, x.slot("buf"), new Buf(new byte[] { 1, 2, 3, 4}));

    // verify readProp
    verifyEq(c.readProp(x, x.slot("z")), Bool.make(false));   
    verifyEq(c.readProp(x, x.slot("b")), Byte.make(200));
    verifyEq(c.readProp(x, x.slot("s")), Short.make(64000));
    verifyEq(c.readProp(x, x.slot("i")), Int.make(-123456789));
    verifyEq(c.readProp(x, x.slot("l")), Long.make(-135557L));
    verifyEq(c.readProp(x, x.slot("f")), Float.make(-567.0f));
    verifyEq(c.readProp(x, x.slot("d")), Double.make(-1972.0));
    verifyEq(c.readProp(x, x.slot("buf")).toString(), "0x[01020304]");
        
    // verify update
    c.update(x, SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS);
    verifyEq(x.parentId(), 0);
    verifyEq(x.childrenIds().length, 0);
    verifyEq(x.getBool("z"),   false);   
    verifyEq(x.getInt("b"),    200);
    verifyEq(x.getInt("s"),    64000);
    verifyEq(x.getInt("i"),    -123456789);
    verifyEq(x.getLong("l"),   -135557);
    verifyEq(x.getFloat("f"),  -567.0f);
    verifyEq(x.getDouble("d"), -1972.0);
    verifyEq(((Buf)x.get("buf")).toString(), "0x[01020304]");
  }
  
}
