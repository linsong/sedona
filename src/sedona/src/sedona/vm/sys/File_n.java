//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 Nov 08  Brian Frank  Creation
//

package sedona.vm.sys;

import java.io.*;
import sedona.vm.*;

/**
 * sys::File native methods
 */
public class File_n
{                                            

  public static boolean rename(StrRef from, StrRef to, Context cx)
  {      
    File fromFile = new File(from.toString());
    File toFile = new File(to.toString());
    return fromFile.renameTo(toFile);
  }
  
}

