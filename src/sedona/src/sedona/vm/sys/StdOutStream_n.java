//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Nov 08  Brian Frank  Creation
//

package sedona.vm.sys;

import sedona.vm.*;

/**
 * sys::StdOutStream native methods
 */
public class StdOutStream_n
{                                   

  public static byte doWrite(int b, Context cx) 
  {                             
    System.out.write(b);
    return 1;
  }
  
  public static byte doWriteBytes(byte[] b, int off, int len, Context cx)
  {                             
    System.out.write(b, off, len);
    return 1;
  }
  
  public static void doFlush(Context cx)
  {
    System.out.flush();
  }

    
}

