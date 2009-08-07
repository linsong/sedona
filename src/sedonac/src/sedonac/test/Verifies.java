//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//   22 Jun 09  Brian Frank  Break out from Test into separate class
//

package sedonac.test;

import java.io.*;
import java.lang.reflect.*;
import sedona.*;
import sedona.util.*;

/**
 * Verifies
 */
public abstract class Verifies
{

  public abstract void verify(boolean cond, String msg);

  public void verify(boolean cond)
  {                              
    verify(cond, "failed");
  }

  public void fail()
  {
    throw new TestException("failed");    
  }

  public void fail(String msg)
  {
    throw new TestException(msg);
  }

  public void verifyEq(Object a, Object b)
  {
    if (a == b)
      verify(true);
    else if (a != null && b != null && a.equals(b))
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(int a, int b)
  {
    if (a == b)
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyNotEq(int a, int b)
  {
    if (a != b)
      verify(true);
    else
      throw new TestException(a + " == " + b);
  }

  public void verifyEq(long a, long b)
  {
    if (a == b)
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(float a, float b)
  {               
    if (sedona.Float.equals(a, b))
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }                   
  
  public void verifyEq(double a, double b)
  {               
    if (sedona.Double.equals(a, b))
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(boolean a, boolean b)
  {
    if (a == b)
      verify(true);
    else
      throw new TestException(a + " != " + b);
  }                    
  
  public void verifyEq(byte[] a, byte[] b)
  {                                    
    if (a == null)
    {
      if (b == null) 
        verify(true);
      else
        throw new TestException("null != " + new Buf(b));      
    }                                                  
    else if (b == null)
    {
      throw new TestException(new Buf(a) + " != null");      
    }         
    else
    {    
      boolean eq = true;
      if (a.length != b.length) eq = false;
      else
      {
        for (int i=0; i<a.length; ++i)
          if (a[i] != b[i]) { eq = false; break; }
      }                       
      
      if (eq)
        verify(true);
      else
        throw new TestException(new Buf(a) + " != " + new Buf(b));
    }
  }

  public void verifyEq(int[] a, int[] b)
  {                                    
    if (a == null)
    {
      if (b == null) 
        verify(true);
      else
        throw new TestException("null != " + ArrayUtil.toString(b));      
    }                                                  
    else if (b == null)
    {
      throw new TestException(ArrayUtil.toString(a) + " != null");      
    }         
    else
    {    
      boolean eq = true;
      if (a.length != b.length) eq = false;
      else
      {
        for (int i=0; i<a.length; ++i)
          if (a[i] != b[i]) { eq = false; break; }
      }                       
      
      if (eq)
        verify(true);
      else
        throw new TestException(ArrayUtil.toString(a) + " != " + ArrayUtil.toString(b));
    }
  }

}
