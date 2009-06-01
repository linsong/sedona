//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.lang.Float;
import java.lang.Double;
import java.io.*;
import java.lang.reflect.*;
import sedona.*;
import sedona.kit.*;
import sedona.util.*;

/**
 * VmUtil
 */
public class VmUtil
{  

////////////////////////////////////////////////////////////////
// Float
////////////////////////////////////////////////////////////////

  public static boolean floatEQ(float a, float b)
  {     
    if (Float.isNaN(a)) return Float.isNaN(b);               
    return a == b;
  }

  public static boolean floatNE(float a, float b)
  {                    
    if (Float.isNaN(a)) return !Float.isNaN(b);               
    return a != b;
  }

////////////////////////////////////////////////////////////////
// Double
////////////////////////////////////////////////////////////////

  public static boolean doubleEQ(double a, double b)
  {     
    if (Double.isNaN(a)) return Double.isNaN(b);               
    return a == b;
  }

  public static boolean doubleNE(double a, double b)
  {                    
    if (Double.isNaN(a)) return !Double.isNaN(b);               
    return a != b;
  }

////////////////////////////////////////////////////////////////
// Constants
////////////////////////////////////////////////////////////////
  
  public static StrRef strConst(String s)
  {                 
    return StrRef.make(s);
  }

  public static byte[] bufConst(String s)
  {          
    // decode base64 string into byte array                  
    return Base64.decode(s);
  }

////////////////////////////////////////////////////////////////
// Assert
////////////////////////////////////////////////////////////////

  public static void assertOp(boolean cond)
    throws Exception
  {                                      
    if (cond)
    {
      assertSuccess++;
    }
    else
    {
      assertFailure++; 
      System.out.println();
      System.out.println("TEST FAILED");
      new Exception("Assert Failed").printStackTrace();
    }
  }     

  public static void echo(Object obj)
  {
    System.out.println(obj);
  }
  
  public static int assertSuccess = 0;
  public static int assertFailure = 0;  
  
}

