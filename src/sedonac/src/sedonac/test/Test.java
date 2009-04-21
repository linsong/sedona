//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.lang.reflect.*;
import sedona.*;
import sedona.util.*;

/**
 * Test
 */
public class Test
{

//////////////////////////////////////////////////////////////////////////
// Top Level
//////////////////////////////////////////////////////////////////////////

  public static int run(String testName)
  {
    try
    {
      long t1 = Env.ticks();
      int totalVerified = 0;
      int totalFailed = 0;

      String[] tests =
      {
        "sedonac.test.TokenizerTest",
        "sedonac.test.ParserTest",
        "sedonac.test.ManifestTest",
        "sedonac.test.KitDbTest",
        "sedonac.test.AppTest",        
        "sedonac.test.DocParserTest",
        "sedonac.test.DaspTest",
        "sedonac.test.SoxTest",
        "sedonac.test.SecurityTest",        
        "sedonac.test.PrimitiveDecodeTest",
      };

      for (int i=0; i<tests.length; ++i)
      {
        if (testName != null && !TextUtil.getClassName(tests[i]).equals(testName)) continue;
        Class cls = Class.forName(tests[i]);
        Test test = (Test)cls.newInstance();
        test.run();
        totalVerified += test.verified;
        totalFailed   += test.failed;
      }
      
      long t2 = Env.ticks();
      System.out.println();
      if (totalFailed == 0)
        System.out.println("ALL TESTS PASSED " + totalVerified + " total verifies [" + (t2-t1) + "ms]");
      else
        System.out.println("SOME TESTS FAILED " + totalFailed + " total failures [" + (t2-t1) + "ms]");

      return totalFailed;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return 1;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
    throws Exception
  {
    Method[] methods = getClass().getMethods();
    for (int i=0; i<methods.length; ++i)
    {
      Method m = methods[i];     
      if (m.getDeclaringClass() == Test.class) continue;
      if (m.getName().startsWith("test")) run(m);
    }
  }

  public void run(Method m)
    throws Exception
  {
    System.out.print("-- " + getClass().getName() + "." + m.getName());
    System.out.flush();
    int startVerified = verified;
    try
    {
      m.invoke(this, new Object[] {});
      System.out.println(" [" + (verified-startVerified) + " verifies]");
    }
    catch (InvocationTargetException e)
    {
      failed++;
      System.out.println("\nFAILED");
      e.getTargetException().printStackTrace();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Verify
//////////////////////////////////////////////////////////////////////////

  public void verify(boolean cond)
  {                              
    verify(cond, "failed");
  }

  public void verify(boolean cond, String msg)
  {
    if (cond)
      verified++;
    else
      throw new TestException(msg);
  }

  public void fail()
  {
    throw new TestException("failed");
  }

  public void verifyEq(Object a, Object b)
  {
    if (a == b)
      verified++;
    else if (a != null && b != null && a.equals(b))
      verified++;
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(int a, int b)
  {
    if (a == b)
      verified++;
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(long a, long b)
  {
    if (a == b)
      verified++;
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(float a, float b)
  {               
    if (sedona.Float.equals(a, b))
      verified++;
    else
      throw new TestException(a + " != " + b);
  }                   
  
  public void verifyEq(double a, double b)
  {               
    if (sedona.Double.equals(a, b))
      verified++;
    else
      throw new TestException(a + " != " + b);
  }

  public void verifyEq(boolean a, boolean b)
  {
    if (a == b)
      verified++;
    else
      throw new TestException(a + " != " + b);
  }                    
  
  public void verifyEq(byte[] a, byte[] b)
  {                                    
    if (a == null)
    {
      if (b == null) 
        verified++;
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
        verified++;
      else
        throw new TestException(new Buf(a) + " != " + new Buf(b));
    }
  }

  public void verifyEq(int[] a, int[] b)
  {                                    
    if (a == null)
    {
      if (b == null) 
        verified++;
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
        verified++;
      else
        throw new TestException(ArrayUtil.toString(a) + " != " + ArrayUtil.toString(b));
    }
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public File testDir()
  {
    File dir = new File(Env.home, "test");
    dir.mkdirs();
    verify(dir.exists());
    return dir;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  int verified;
  int failed;

}
