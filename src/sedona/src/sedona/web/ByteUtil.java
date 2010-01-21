//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedona.web;

import java.io.PrintWriter;

import sedona.util.TextUtil;

public class ByteUtil
{
  /**
   * Dump a byte array to standard out.
   */
  public static void hexDump(byte[] b)
  {
    if (b == null) return;
    PrintWriter out = new PrintWriter(System.out);
    hexDump(out, b, 0, b.length);
    out.flush();
  }

  /**
   * Dump a slice of a byte array to standard out.
   */
  public static void hexDump(byte[] b, int offset, int length)
  {
    PrintWriter out = new PrintWriter(System.out);
    hexDump(out, b, offset, length);
    out.flush();
  }

  /**
   * Dump a byte array to the given print writer.
   */
  public static void hexDump(PrintWriter out, byte[] b, int offset, int length)
  {
    if (b == null) return;
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
}