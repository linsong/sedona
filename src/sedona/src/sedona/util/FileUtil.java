//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Jan 03  Brian Frank  Creation
//

package sedona.util;

import java.io.*;
import java.util.*;

/**
 * FileUtil provides handy utility methods for files
 * which probably should be built into java.io.File, but
 * never will.
 */
public class FileUtil
{

////////////////////////////////////////////////////////////////
// Naming
////////////////////////////////////////////////////////////////

  /**
   * Get the base name without the extension for the
   * specified file name.  The extension appears after
   * the last '.' in the file name.  Return fileName if
   * no '.' appears in the file name.
   */
  public static String getBase(String fileName)
  {
    if (fileName == null) return null;
    int x = fileName.lastIndexOf('.');
    if (x >= 1)
      return fileName.substring(0, x);
    else
      return fileName;
  }

  /**
   * Get the extension for the specified file name.
   * The extension appears after the last '.' in
   * the file name.  Return null if no '.' appears
   * in the file name.
   */
  public static String getExtension(String fileName)
  {
    if (fileName == null) return null;
    int x = fileName.lastIndexOf('.');
    if (x >= 1)
      return fileName.substring(x+1);
    else
      return null;
  }

////////////////////////////////////////////////////////////////
// IO
////////////////////////////////////////////////////////////////

  /**
   * Read the specified number of bytes from the input
   * stream into a byte array.  The stream is closed.
   */
  public static byte[] read(InputStream in, long size)
    throws IOException
  {
    if (size < 0 || size > Integer.MAX_VALUE)
      throw new IOException("Invalid size " + size);

    int sz = (int)size;
    byte[] buf = new byte[sz];

    int count = 0;
    while (count < sz)
    {
      int n = in.read(buf, count, sz-count);
      if (n < 0)
        throw new IOException("Unexpected EOF");
      count += n;
    }

    in.close();
    return buf;
  }

  /**
   * Read the specified number of bytes off the given input
   * stream to the specified output stream.  This does not
   * close either the input or output stream.
   */
  public static void pipe(InputStream in, long size, OutputStream out)
    throws IOException
  {
    int len = 4096;
    byte[] buf = new byte[len];
    while (size > 0)
    {
      int n = in.read(buf, 0, (int)Math.min(size, len));
      if (n <= 0)
        throw new IOException("Unexpected EOF");
      out.write(buf, 0, n);
      size -= n;
    }
  }

  /**
   * Read the specified number of bytes off the given input
   * stream to the specified output stream until the input
   * stream returns -1.  This does not close either the input
   * or output stream.
   */
  public static void pipe(InputStream in, OutputStream out)
    throws IOException
  {
    int len = 4096;
    byte[] buf = new byte[len];
    while (true)
    {
      int n = in.read(buf, 0, len);
      if (n < 0) break;
      out.write(buf, 0, n);
    }
  }

  /**
   * Read a UTF-8 file into an array of lines.
   */
  public static String[] readLines(File f)
    throws IOException
  {
    return readLines(new FileInputStream(f));
  }

  /**
   * Read a UTF-8 file into an array of lines and close the stream.
   */
  public static String[] readLines(InputStream inputStream)
    throws IOException
  {
    BufferedReader in = null;
    try
    {
      ArrayList lines = new ArrayList();
      in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      String line;
      while ((line = in.readLine()) != null)
        lines.add(line);
      return (String[])lines.toArray(new String[lines.size()]);
    }
    finally
    {
      try { if (in != null) in.close(); } catch (Exception e) {}
    }
  }

////////////////////////////////////////////////////////////////
// File Management
////////////////////////////////////////////////////////////////

  /**
   * Make a directory.
   */
  public static void mkdir(File dir, Log log)
    throws IOException
  {
    if (dir.exists() && dir.isDirectory())
      return;

    if (log != null) log.debug("    MakeDir [" + dir + "]");
    if (!dir.mkdirs())
      throw new IOException("Cannot make directory: " + dir);
  }

  /**
   * Copy a directory or a data file by routing
   * to copyDir() or copyFile().
   */
  public static void copy(File oldFile, File newFile, Log log)
    throws IOException
  {
    if (log != null) log.debug("    Copy [" + oldFile + " -> " + newFile + "]");
    if (newFile.exists())
      throw new IOException("Cannot copy to existing file: " + newFile);
    if (oldFile.isDirectory())
      copyDir(oldFile, newFile, log);
    else
      copyFile(oldFile, newFile);
  }

  /**
   * Copy directory recursively.
   */
  public static void copyDir(File oldFile, File newFile, Log log)
    throws IOException
  {
    if (!newFile.exists() && !newFile.mkdirs())
      throw new IOException("Cannot make dir: " + newFile);

    File[] kids = oldFile.listFiles();
    for (int i=0; i<kids.length; ++i)
      copy(kids[i], new File(newFile, kids[i].getName()), log);
  }

  /**
   * Copy file contents.
   */
  public static void copyFile(File oldFile, File newFile)
    throws IOException
  {
    FileOutputStream out = null;
    FileInputStream in = null;

    try
    {
      out = new FileOutputStream(newFile);
      in = new FileInputStream(oldFile);

      byte[] buf = new byte[4096];
      long size = oldFile.length();
      long copied = 0;
      while (copied < size)
      {
        int n = in.read(buf, 0, buf.length);
        if (n < 0) throw new EOFException("Early EOF in input file");
        out.write(buf, 0, n);
        copied += n;
      }
    }
    finally
    {
      if (in != null) try { in.close(); } catch(IOException e) {}
      if (out != null) try { out.close(); } catch(IOException e) {}
    }
  }

  /**
   * Recursively delete a file or directory.
   */
  public static void delete(File file, Log log)
    throws IOException
  {
    if (!file.exists()) return;

    if (log != null)  log.debug("    Delete [" + file + "]");

    if (file.isDirectory())
    {
      File[] kids = file.listFiles();
      for (int i=0; i<kids.length; ++i)
        delete(kids[i], log);
    }

    if (!file.delete())
      throw new IOException("Cannot delete: " + file);
  }

}
