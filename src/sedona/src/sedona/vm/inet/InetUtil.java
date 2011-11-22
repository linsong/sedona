//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Nov 08  Brian Frank  Creation
//                     

package sedona.vm.inet;

import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import sedona.vm.*;

/**
 * InetUtil
 */
public class InetUtil   
  extends ReflectUtil
{                  

  
  /**
   * Convert a Java InetAddress to a Sedona inet::IpAddr
   */
  static void javaToIpAddr(InetAddress java, Object sedona)
    throws Exception
  {    
    byte[] sbytes = (byte[])get(sedona, "addr");
    byte[] jbytes = java.getAddress();
    if (jbytes.length == 4)
    {                                        
      for (int i=0; i<10; ++i) sbytes[0] = 0;
      sbytes[10] = sbytes[11] = (byte)0xff;
      System.arraycopy(jbytes, 0, sbytes, 12, jbytes.length);
    }
    else
    {
      System.arraycopy(jbytes, 0, sbytes, 0, jbytes.length);
    }
  }            
  
  /**
   * Convert a Sedona inet::IpAddr to a Java InetAddress.
   */
  static InetAddress javaFromIpAddr(Object sedona)
    throws Exception
  { 
    // Java accepts IPv4 as 16 bytes            
    return InetAddress.getByAddress((byte[])get(sedona, "addr"));
  }                                             
  
  /*
  static boolean isIpV4(byte[] addr)
  {                                
    if ((addr[10+2] & 0xff) != 0xff) return false;
    if ((addr[11+2] & 0xff) != 0xff) return false;
    for (int i=0; i<10; ++i)
      if ((addr[i+2] & 0xff) != 0xff) return false;
    return true;
  } 
  */
  
}

