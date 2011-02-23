//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Apr 09  Brian Frank  Creation
//

package sedona.web;

import java.io.*;
import java.util.*;
import sedona.util.*;

/**
 * WebMsg is the common base class for WebReq and WebRes.
 * It handles HTTP headers and provides IO utility methods.
 */
public class WebMsg
{

////////////////////////////////////////////////////////////////
// Headers
////////////////////////////////////////////////////////////////

  /**
   * List the set of header names.
   */
  public String[] list()
  {
    return (String[])headers.keySet().toArray(new String[headers.size()]);
  }

  /**
   * Get a header by case-insensitive name or return null if not mapped.
   */
  public String get(String name)
  {
    return (String)headers.get(name);
  }

  /**
   * Get a header by case-insensitive name or return "def" if not mapped.
   */
  public String get(String name, String def)
  {
    String val = (String)headers.get(name);
    return val != null ? val : def;
  }

  /**
   * Set a header by its case-insensitive name.  Return this.
   */
  public WebMsg set(String name, String val)
  {
    headers.put(name, val);
    return this;
  }

////////////////////////////////////////////////////////////////
// Text I/O
////////////////////////////////////////////////////////////////

  /**
   * Read the headers in HTTP text format.
   */
  public void readHeadersText(InputStream in)
    throws IOException
  {
    headers.clear();
    while (true)
    {
      String line = readLine(in).trim();
      if (line.length() == 0) break;
      int colon = line.indexOf(':');
      String key = line.substring(0, colon).trim();
      String val = line.substring(colon+1).trim();
      set(key, val);
    }
  }

  /**
   * Write the headers in HTTP text format.
   */
  public void writeHeadersText(OutputStream out)
    throws IOException
  {
    Iterator it = headers.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry e = (Map.Entry)it.next();
      writeLine(out, e.getKey() + ": " + e.getValue());
    }
    writeLine(out, "");
  }

////////////////////////////////////////////////////////////////
// Binary I/O
////////////////////////////////////////////////////////////////

  /**
   * Read a request in UDP binary format.
   */
  public void readHeadersBinary(InputStream in)
    throws IOException
  {
    headers.clear();
    int hcode = in.read();
    while (hcode > 0)
    {
      set(WebUtil.decompressHeaderName(hcode), WebUtil.readHeader(hcode, in));
      hcode = in.read();
    }
  }

  /**
   * Write a request in UDP binary format.
   */
  public void writeHeadersBinary(OutputStream out)
    throws IOException
  {
    Iterator it = headers.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry e = (Map.Entry)it.next();
      WebUtil.writeHeader(e.getKey(), e.getValue(), out);
    }
    out.write(0);
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  /**
   * Read an HTTP line terminated by CR LF
   */
  public static String readLine(InputStream in)
    throws IOException
  {
    StringBuffer s = new StringBuffer();
    int last = 0;
    while (true)
    {
      int c = in.read();
      if (c < 0) throw new EOFException();
      if (last == '\r' && c == '\n') break;
      s.append((char)c);
      last = c;
    }
    s.setLength(s.length()-1);
    return s.toString();
  }

  /**
   * Write an HTTP line terminated by CR LF
   */
  public static void writeLine(OutputStream out, String line)
    throws IOException
  {
    for (int i=0; i<line.length(); ++i) out.write(line.charAt(i));
    out.write('\r');
    out.write('\n');
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  private TreeMap headers = new TreeMap(TextUtil.caseInsensitiveComparator);

}
