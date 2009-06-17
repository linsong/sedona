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
 * sys::Str native methods
 */
public class Str_n
{                     

  public static void _iInit(StrRef self, int len)
  {
  }

  public static void _sInit()
  {
  }

  public static int get(StrRef self, int index)
  {                                           
    return self.buf[self.off + index] & 0xFF;
  }

  public static void set(StrRef self, int index, int ch)
  {                                                    
    self.buf[self.off + index] = (byte)ch;
  }

  public static byte[] toBytes(StrRef self)        
  {
    byte[] temp = new byte[self.buf.length-self.off];
    System.arraycopy(self.buf, self.off, temp, 0, temp.length);
    return temp;
  }

  public static byte equals(StrRef self, StrRef that)
  {                    
    int ch = 1;
    for (int i=0; ch != 0; ++i)
    {
      ch = get(self, i);
      if (ch != get(that, i)) return 0;
    }
    return 1;
  }    

  public static byte equalsRegion(StrRef self, StrRef that, int start, int end)
  {                                
    int count = end-start;
    for (int i=0; i<count; ++i)
    {             
      int thatIndex = i+start;
      int thisChar = get(self, i);
      int thatChar = get(that, thatIndex);
      if (thisChar != thatChar) return 0;
      if (thisChar == 0) return (byte)(thatChar == 0 ? 1 : 0);
    }
    return 1;
  }                                 
  
  public static byte startsWith(StrRef self, StrRef that)
  {
    int ch = 1;
    for (int i=0; ch != 0; ++i)
    {
      ch = get(self, i);
      int x = get(that, i);
      if (x == 0) return 1;
      if (ch != x) return 0;
    }
    return 0;
  }

  public static int index(StrRef self, int ch)
  {
    int x = -1;
    for (int i=0; x != 0; ++i)
    {
      x = get(self, i);
      if (x == ch) return i;
    }
    return -1;
  }

  public static byte copyFromStr(StrRef self, StrRef from, int max)
  {    
    if (self.off != 0) throw new IllegalStateException();
    for (int i=0; i<max; ++i)
    {
      int ch = get(from, i);
      set(self, i, ch);
      if (ch == 0) return 1;
    }
    set(self, max-1, 0);
    return 0;
  }           

  
  public static int length(StrRef self)
  {
    int len = 0;
    while (get(self, len) != 0) ++len;
    return len;
  }
  
  public static StrRef fromBytes(byte[] buf, int off)
  {                                 
    return new StrRef(buf, off);
  }

  public static StrRef trim(StrRef self)
  {                                   
    // trim front     
    byte[] buf = self.buf;     
    int off = 0;
    while (buf[off] == ' ') ++off;

    // trim back
    int len = off;
    while(buf[len] != 0) len++;
    for (int i=len-1; i>=0; --i)
    {
      if (buf[i] == ' ') buf[i] = 0;
      else break;
    }      
    
    StrRef x;
    if (off == 0) return self;
    return new StrRef(buf, off);
  }

  public static int parseInt(StrRef self)
  {
    int val = 0;
    int i = 0;
    boolean neg = false;

    if (get(self, 0) == '-')
    {
      i++;
      neg = true;
    }

    if (get(self, i) == 0) return -1;

    int c;
    while((c = get(self, i++)) != 0)
    {
      if (c < '0' || c > '9') return -1;
      val = (val*10) + (c-'0');
    }
    if (neg) val = -val;
    return val;    
  }
  
}

