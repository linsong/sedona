//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * Buffer
 */
public class Buffer
{ 

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  public Buffer()
  {
    this(1024);
  }
  
  public Buffer(int size)
  {
    bytes = new byte[size];
  } 

////////////////////////////////////////////////////////////////
// Encoding
////////////////////////////////////////////////////////////////

  public final int u1(int v)
  {
    int cur = count;
    if (count+1 >= bytes.length) grow(count+1);
    bytes[count++] = (byte)(v & 0xFF);
    return cur;
  }

  public final int u2(int v)
  {
    int cur = count;
    if (count+2 >= bytes.length) grow(count+2);
    bytes[count++] = (byte)((v >>> 8) & 0xFF);
    bytes[count++] = (byte)((v >>> 0) & 0xFF);
    return cur;
  }

  public final int u4(int v)
  {
    int cur = count;
    if (count+4 >= bytes.length) grow(count+4);
    bytes[count++] = (byte)((v >>> 24) & 0xFF);
    bytes[count++] = (byte)((v >>> 16) & 0xFF);
    bytes[count++] = (byte)((v >>>  8) & 0xFF);
    bytes[count++] = (byte)((v >>>  0) & 0xFF);
    return cur;
  }

  public final int u8(long v)
  {
    int cur = count;
    if (count+8 >= bytes.length) grow(count+8);

    bytes[count++] = (byte)((v >>> 56) & 0xFF);
    bytes[count++] = (byte)((v >>> 48) & 0xFF);
    bytes[count++] = (byte)((v >>> 40) & 0xFF);
    bytes[count++] = (byte)((v >>> 32) & 0xFF);

    bytes[count++] = (byte)((v >>> 24) & 0xFF);
    bytes[count++] = (byte)((v >>> 16) & 0xFF);
    bytes[count++] = (byte)((v >>>  8) & 0xFF);
    bytes[count++] = (byte)((v >>>  0) & 0xFF);

    return cur;
  }

  public final void utf(String str)
  {
    int strlen = str.length();
    int utflen = 0;

    for (int i=0 ; i<strlen ; ++i)
    {
      int c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F))
      {
        utflen++;
      }
      else if (c > 0x07FF)
      {
        utflen += 3;
      }
      else
      {
        utflen += 2;
      }
    }

    if (utflen > 65535)
      throw new RuntimeException("Illegal UTF exception");

    if (count+utflen+2 >= bytes.length) grow(count+utflen+2);
    
    bytes[count++] = (byte)((utflen >>> 8) & 0xFF);
    bytes[count++] = (byte)((utflen >>> 0) & 0xFF);
    for (int i=0 ; i<strlen ; ++i)
    {
      int c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F))
      {
        bytes[count++] = (byte)c;
      }
      else if (c > 0x07FF)
      {
        bytes[count++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
        bytes[count++] = (byte)(0x80 | ((c >>  6) & 0x3F));
        bytes[count++] = (byte)(0x80 | ((c >>  0) & 0x3F));
      }
      else
      {
        bytes[count++] = (byte)(0xC0 | ((c >>  6) & 0x1F));
        bytes[count++] = (byte)(0x80 | ((c >>  0) & 0x3F));
      }
    }
  }
  
  public void append(Buffer buffer)
  {
    if (count+buffer.count >= bytes.length) 
      grow(count+buffer.count);
    System.arraycopy(buffer.bytes, 0, bytes, count, buffer.count);
    count += buffer.count;
  }

  public void append(byte[] b)
  {
    if (count+b.length >= bytes.length) 
      grow(count+b.length);
    System.arraycopy(b, 0, bytes, count, b.length);
    count += b.length;
  }

////////////////////////////////////////////////////////////////
// Backpatch
////////////////////////////////////////////////////////////////

  public final void u1(int offset, int v)
  {
    bytes[offset+0] = (byte)(v & 0xFF);
  }

  public final void u2(int offset, int v)
  {
    bytes[offset+0] = (byte)((v >>> 8) & 0xFF);
    bytes[offset+1] = (byte)((v >>> 0) & 0xFF);
  }

  public final void u4(int offset, int v)
  {
    bytes[offset+0] = (byte)((v >>> 24) & 0xFF);
    bytes[offset+1] = (byte)((v >>> 16) & 0xFF);
    bytes[offset+2] = (byte)((v >>>  8) & 0xFF);
    bytes[offset+3] = (byte)((v >>>  0) & 0xFF);
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  byte[] trim()
  {
    byte[] r = new byte[count];
    System.arraycopy(bytes, 0, r, 0, count);
    return r;
  }
  
  private void grow(int capacity)
  {
    int len = Math.max(capacity, bytes.length*2);
    byte[] temp = new byte[len];
    System.arraycopy(bytes, 0, temp, 0, bytes.length);
    bytes = temp;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public byte[] bytes;
  public int count;  
  
}
