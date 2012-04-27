//
// Copyright (c) 2011 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
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
public class Buf extends Value
  implements Comparable
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Create a Buf of size {@code len} using the specified byte array for the
   * internal buffer. The position is set to 0 (the beginning of the Buf).
   */
  public Buf(byte[] buf, int len)
  {
    pos = 0;
    size = len;
    bytes = buf;
  }

  /**
   * Create a Buf of size {@code buf.length} using the specified byte array for
   * the internal buffer. The position is set to 0 (the beginning of the Buf).
   */
  public Buf(byte[] buf)
  {
    pos = 0;
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

  public int typeId()
  {
    return Type.bufId;
  }

  public int hashCode()
  {
    final int prime = 31;
    CRC32 crc = new CRC32();
    crc.update(bytes, 0, size);
    long x = crc.getValue();

    int result = prime * (int)(x ^ (x >>> 32));
    result = prime * result + size;
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Buf))
      return false;
    Buf other = (Buf)obj;
    if (size != other.size)
      return false;
    for (int i=0; i<size; ++i)
    {
      if (bytes[i] != other.bytes[i])
        return false;
    }
    return true;
  }

  public int compareTo(Object obj)
  {
    return toString().compareTo(obj.toString());
  }

  /**
   * Get a Base64 encoding of Buf.
   */
  public String encodeString()
  {
    return Base64.encode(trim());
  }

  /**
   * Decode a Buf from its String encoding. The read/write position will be
   * set to 0 in the resulting Buf.
   *
   * @see #encodeString()
   */
  public Value decodeString(String s)
  {
    return new Buf(Base64.decode(s));
  }

  /**
   * Encode this buffer's content into the binary format expected by Sedona,
   * and write it to {@code out}.
   * <pre>
   * u2  : size
   * u1[]: bytes</pre>
   */
  public void encodeBinary(Buf out)
  {
    out.u2(size);
    out.write(bytes, 0, size);
  }

  /**
   * Decode a binary-encoded Sedona Buf that was encoded in the format used by
   * {@link #encodeBinary(Buf}. The {@code in} Buf is assumed to be positioned
   * at the beginning of the encoding.
   */
  public Value decodeBinary(Buf in)
    throws IOException
  {
    int size = in.u2();
    byte[] buf = new byte[size];
    if (in.read(buf) != size) throw new IOException("unexpected end of file " + size);
    return new Buf(buf);
  }

  /**
   * Get a Sedona Buf string reprsentation of this Buf: {@code 0x[<bytes>]}.
   */
  public String toString()
  {
    StringBuffer s = new StringBuffer("0x[");
    for (int i=0; i<size; ++i)
      s.append(TextUtil.byteToHexString(bytes[i] & 0xFF));
    s.append(']');
    return s.toString();
  }

  /**
   * Decodes a String representation of a Sedona Buf: {@code 0x[<bytes>]} and
   * returns the equivalent Buf representation. The position will be set to 0 in
   * the resulting Buf.
   *
   * @param s
   *          a String encoding of a Sedona Buf.
   *
   * @return a Buf representation of the decoded Sedona Buf. The position will
   *         be set to 0.
   */
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
    buf.seek(0);
    return buf;
  }

