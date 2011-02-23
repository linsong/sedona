//
// Copyright (c) 2001 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Jan 01  Brian Frank  Creation
//

package sedona;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.zip.CRC32;

import sedona.util.Base64;
import sedona.util.TextUtil;


/**
 * Buf is a dynamically growable byte array which is designed
 * to provide the same semantics as Buf.sedona, InStream.sedona
 * and OutStream.sedona.
 *
 * NOTE: if the Buf is being used as a Property value, then you
 * you should treat it as immutable.
 */
public class Buf
  extends Value   
  implements Comparable
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Create a Buf of size <code>len</code> using
   * the specified byte array for the internal buffer.
   */
  public Buf(byte[] buf, int len)
  {
    size = len;
    bytes = buf;
  }

  /**
   * Create a Buf of size <code>buf.length</code>
   * using the specified byte array for the internal buffer.
   */
  public Buf(byte[] buf)
  {
    size = buf.length;
    bytes = buf;
  }

  /**
   * Create a buffer with the specified inital capacity.
   */
  public Buf(int initialCapacity)
  {
    bytes = new byte[initialCapacity];
  }

  /**
   * Create a buffer with an inital capacity of 256 bytes.
   */
  public Buf()
  {
    this(256);
  }

//////////////////////////////////////////////////////////////////////////
// Value
//////////////////////////////////////////////////////////////////////////

  public int typeId() { return Type.bufId; }
  
  public int hashCode()
  { 
    CRC32 crc = new CRC32();
    crc.update(bytes, 0, size);
    long x = crc.getValue();
    return (int)(x ^ (x >>> 32));
  }

  public boolean equals(Object obj)
  {
    if (!(obj instanceof Buf)) return false;
    Buf x = (Buf)obj;
    if (size != x.size) return false;
    for (int i=0; i<size; ++i)
      if (bytes[i] != x.bytes[i]) return false;
    return true;
  }               
  
  public int compareTo(Object obj)
  {
    return toString().compareTo(obj.toString());
  }

  public String encodeString()
  {
    return Base64.encode(trim());
  }

  public Value decodeString(String s)
  {
    return new Buf(Base64.decode(s));
  }

  public void encodeBinary(Buf out)
  {
    out.u2(size);
    out.write(bytes, 0, size);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    int size = in.u2();
    byte[] buf = new byte[size];
    if (in.read(buf) != size) throw new IOException("unexpected end of file " + size);
    return new Buf(buf);
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer("0x[");
    for (int i=0; i<size; ++i)
      s.append(TextUtil.byteToHexString(bytes[i] & 0xFF));
    s.append(']');
    return s.toString();
  }                   
  
  public static Buf fromString(String s)
  {
    if (!s.startsWith("0x[") || !s.endsWith("]"))
      throw new IllegalArgumentException("Not a Buf literal: " + s);
    
    Buf buf = new Buf();
    for (int i=3; i<s.length()-1; ++i)
    {
      int c = s.charAt(i);
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n') continue;
      int hi = TextUtil.hexCharToInt((char)c);
      int lo = TextUtil.hexCharToInt(s.charAt(++i));
      buf.write((hi << 4) | lo);
    }            
    return buf;
  }

////////////////////////////////////////////////////////////////
// Buf
////////////////////////////////////////////////////////////////

  /**
   * Set the size and pos back to 0.
   */
  public void clear()
  {
    this.pos = 0;
    this.size = 0;
  }

  /**
   * Move the current read position index.
   */
  public void seek(int pos)
  {
    this.pos = pos;
  }

  /**
   * Get the byte at the specified index.
   */
  public int get(int index)
  {
    if (index >= size) throw new ArrayIndexOutOfBoundsException(index);
    return bytes[index] & 0xff;
  }

  /**
   * Flip is used to transition a buf from write mode
   * in read mode.  It sets pos = 0 and leaves size alone.
   */
  public void flip()
  {
    pos = 0;
  }

  /**
   * Get a copy of the byte array sized to actual size.
   */
  public byte[] trim()
  {
    byte[] copy = new byte[size];
    System.arraycopy(bytes, 0, copy, 0, size);
    return copy;
  }

