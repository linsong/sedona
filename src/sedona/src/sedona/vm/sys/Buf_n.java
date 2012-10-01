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
 * sys::Buf native methods
 */
public class Buf_n
  extends ReflectUtil
{                             
  
  public static StrRef toStr(Object self, Context cx)      
    throws Exception
  {        
    // TODO: roll this into bytecode                         
    return new StrRef((byte[])get(self, "bytes"));
  }
  
}