////////////////////////////////////////////////////////////////
// Buf
////////////////////////////////////////////////////////////////

  /**
   * Get the size of the Buf.  This is the actual number of bytes in the
   * internal byte array.
   */
  public int size()
  {
    return size;
  }

  /**
   * Get the current read/write position. This is the byte offset at which the
   * next read or write will occur.
   *
   * @see #seek(int)
   */
  public int pos()
  {
    return pos;
  }

  /**
   * Set the size and pos back to 0.
   */
  public void clear()
  {
    this.pos  = 0;
    this.size = 0;
    this.bytes = new byte[bytes.length / 2];
  }

  /**
   * Move {@code pos} to the given index for the next read or write. The pos
   * may be set beyond the end of the buffer. Setting the offset beyond the
   * end of the buffer does not change the buffer size.  The buffer size will
   * change only by writing after the pos has been set beyond the end of the
   * buffer.
   */
  public void seek(int pos)
  {
    this.pos = pos;
  }

  /**
   * Truncate the buffer so that it has the given size. If {@code newSize} is
   * not smaller than the current size, this is a no-op.
   * <p>
   * The read/write position is never changed as a result of this operation.
   */
  public void truncate(int newSize)
  {
    if (newSize >= size) return;
    byte[] truncated = new byte[newSize];
    System.arraycopy(bytes, 0, truncated, 0, newSize);
    this.bytes = truncated;
    this.size = newSize;
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
   * to read mode.  It sets pos = 0 and leaves size alone.
   * <p>
   * This is equivalent to {@code seek(0)}
   *
   * @see #seek(int)
   */
  public void flip()
  {
    pos = 0;
  }

  /**
   * Obtain direct access to the internal byte array. The length of the array is
   * not guaranteed to be equal to {@code size()}.
   */
  public byte[] bytes()
  {
    return bytes;
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
   * Convenience for {@link #u1(int)}
   */
  public void write(int b)
  {
    u1(b);
  }

  /**
   * Convenience for {@code write(buf, 0, buf.length)}.
   */
  public void write(byte[] buf)
  {
    write(buf, 0, buf.length);
  }

  /**
   * Writes {@code len} bytes from the specified byte array starting at
   * {@code offset} to the internal buffer starting at the current position.
   */
  public void write(byte[] buf, int offset, int len)
  {
    grow(pos, len);
    System.arraycopy(buf, offset, bytes, pos, len);
    pos += len;
  }

  /**
   * Append the buffer's bytes to to my owner buffer. The position will be moved
   * to the end of the buffer after this operation.
   */
  public void append(Buf that)
  {
    pos = size;
    grow(size, that.size);
    System.arraycopy(that.bytes, 0, bytes, pos, that.size);
    pos += that.size;
  }

  /**
   * Pad the <strong>end of the buffer</strong> to achieve the specified
   * alignment. The position will be moved to the end of the buffer after this
   * operation.
   */
  public void align(int a)
  {
    int rem = size % a;
    if (rem == 0) return;
    pad(a - rem);
  }

  /**
   * Pad the <strong>end of the buffer</strong> with the specified number of
   * zeros. The position will be moved to the end of the buffer after this
   * operation.
   */
  public void pad(int num)
  {
    pos = size;
    grow(size, num);
    for (int i=0; i<num; ++i) bytes[pos++] = 0;
  }

  /**
   * Grow the internal bytes array to have room to write {@code bytesNeeded} at
   * the given position. The buffer size will be set to
   * {@code max(size, (pos + bytesNeeded))}. Thefore, this method should only be
   * called immediately before a write to the buffer.
   */
  private void grow(int pos, int bytesNeeded)
  {
    final int afterWrite = pos + bytesNeeded;
    if (bytes.length < afterWrite)
    {
      byte[] temp = new byte[Math.max(afterWrite, bytes.length*2)];
      System.arraycopy(bytes, 0, temp, 0, bytes.length);
      bytes = temp;
    }
    if (size < afterWrite)
      size = afterWrite;
  }

//////////////////////////////////////////////////////////////////////////
// Data Output
//////////////////////////////////////////////////////////////////////////

  /**
   * Write a bool 1 or 0 byte at the current position.
   */
  public void bool(boolean b)
  {
    u1(b ? 1 : 0);
  }

  /**
   * Write an unsigned byte to the current position.
   */
  public void u1(int v)
  {
    grow(pos, 1);
    bytes[pos++] = (byte)v;
  }

  /**
   * Write an unsigned byte at the specified position.
   */
  public void u1(int pos, int v)
  {
    grow(pos, 1);
    bytes[pos] = (byte)v;
  }

  /**
   * Write a signed byte at the current position.
   */
  public void s1(int v)
  {
    u1(v); // same difference on writes
  }

  /**
   * Write a signed byte at the specified position.
   */
  public void s1(int pos, int v)
  {
    u1(pos, v); // same difference on writes
  }

  /**
   * Convenience for {@code u2(v, this.checkAlignment)}
   */
  public void u2(int v)
  {
    u2(v, checkAlignment);
  }

  /**
   * Write an unsigned short at the current position. Check alignment if
   * requested.
   */
  public void u2(int v, boolean checkAlign)
  {
    u2(pos, v, checkAlign);
    pos += 2;
  }

  /**
   * Convenience for {@code u2(pos, v, this.checkAlignment)}
   */
  public void u2(int pos, int v)
  {
    u2(pos, v, checkAlignment);
  }

  /**
   * Write an unsigned short at the specified position. Check alignment if
   * requested.
   */
  public void u2(int pos, int v, boolean checkAlign)
  {
    if (checkAlign && (pos % 2) != 0)
      throw new RuntimeException("u2 not aligned (" + pos + ")");

    grow(pos, 2);
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
   * Write a signed short to the current position.
   */
  public void s2(int v)
  {
    u2(v); // same difference on writes
  }

  /**
   * Write a signed short at the specified position.
   */
  public void s2(int pos, int v)
  {
    u2(pos, v); // same difference on writes
  }

  /**
   * Write a signed int to the current position.
   */
  public void i4(int v)
  {
    i4(pos, v);
    pos += 4;
  }

  public void i4(int pos, int v)
  {
    if (checkAlignment && (pos % 4) != 0)
      throw new RuntimeException("i4 not aligned (" + pos + ")");

    grow(pos, 4);
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
   * Write a signed long to the current position.
   */
  public void i8(long v)
  {
    i8(pos, v);
    pos += 8;
  }

  /**
   * Write a signed long to specified position.
   */
  public void i8(int pos, long v)
  {
    if (checkAlignment && (size % 8) != 0)
      throw new RuntimeException("i8 not aligned (" + pos + ")");

    grow(pos, 8);
    if (bigEndian)
    {
      bytes[pos+0] = (byte)(v >>> 56);
      bytes[pos+1] = (byte)(v >>> 48);
      bytes[pos+2] = (byte)(v >>> 40);
      bytes[pos+3] = (byte)(v >>> 32);
      bytes[pos+4] = (byte)(v >>> 24);
      bytes[pos+5] = (byte)(v >>> 16);
      bytes[pos+6] = (byte)(v >>> 8);
      bytes[pos+7] = (byte)(v >>> 0);
    }
    else
    {
      bytes[pos+0] = (byte)(v >>> 0);
      bytes[pos+1] = (byte)(v >>> 8);
      bytes[pos+2] = (byte)(v >>> 16);
      bytes[pos+3] = (byte)(v >>> 24);
      bytes[pos+4] = (byte)(v >>> 32);
      bytes[pos+5] = (byte)(v >>> 40);
      bytes[pos+6] = (byte)(v >>> 48);
      bytes[pos+7] = (byte)(v >>> 56);
    }
  }

  /**
   * Write a 32-bit float to the current position.
   */
  public void f4(float v)
  {
    i4(pos, java.lang.Float.floatToIntBits(v));
    pos += 4;
  }

  /**
   * Write a 32-bit float to the specified position.
   */
  public void f4(int pos, float v)
  {
    i4(pos, java.lang.Float.floatToIntBits(v));
  }

  /**
   * Write a 64-bit double to the current position.
   */
  public void f8(double v)
  {
    f8(pos, v);
    pos += 8;
  }

  /**
   * Write a 64-bit double to the specified position.
   */
  public void f8(int pos, double v)
  {
    i8(pos, java.lang.Double.doubleToLongBits(v));
  }

  /**
   * Writes an ASCII null-terminated string.
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
   * Return the number of bytes available based on the current read position and
   * the the buffer length;
   */
  public int available()
  {
    return size - pos;
  }

  /**
   * Peek at the next byt to read without actually changing the read position.
   */
  public int peek() throws IOException
  {
    if (pos >= size) throw new EOFException();
    return bytes[pos] & 0xFF;
  }

  /**
   * Read the byte at the current position. Return -1 if no more bytes.
   */
  public int read() throws IOException
  {
    if (pos >= size) return -1;
    return bytes[pos++] & 0xFF;
  }

  /**
   * Convenience for {@code read(buf, 0, buf.length)}.
   *
   * @return the actual number of bytes read, or -1 if no more bytes.
   * @see #read(byte[], int, int)
   */
  public int read(byte[] buf) throws IOException
  {
    return read(buf, 0, buf.length);
  }

  /**
   * Read {@code len} bytes starting at the current position in the internal
   * buffer into the specified byte array starting at {@code offset}.
   *
   * @return the actual number of bytes read, or -1 if no more bytes.
   */
  public int read(byte[] buf, int offset, int len) throws IOException
  {
    if (len == 0) return 0;
    if (pos >= size) return -1;
    int actual = Math.min(len, available());
    System.arraycopy(bytes, pos, buf, offset, actual);
    pos += actual;
    return actual;
  }

  /**
   * Skip {@code n} bytes in the internal buffer. No error checking is done.
   * This is equivalent to {@code seek(pos + n)}
   *
   * @return the number of bytes skipped.
   * @see #seek(int)
   */
  public int skipBytes(int n)
  {
    pos += n;
    return n;
  }

//////////////////////////////////////////////////////////////////////////
// Data Input
//////////////////////////////////////////////////////////////////////////

  /**
   * Read a bool byte starting at the current position in the internal buffer.
   */
  public boolean bool() throws IOException
  {
    return u1() != 0;
  }

  /**
   * Read a 8-bit unsigned byte value starting at the current position in the
   * internal buffer.
   */
  public int u1() throws IOException
  {
    if (pos >= size) throw new EOFException();
    return bytes[pos++] & 0xFF;
  }

  /**
   * Read a 8-bit signed byte value starting at the current position in the
   * internal buffer.
   */
  public int s1() throws IOException
  {
    return (byte)u1();
  }

  /**
   * Read a 16-bit unsigned byte value starting at the current position in the
   * internal buffer.
   */
  public int u2() throws IOException
  {
    if (bigEndian)
      return (u1() << 8) + u1();
    else
      return u1() + (u1() << 8);
  }

  /**
   * Read a 16-bit signed byte value starting at the current position in the
   * internal buffer.
   */
  public int s2() throws IOException
  {
    if (bigEndian)
      return (short)((u1() << 8) + u1());
    else
      return (short)(u1() + (u1() << 8));
  }

  /**
   * Read a 32-bit signed integer value starting at the current posistion in the
   * internal buffer.
   */
  public int i4() throws IOException
  {
    if (bigEndian)
      return (u1() << 24) + (u1() << 16) +
             (u1() << 8)  + (u1() << 0);
    else
      return (u1() << 0)  + (u1() << 8) +
             (u1() << 16) + (u1() << 24);
  }

  /**
   * Read a 64-bit long value starting at the current position in the internal
   * buffer.
   */
  public long i8() throws IOException
  {
    if (bigEndian)
      return (((long)i4()) << 32) + (i4() & 0xFFFFFFFFL);
    else
      return ((i4() & 0xFFFFFFFFL) + ((long)i4() << 32));
  }

  /**
   * Read a 32-bit float value starting at the current position in the internal
   * buffer.
   */
  public float f4() throws IOException
  {
    return java.lang.Float.intBitsToFloat(i4());
  }

  /**
   * Read a 64-bit float value starting at the current positiong in the internal
   * buffer.
   */
  public double f8() throws IOException
  {
    return java.lang.Double.longBitsToDouble(i8());
  }

  /**
   * Read a null terminated string starting at the current position in the
   * internal buffer.
   */
  public String str() throws IOException
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
   * Read up to {@code len} bytes from the input stream into the internal buffer
   * starting at the current position.
   * <p>
   * If necessary, the size of the buffer will be increased to accommodate
   * reading the specified number of bytes. Therefore, the size of the buffer
   * after the read is <strong>not dependent</strong> on the actual number of
   * bytes read. For example, consider the following code
   *
   * <pre> Buf b = new Buf();
   * b.readFrom(input, 10);</pre>
   *
   * After this code executes, the size of {@code b} will be 10 no matter how
   * many bytes were actually read.
   *
   * @param in
   *          the InputStream to read from.
   * @param len
   *          the maximum number of bytes to read. The actual number of bytes
   *          read might be less.
   * @return the number of bytes actually read.
   */
  public int readFrom(InputStream in, int len) throws IOException
  {
    grow(pos, len);
    int n = 0;
    while (n < len)
    {
      int count = in.read(bytes, pos, len-n);
      if (count < 0) break;
      pos += count;
      n += count;
    }
    return n;
  }

  /**
   * Read the specified number of bytes from the input stream into the internal
   * buffer starting at the current position. If the len passed is less than
   * zero, then this method routes to {@link #readToEnd(InputStream)}
   *
   * @param in
   *          the InputStream to read from.
   * @param len
   *          the exact number of bytes to read. This method will not return
   *          until len bytes have been read, or an IOException occurs.
   *
   * @throws EOFException
   *           if the end of the stream is reached before <code>len</code> bytes
   *           are read.
   */
  public void readFullyFrom(InputStream in, int len) throws IOException
  {
    if (len < 0) { readToEnd(in); return; }
    grow(pos, len);
    int n = 0;
    while (n < len)
    {
      int count = in.read(bytes, pos, len-n);
      if (count < 0) throw new EOFException();
      pos += count;
      n += count;
    }
  }

  /**
   * Read from the specified InputStream into the internal buffer starting at
   * the current position. The stream is read until it returns -1 indicating the
   * end of the input stream.
   */
  public void readToEnd(InputStream in) throws IOException
  {
    int n;
    byte[] buf = new byte[1024];
    while ((n = in.read(buf, 0, 1024)) >= 0)
      write(buf, 0, n);
  }

  /**
   * Write the internal buffer in its entirety to the specified output stream.
   * This method does not change the read/write position.
   */
  public void writeTo(OutputStream out) throws IOException
  {
    out.write(bytes, 0, size);
  }

  /**
   * Write {@code len} bytes of the internal buffer starting at {@code offset}
   * to the specified output stream.
   */
  public void writeTo(OutputStream out, int offset, int len) throws IOException
  {
    if (len > size) throw new IOException("len > internal buffer");
    out.write(bytes, offset, len);
  }

//////////////////////////////////////////////////////////////////////////
// File Utils
//////////////////////////////////////////////////////////////////////////

  /**
   * Read the given file into a Buf. The read/write position will be set to 0
   * in the resulting Buf.
   */
  public static Buf readFrom(File file) throws IOException
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
    buf.seek(0);
    return buf;
  }

  /**
   * Read exactly {@code n} bytes from the given file and write them to this Buf
   * starting at the current position.
   */
  public void readFrom(RandomAccessFile fp, int n) throws IOException
  {
    grow(pos, n);
    fp.readFully(bytes, pos, n);
    pos += n;
  }

  /**
   * Write the internal buffer in its entirety to the given file. This method
   * does not change the read/write position.
   *
   * @see #writeTo(OutputStream)
   */
  public void writeTo(File file) throws IOException
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

  /**
   * Write {@code n} bytes starting at the current position in the Buf to the
   * given file. There must be {@code n} available bytes in the Buf.
   *
   * @see #available()
   */
  public void writeTo(RandomAccessFile fp, int n) throws IOException
  {
    if (pos+n > size)
      throw new IOException("Unexpected end of buf. pos=" + pos + " size=" + size + " n="+n);
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
// Fields
////////////////////////////////////////////////////////////////

  /**
   * Actual number of bytes in the bytes array.
   * <p>
   * <font color='red'>Direct manipulation of this field is strongly discouraged.
   * Public access may be removed in a future release. </font>
   *
   * @see #size()
   * @see #truncate(int)
   */
  public int size;

  /**
   * Internal byte array buffer.
   * <p>
   * <font color='red'>Direct manipulation of this field is strongly discouraged.
   * Public access may be removed in a future release. </font>
   *
   * @see #bytes()
   * @see #trim()
   */
  public byte[] bytes;

  /**
   * Pos is index for next read or write.
   * <p>
   * <font color='red'>Direct manipulation of this field is strongly discouraged.
   * Public access may be removed in a future release. </font>
   *
   * @see #pos()
   * @see #seek(int)
   */
  public int pos;

  /**
   * Flag for big or little endian byte ordering. Defaults to big endian.
   */
  public boolean bigEndian = true;

  /**
   * Flag to assert that multi-byte values are aligned. Defaults to false.
   */
  public boolean checkAlignment = false;
}