//////////////////////////////////////////////////////////////////////////
// Basic Output
//////////////////////////////////////////////////////////////////////////

  /**
   * Write the specified byte to the internal buffer.
   */
  public void write(int b)
  {
    grow(size+1);
    bytes[size++] = (byte)b;
  }

  /**
   * Write the specified array to the internal buffer.
   */
  public void write(byte[] buf)
  {
    write(buf, 0, buf.length);
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array
   * starting at <code>offset</code> to the internal buffer.
   */
  public void write(byte[] buf, int offset, int len)
  {
    grow(size+len);
    System.arraycopy(buf, offset, bytes, size, len);
    size += len;
  }

  /**
   * Append the buffer's bytes to my own buffer.
   */
  public void append(Buf that)
  {
    grow(size+that.size);
    System.arraycopy(that.bytes, 0, bytes, size, that.size);
    size += that.size;
  }

  /**
   * Pad the end of the buffer to achieve specified alignment.
   */
  public void align(int a)
  {
    int rem = size % a;
    if (rem == 0) return;
    pad(a - rem);
  }

  /**
   * Pad the end of the buffer with the specified number of zeros.
   */
  public void pad(int num)
  {
    grow(size+num);
    for (int i=0; i<num; ++i) bytes[size++] = 0;
  }

  /**
   * Ensure the interal array has enough capacity for
   * newSize, if not then resize the array.
   */
  private void grow(int newSize)
  {
    if (bytes.length < newSize)
    {
      newSize = Math.max(newSize, bytes.length*2);
      byte[] temp = new byte[newSize];
      System.arraycopy(bytes, 0, temp, 0, bytes.length);
      bytes = temp;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Data Output
//////////////////////////////////////////////////////////////////////////

  /**
   * Write an bool 1 or 0 byte to the end of the buffer.
   */
  public void bool(boolean b)
  {
    u1(b ? 1 : 0);
  }

  /**
   * Write an unsigned byte to the end of the buffer.
   */
  public void u1(int v)
  {
    grow(size+1);
    bytes[size++] = (byte)v;
  }

  /**
   * Write an unsigned byte at specified position.
   */
  public void u1(int pos, int v)
  {
    bytes[pos] = (byte)v;
  }

  /**
   * Write a signed byte to the end of buffer.
   */
  public void s1(int v)
  {
    u1(v);  // same difference on writes
  }

  /**
   * Write a signed byte at specified position.
   */
  public void s1(int pos, int v)
  {
    u1(pos, v);  // same difference on writes
  }

  /**
   * Write an unsigned short to the end of buffer.
   */
  public void u2(int v) { u2(v, checkAlignment); }
  public void u2(int v, boolean checkAlign)
  {
    if (checkAlign && (size % 2) != 0)
      throw new RuntimeException("u2 not aligned (" + size + ")");
       
    grow(size+2);
    if (bigEndian)
    {
      bytes[size++] = (byte)(v >>> 8);
      bytes[size++] = (byte)(v >>> 0);
    }
    else
    {
      bytes[size++] = (byte)(v >>> 0);
      bytes[size++] = (byte)(v >>> 8);
    }
  }

  /**
   * Write an unsigned short at specified position.
   */
  public void u2(int pos, int v) { u2(pos, v, checkAlignment); }
  public void u2(int pos, int v, boolean checkAlign)
  {
    if (checkAlign && (pos % 2) != 0)
      throw new RuntimeException("u2 not aligned (" + pos + ")");
      
    // backpatch
    if (bigEndian)
    {
      bytes[pos+0] = (byte)(v >>> 8);
      bytes[pos+1] = (byte)(v >>> 0);
    }
    else
    {
      bytes[pos+0] = (byte)(v >>> 0);
      bytes[pos+1] = (byte)(v >>> 8);
    }
  }

  /**
   * Write a signed short to the end of buffer.
   */
  public void s2(int v)
  {
    u2(v);  // same difference on writes
  }

  /**
   * Write a signed short at specified position.
   */
  public void s2(int pos, int v)
  {
    u2(pos, v);  // same difference on writes
  }

  /**
   * Write a signed int to the end of buffer.
   */
  public void i4(int v)
  {
    if (checkAlignment && (size % 4) != 0)
      throw new RuntimeException("i4 not aligned (" + size + ")");
      
    grow(size+4);
    if (bigEndian)
    {
      bytes[size++] = (byte)(v >>> 24);
      bytes[size++] = (byte)(v >>> 16);
      bytes[size++] = (byte)(v >>> 8);
      bytes[size++] = (byte)(v >>> 0);
    }
    else
    {
      bytes[size++] = (byte)(v >>> 0);
      bytes[size++] = (byte)(v >>> 8);
      bytes[size++] = (byte)(v >>> 16);
      bytes[size++] = (byte)(v >>> 24);
    }
  }

  /**
   * Write a signed int at specified position.
   */
  public void i4(int pos, int v)
  {
    if (checkAlignment && (pos % 4) != 0)
      throw new RuntimeException("i4 not aligned (" + pos + ")");
      
    // backpatch
    if (bigEndian)
    {
      bytes[pos+0] = (byte)(v >>> 24);
      bytes[pos+1] = (byte)(v >>> 16);
      bytes[pos+2] = (byte)(v >>> 8);
      bytes[pos+3] = (byte)(v >>> 0);
    }
    else
    {
      bytes[pos+0] = (byte)(v >>> 0);
      bytes[pos+1] = (byte)(v >>> 8);
      bytes[pos+2] = (byte)(v >>> 16);
      bytes[pos+3] = (byte)(v >>> 24);
    }
  }

  /**
   * Write a signed long to the end of buffer.
   */    
  public void i8(long v)
  {
    if (checkAlignment && (size % 8) != 0)
      throw new RuntimeException("i8 not aligned");
      
    grow(size+8);
    if (bigEndian)
    {
      bytes[size++] = (byte)(v >>> 56);
      bytes[size++] = (byte)(v >>> 48);
      bytes[size++] = (byte)(v >>> 40);
      bytes[size++] = (byte)(v >>> 32);
      bytes[size++] = (byte)(v >>> 24);
      bytes[size++] = (byte)(v >>> 16);
      bytes[size++] = (byte)(v >>> 8);
      bytes[size++] = (byte)(v >>> 0);
    }
    else
    {                      
      bytes[size++] = (byte)(v >>> 0);
      bytes[size++] = (byte)(v >>> 8);
      bytes[size++] = (byte)(v >>> 16);
      bytes[size++] = (byte)(v >>> 24);
      bytes[size++] = (byte)(v >>> 32);
      bytes[size++] = (byte)(v >>> 40);
      bytes[size++] = (byte)(v >>> 48);
      bytes[size++] = (byte)(v >>> 56);
    }
  }

  /**
   * Write a 32-bit float to the end of buffer.
   */
  public void f4(float v)
  {
    i4(java.lang.Float.floatToIntBits(v));
  }

  /**
   * Write a 64-bit double to the end of buffer.
   */
  public void f8(double v)
  {
    i8(java.lang.Double.doubleToLongBits(v));
  }

  /**
   * Write an ASCII null terminated string.
   */
  public void str(String s)
  {
    for (int i=0; i<s.length(); ++i)
    {
      int c = s.charAt(i);
      if (c > 0x7f) throw new IllegalStateException("Not ASCII string: " + s);
      u1(c);
    }
    u1(0);
  }

////////////////////////////////////////////////////////////////
// Basic Input
////////////////////////////////////////////////////////////////

  /**
   * Return the number of bytes available based on the
   * current read position and the buffer length.
   */
  public int available()
  {
    return size - pos;
  }

  /**
   * Peek at the next byte to read without
   * actually changing the read position.
   */
  public int peek()
    throws IOException
  {
    if (pos >= size) throw new EOFException();
    return bytes[pos] & 0xFF;
  }

  /**
   * Read the next byte from the internal buffer.
   * Return -1 if no more bytes.
   */
  public int read()
    throws IOException
  {
    if (pos >= size) return -1;
    return bytes[pos++] & 0xFF;
  }

  /**
   * Read <code>buf.length</code> bytes from the internal
   * buffer into the specified byte array.  Return -1 if no
   * more bytes.
   */
  public int read(byte[] buf)
    throws IOException
  {
    return read(buf, 0, buf.length);
  }

  /**
   * Read <code>len</code> bytes from the internal buffer
   * into the specified byte array at <code>offset</code>.
   * Return -1 if no more bytes.
   */
  public int read(byte[] buf, int offset, int len)
    throws IOException
  {
    if (len == 0) return 0;
    if (pos >= size) return -1;
    int actual = Math.min(len, size-pos);
    System.arraycopy(bytes, pos, buf, offset, actual);
    pos += actual;
    return actual;
  }

  /**
   * Skip <code>n</code> bytes in the internal buffer.
   */
  public int skipBytes(int n)
    throws IOException
  {
    pos += n;
    return n;
  }

//////////////////////////////////////////////////////////////////////////
// Data Input
//////////////////////////////////////////////////////////////////////////

  /**
   * Read a bool byte from the internal buffer.
   */
  public boolean bool()
    throws IOException
  {
    return u1() != 0;
  }

  /**
   * Read a 8-bit unsigned byte value value from the internal buffer.
   */
  public int u1()
    throws IOException
  {
    if (pos >= size) throw new EOFException();
    return bytes[pos++] & 0xFF;
  }

  /**
   * Read a 8-bit signed byte value value from the internal buffer.
   */
  public int s1()
    throws IOException
  {
    return (byte)u1();
  }

  /**
   * Read a 16-bit unsigned byte value from the internal buffer.
   */
  public int u2()
    throws IOException
  {
    if (bigEndian)
      return (u1() << 8) + u1();
    else
      return u1() + (u1() << 8);
  }

  /**
   * Read a 16-bit signed byte value from the internal buffer.
   */
  public int s2()
    throws IOException
  {
    if (bigEndian)
      return (short)((u1() << 8) + u1());
    else
      return (short)(u1() + (u1() << 8));
  }

  /**
   * Read a 32-bit integer value from the internal buffer.
   */
  public int i4()
    throws IOException
  {
    if (bigEndian)
      return (u1() << 24) + (u1() << 16) +
             (u1() << 8) + (u1() << 0);
    else
      return (u1() << 0) + (u1() << 8) +
             (u1() << 16) + (u1() << 24);
  }

  /**
   * Read a 64-bit long value from the internal buffer.
   */
  public long i8()
    throws IOException
  {
    if (bigEndian)
      return (((long)i4()) << 32) + (i4() & 0xFFFFFFFFL);
    else
      return ((i4() & 0xFFFFFFFFL) + ((long)i4() << 32));
  }

  /**
   * Read a 32-bit float value from the internal buffer.
   */
  public float f4()
    throws IOException
  {
    return java.lang.Float.intBitsToFloat(i4());
  }

  /**
   * Read a 64-bit float value from the internal buffer.
   */
  public double f8()
    throws IOException
  {
    return java.lang.Double.longBitsToDouble(i8());
  }

  /**
   * Read a null terminated string.
   */
  public String str()
    throws IOException
  {
    StringBuffer s = new StringBuffer();
    int c;
    while ((c = u1()) != 0)
      s.append((char)c);
    return s.toString();
  }

////////////////////////////////////////////////////////////////
// IO Stream Utils
////////////////////////////////////////////////////////////////

  /**
   * Get a new InputStream for reading the internal buffer.
   */
  public InputStream getInputStream()
  {
    return new InputStream()
    {
      public final int available() throws IOException { return Buf.this.available(); }
      public final int read() throws IOException { return Buf.this.read(); }
      public final int read(byte[] buf, int off, int len) throws IOException { return Buf.this.read(buf, off, len); }
    };
  }

  /**
   * Get a new OutputStream for the writing to the internal buffer.
   */
  public OutputStream getOutputStream()
  {
    return new OutputStream()
    {
      public final void write(int v) { Buf.this.write(v); }
      public final void write(byte[] buf, int off, int len) { Buf.this.write(buf, off, len); }
    };
  }

  /**
   * Read the specified number of bytes from the input
   * stream into the internal buffer.
   *
   * @return the number of bytes actually read.
   */
  public int readFrom(InputStream in, int len)
    throws IOException
  {
    grow(size+len);
    int n = 0;
    while(n < len)
    {
      int count = in.read(bytes, pos+n, len-n);
      if (count < 0) break;
      n += count;
    }
    size += len;
    return n;
  }

  /**
   * Read the specified number of bytes from the input
   * stream into the internal buffer.  If the len
   * passed is less than zero, then this method routes
   * to readToEnd().
   *
   * @throws EOFException if the end of the stream
   *    is reached before <code>len</code> bytes
   *    are read.
   */
  public void readFullyFrom(InputStream in, int len)
    throws IOException
  {
    if (len < 0) { readToEnd(in); return; }
    grow(len);
    int n = 0;
    while(n < len)
    {
      int count = in.read(bytes, pos+n, len-n);
      if (count < 0)
        throw new EOFException();
      n += count;
    }
    size += len;
  }

  /**
   * Read from the specified InputStream into the internal
   * buffer.  The stream is read until it returns -1 indicating
   * the end of the input stream.
   */
  public void readToEnd(InputStream in)
    throws IOException
  {
    int n;
    byte[] buf = new byte[1024];
    while((n = in.read(buf, 0, 1024)) >= 0)
      write(buf, 0, n);
  }

  /**
   * Write the internal buffer in its entirety to the
   * specified output stream.
   */
  public void writeTo(OutputStream out)
    throws IOException
  {
    out.write(bytes, 0, size);
  }

  /**
   * Write <code>len</code> bytes of the internal buffer
   * starting at <code>offset</code> to the specified output
   * stream.
   */
  public void writeTo(OutputStream out, int offset, int len)
    throws IOException
  {
    if (len > size) throw new IOException("len > internal buffer");
    out.write(bytes, offset, len);
  }

//////////////////////////////////////////////////////////////////////////
// File Utils
//////////////////////////////////////////////////////////////////////////

  public static Buf readFrom(File file)
    throws IOException
  {
    int len = (int)file.length();
    Buf buf = new Buf(len);
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    try
    {
      buf.readFullyFrom(in, len);
    }
    finally
    {
      in.close();
    }
    return buf;
  }

  public void writeTo(File file)
    throws IOException
  {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try
    {
      writeTo(out);
    }
    finally
    {
      out.close();
    }
  }

  public void readFrom(RandomAccessFile fp, int n)
    throws IOException
  {
    grow(size+n);
    fp.readFully(bytes, size, n);
    size += n;
  }

  public void writeTo(RandomAccessFile fp, int n)
    throws IOException
  {
    if (pos+n > size) throw new IOException("Unexpected end of buf pos=" + pos + " size=" + size + " n=" + n);
    fp.write(bytes, pos, n);
    pos += n;
  }

////////////////////////////////////////////////////////////////
// Debugging
////////////////////////////////////////////////////////////////

  /**
   * Dump the byte buffer to standard output.
   */
  public void dump()
  {
    PrintWriter out = new PrintWriter(System.out);
    hexDump(out, bytes, 0, size);
    out.flush();
  }

  /**
   * Dump the byte buffer to a String.
   */
  public String dumpToString()
  {
    StringWriter sout = new StringWriter();
    PrintWriter out = new PrintWriter(sout);
    hexDump(out, bytes, 0, size);
    out.flush();
    return sout.toString();
  }

  /**
   * Dump a byte array to the given print writer.
   */
  public static void hexDump(PrintWriter out, byte[] b, int offset, int length)
  {
    int rowLen = 0;
    byte[] row = new byte[16];

    for(int i=0; i<length; i += rowLen)
    {
      // get the row
      rowLen = Math.min(16, length-i);
      System.arraycopy(b, offset+i, row, 0, rowLen);

      // print buffer offset
      String off = Integer.toHexString(i+offset);
      out.print( TextUtil.padLeft(off, 3) );
      out.print(':');

      // print in hex
      for(int j=0; j<16; ++j)
      {
        if (j % 4 == 0) out.print(' ');
        if (j >= rowLen) out.print("  ");
        else out.print( TextUtil.byteToHexString(row[j] & 0xFF) );
      }
      out.print("  ");

      // print in ascii
      for(int j=0; j<rowLen; ++j)
        out.print( TextUtil.byteToChar(row[j] & 0xFF, '.') );
      out.println();
    }
  }

////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////

  /** Actual number of bytes used in the bytes array. */
  public int size;

  /** Byte array buffer. */
  public byte[] bytes;

  /** Pos is used to store the next read index */
  public int pos;

  /** Flag for big or little endian byte ordering */
  public boolean bigEndian = true;

  /** Flag to assert that multi-byte values are aligned */
  public boolean checkAlignment = false;

}
