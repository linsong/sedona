//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jul 08  Brian Frank  Creation
//

package sedona.util;

import java.security.*;
import sedona.Buf;

/**
 * UserUtil provides utility functions for working with Sedona 
 * users and their associated SHA-1 digests.
 */
public class UserUtil
{                   

  public static byte[] credentials(String user, String pass)
  {
    try
    {
      String text = user + ":" + pass;
      return MessageDigest.getInstance("SHA-1").digest(text.getBytes("UTF-8"));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.toString());
    }
  }

  public static void main(String[] args)
  {
    if (args.length < 2)
    {
      System.out.println("usage: UserUtil <user> <pass>");
      return;
    }        
    
    String user = args[0];
    String pass = args[1];
    Buf buf = new Buf(credentials(user, pass));
    
    System.out.println("User:   " + user + ":" + pass);
    System.out.println("Digest: " + buf);
    System.out.println("Base64: " + buf.encodeString());
  }
  
}
