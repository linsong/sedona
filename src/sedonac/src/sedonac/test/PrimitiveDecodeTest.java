//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//  21 Apr 09  Dan Giorgis  Creation
//

package sedonac.test;

import sedona.*;
import sedona.Byte;
import sedona.Short;
import sedona.Int;
import sedona.Long;

import java.io.*;

import sedona.offline.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * PrimitiveDecodeTest
 */
public class PrimitiveDecodeTest
  extends Test
{

  public void testByte()
    throws Exception
  {
    Exception ex;
    Byte b = Byte.make(0);
    

    b = (Byte)b.decodeString("0");
    verify(b.val == 0);

    b = (Byte)b.decodeString("0x10");
    verify(b.val == 0x10);

    b = (Byte)b.decodeString("255");
    verify(b.val == 255);

    b = (Byte)b.decodeString("0xff");
    verify(b.val == 0xff);
  
    // out of range
    ex = null; try { b = (Byte)b.decodeString("-1"); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // out of range
    ex = null; try { b = (Byte)b.decodeString("256"); } catch (Exception e) { ex = e; }         
    verify(ex != null);
  }                            
  
  public void testShort()
    throws Exception
  {
    Exception ex;
    Short s = Short.make(0);
    

    s = (Short)s.decodeString("0");
    verify(s.val == 0);

    s = (Short)s.decodeString("1234");
    verify(s.val == 1234);

    s = (Short)s.decodeString("0xffff");
    verify(s.val == 0xffff);

    s = (Short)s.decodeString("0x1234");
    verify(s.val == 0x1234);
  
    // out of range
    ex = null; try { s = (Short)s.decodeString("-1"); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // out of range
    ex = null; try { s = (Short)s.decodeString("0x10000"); } catch (Exception e) { ex = e; }         
    verify(ex != null);
  }                            

  public void testInt()
    throws Exception
  {
    Exception ex;
    Int i = Int.make(0);
    

    i = (Int)i.decodeString("0");
    verify(i.val == 0);

    i = (Int)i.decodeString("0x10");
    verify(i.val == 0x10);

    i = (Int)i.decodeString("5678");
    verify(i.val == 5678);

    i = (Int)i.decodeString("0x7fffffff");
    verify(i.val == 0x7fffffff);
  
    // out of range                             abcdabcda
    ex = null; try { i = (Int)i.decodeString("0xfffffffff"); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // out of range
    ex = null; try { i = (Int)i.decodeString("0x100000000"); } catch (Exception e) { ex = e; }         
    verify(ex != null);
  }                            

  public void testLong()
    throws Exception
  {
    Exception ex;
    Long l = Long.make(0);
    

    l = (Long)l.decodeString("0");
    verify(l.val == 0);

    l = (Long)l.decodeString("0x10");
    verify(l.val == 0x10);

    l = (Long)l.decodeString("255");
    verify(l.val == 255);

    //                          abcdABCDabcdABCD
    l = (Long)l.decodeString("0x7fffffffffffffff");
    verify(l.val == 0x7fffffffffffffffL);
  
    // out of range                              abcdABCDabcdABCDa
    ex = null; try { l = (Long)l.decodeString("0xfffffffffffffffff"); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // out of range                              abcdABCDabcdABCDa
    ex = null; try { l = (Long)l.decodeString("0x10000000000000000"); } catch (Exception e) { ex = e; }         
    verify(ex != null);
  }                            

}
