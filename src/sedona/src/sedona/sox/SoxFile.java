//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Jan 07  Brian Frank  Creation
//

package sedona.sox;

import java.io.*;

import sedona.*;
import sedona.util.*;

/**
 * SoxFile is used to represent the local side of a file
 * being read/written using SoxClient.  There are two
 * read to use implementations - one for java.io.File
 * and another for byte[].
 */
public abstract class SoxFile
{

////////////////////////////////////////////////////////////////
// Factories
////////////////////////////////////////////////////////////////

  /**
   * Make a SoxFile to read/write to a file on the
   * local file system.
   */
  public static SoxFile make(File f)
  {
    return new LocalFile(f);
  }
  
  /**
   * Make a SoxFile to read/write to the given Buf. This Buf remains
   * in memory at all times.
   */
  public static SoxFile make(Buf b)
  {
    return new MemoryFile(b);
  }

////////////////////////////////////////////////////////////////
// Abstract
////////////////////////////////////////////////////////////////

  /**
   * Get the size of the file in bytes.
   */
  public abstract int size();

  /**
   * Open this file for I/O.  Mode is "r" for
   * reading and "w" for writing.
   */
  public abstract void open(String mode)
    throws IOException;

  /**
   * Read n bytes the file's pos into the buffer.
   */
  public abstract void read(int pos, Buf buf, int n)
    throws IOException;

  /**
   * Write n bytes from the buffer to the file's pos.
   */
  public abstract void write(int pos, Buf buf, int n)
    throws IOException;

  /**
   * Close this file.
   */
  public abstract void close();

////////////////////////////////////////////////////////////////
// LocalFile
////////////////////////////////////////////////////////////////

  static class LocalFile extends SoxFile
  {
    LocalFile(File f) { file = f; }

    public int size()
    {
      return (int)file.length();
    }

    public void open(String mode)
      throws IOException
    {
      if (mode.equals("w")) mode = "rw";
      fp = new RandomAccessFile(file, mode);
      if (mode.indexOf('w') >= 0) fp.setLength(0);
    }

    public void read(int pos, Buf buf, int n)
      throws IOException
    {
      fp.seek(pos);
      buf.readFrom(fp, n);
    }

    public void write(int pos, Buf buf, int n)
      throws IOException
    {
      fp.seek(pos);
      buf.writeTo(fp, n);
    }

    public void close()
    {
      try
      {
        if (fp != null)
          fp.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      fp = null;
    }

    File file;
    RandomAccessFile fp;
  }

////////////////////////////////////////////////////////////////
// MemoryFile
////////////////////////////////////////////////////////////////
  
  static class MemoryFile extends SoxFile
  {
    public MemoryFile(Buf b) { this.b = b; }
    
    public void close() { }
    public int size() { return b.size; }

    public void open(String mode) throws IOException
    {
      if (mode.indexOf('w') >= 0) b.clear();
      b.seek(0);
    }

    public void read(int pos, Buf buf, int n) throws IOException
    {
      b.writeTo(buf.getOutputStream(), pos, n);
    }

    public void write(int pos, Buf buf, int n) throws IOException
    {
      b.seek(pos);
      b.readFrom(buf.getInputStream(), n);
    }
    
    Buf b;
  }

}
