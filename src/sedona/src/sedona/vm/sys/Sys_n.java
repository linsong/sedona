//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Nov 08  Brian Frank  Creation
//

package sedona.vm.sys;

import java.text.*;
import sedona.vm.*;

/**
 * sys::Sys native methods
 */
public class Sys_n
{                       

////////////////////////////////////////////////////////////////
// Arrays
////////////////////////////////////////////////////////////////

  /**
   * Copy num bytes from the source byte array to the destination byte
   * array.  The arrays may be overlapping (like memmove, not memcpy).
   */
  public static void copy(byte[] src, int srcOff, byte[] dest, int destOff, int len, Context cx)
  {     
    System.arraycopy(src, srcOff, dest, destOff, len);
  }

  /**
   * Compare two byte arrays for equality. If equal return 0, if
   * a is less than b return -1, if a greater than b return 1.
   */
  public static int compareBytes(byte[] a, int aOff, byte[] b, int bOff, int len, Context cx)
  {             
    for (int i=0; i<len; ++i)
    {                         
      int ax = a[aOff+i];
      int bx = b[bOff+i];
      if (ax != bx) return ax < bx ? -1 : +1;
    }
    return 0;
  }            

  /**
   * Set all the bytes in the specified array to val.
   */
  public static void setBytes(int val, byte[] bytes, int off, int len, Context cx)
  {   
    for (int i=0; i<len; ++i) bytes[off+i] = (byte)val;      
  }

  /**
   * Perform a bitwise "and" using the specified mask on each
   * byte in the bytes array.
   */
  public static void andBytes(int mask, byte[] bytes, int off, int len, Context cx)
  {
    for (int i=0; i<len; ++i) bytes[off+i] &= mask;      
  }

  /**
   * Perform a bitwise "or" using the specified mask on each
   * byte in the bytes array.
   */
  public static void orBytes(int mask, byte[] bytes, int off, int len, Context cx)   
  {
    for (int i=0; i<len; ++i) bytes[off+i] |= mask;      
  }

////////////////////////////////////////////////////////////////
// Strings
////////////////////////////////////////////////////////////////

  /**
   * Format an integer as a decimal string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef intStr(int v, Context cx)
  {                   
    // this is for debug, so don't worry about GC for now
    return str(Integer.toString(v));
  }

  /**
   * Format an integer as a hexadecimal string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef hexStr(int v, Context cx)
  {
    // this is for debug, so don't worry about GC for now
    return str(Integer.toHexString(v));
  }

  /**
   * Format the 64-bit integer into a string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef longStr(long v, Context cx)
  {
    // this is for debug, so don't worry about GC for now
    return str(Long.toString(v));
  }

  /**
   * Format the 64-bit integer into a hexidecimal string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef longHexStr(long v, Context cx)
  {
    // this is for debug, so don't worry about GC for now
    return str(Long.toHexString(v));
  }

  /**
   * Format a float into a string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef floatStr(float v, Context cx)
  {
    // this is for debug, so don't worry about GC for now
    return str(decimalFormat.format(v));
  }

  /**
   * Format a double into a string.
   * The string is stored in a static shared buffer.
   */
  public static StrRef doubleStr(double v, Context cx)
  {                                 
    // this is for debug, so don't worry about GC for now
    return str(decimalFormat.format(v));
  }                          
        
  static DecimalFormat decimalFormat = new DecimalFormat("0.000000");  
  
  static StrRef str(String s) { return VmUtil.strConst(s); }

////////////////////////////////////////////////////////////////
// Floating Point
////////////////////////////////////////////////////////////////

  /**
   * Return an integer representation of a 32-bit floating point
   * value according to the IEEE 754 floating-point "single format"
   * bit layout.
   */
  public static int floatToBits(float v, Context cx)
  {
    return Float.floatToIntBits(v);
  }

  /**
   * Return a long representation of a 64-bit floating point
   * value according to the IEEE 754 floating-point "double format"
   * bit layout.
   */
  public static long doubleToBits(double v, Context cx)
  {
    return Double.doubleToLongBits(v);
  }
                                         
  /**
   * Return a 32-bit floating point value according to the
   * IEEE 754 floating-point "single format" bit layout.
   */
  public static float bitsToFloat(int bits, Context cx)
  {
    return Float.intBitsToFloat(bits);
  }

  /**
   * Return a 64-bit floating point value according to the
   * IEEE 754 floating-point "double format" bit layout.
   */
  public static double bitsToDouble(long bits, Context cx)  
  {
    return Double.longBitsToDouble(bits);
  }
  
////////////////////////////////////////////////////////////////
// Time
////////////////////////////////////////////////////////////////

  public static long ticks(Context cx)
  {
    return System.nanoTime();
  }

  public static void sleep(long ns, Context cx)
    throws InterruptedException
  {                        
    if (ns <= 0) return;
    long ms = ns / 1000000L;
    int rem = (int)(ns % 1000000L);
    Thread.sleep(ms, rem);
  }


////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  public static StrRef platformType(Context cx)    
  {
    return str("sys::Platform");
  }

  public static void free(Object obj, Context cx)    
  {
    // ignored for Java since we have GC
  }

  public static int rand(Context cx)    
  {
    return random.nextInt();
  }  
  static final java.util.Random random = new java.security.SecureRandom();

  
  /**
   * Provide a byte array to which looks like scode to access 
   * scode flags (which is the only reason this should be used)
   */
  public static byte[] scodeAddr() { return scodeAddr; }
  static byte[] scodeAddr = new byte[32];
  static
  {
    // scode flags fixed at 23 byte offset
    scodeAddr[23] = 0x3; // debug|test
  }
  
}

