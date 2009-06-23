//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Nov 08  Brian Frank  Creation
//                     

package sedona.vm.inet;

import java.security.*;
import sedona.vm.*;

/**
 * inet::Cryptp native methods
 */
public class Crypto_n   
{                  

  static MessageDigest digester;
  static
  {
    try
    {
      digester = MessageDigest.getInstance("SHA-1");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }                     
  }
           
  public static void sha1(byte[] in, int inOff, int len, byte[] output, int outOff, Context cx)
    throws Exception
  {                                         
    digester.reset();
    digester.update(in, inOff, len);   
    byte[] result = digester.digest();
    System.arraycopy(result, 0, output, outOff, result.length);
  }
  
}

