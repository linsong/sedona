//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.lang.Byte;
import java.lang.Float;
import java.lang.Double;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import sedona.*;
import sedona.Type;
import sedona.kit.*;
import sedona.util.*;

/**
 * StrRef is used to reference a null-terminated C 
 * string stored in a byte[] with an offset.
 */
public class StrRef
{                         

////////////////////////////////////////////////////////////////
// Construction
////////////////////////////////////////////////////////////////

  public static StrRef make(int size)
  { 
    return new StrRef(new byte[size]);                  
  }
  
  public static StrRef make(String s)
  {                 
    // leave room for null terminator
    byte[] a = new byte[s.length()+1];
    for (int i=0; i<s.length(); ++i)
      a[i] = (byte)s.charAt(i);
    return new StrRef(a);
  }

  public StrRef(byte[] buf, int off) 
  { 
    this.buf = buf; 
    this.off = off; 
  }    
   
  public StrRef(byte[] buf) 
  { 
    this.buf = buf; 
  }    
   
  public StrRef() 
  {
  }         

////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////
  
  /**
   * Get this StrRef as a Java string.
   */
  public String toString()
  {                                  
    StringBuffer s = new StringBuffer();
    for (int i=off; true; ++i)
    {
      int c = buf[i];
      if (c == 0) break;
      s.append((char)c);
    }      
    return s.toString();                        
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  /** Reference to null terminated byte array. */
  public byte[] buf;
  
  /** Offset of first char in buf. */
  public int off;
  
}

