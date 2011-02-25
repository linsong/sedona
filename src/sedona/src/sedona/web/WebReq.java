//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Apr 09  Brian Frank  Creation
//

package sedona.web;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import sedona.util.ArrayUtil;

/**
 * WebReq models a web request.
 */
public class WebReq extends WebMsg
{

////////////////////////////////////////////////////////////////
// Request Line
////////////////////////////////////////////////////////////////

  /**
   * Version string either "1.0" or "1.1".
   * Default is "1.0".
   */
  public String version = "1.0";

  /**
   * HTTP method such as GET normalized upper-case.
   * Default is "GET".
   */
  public String method = "GET";

  /**
   * URI of the request.
   * Default is "/".
   */
  public String uri = "/";

  /**
   * Message Body.
   */
  public byte[] body = null;


////////////////////////////////////////////////////////////////
// Text I/O
////////////////////////////////////////////////////////////////

  /**
   * Read a request in HTTP text format.
   */
  public void readText(InputStream in)
    throws IOException
  {
    String line = readLine(in);
    StringTokenizer st = new StringTokenizer(line," ");
    try
    {
      method = st.nextToken();
      uri = st.nextToken();
      String http = st.nextToken();
      version = http.substring(5);
    }
    catch (NoSuchElementException e)
    {
      throw new IOException("Invalid HTTP Request:"+line);
    }
    readHeadersText(in);

    // read body - in.read() seems to block if no bytes available
    int i = in.available();
    if (i > 0)
    {
      body = new byte[i];
      in.read(body);
    }
  }

  /**
   * Write a request in HTTP text format.
   */
  public void writeText(OutputStream out)
    throws IOException
  {
    writeLine(out, method + " " + uri + " HTTP/" + version);
    writeHeadersText(out);
    if (body != null)
      System.out.println(ArrayUtil.toHex(body, 0, body.length));
    out.flush();
  }


////////////////////////////////////////////////////////////////
// Binary I/O
////////////////////////////////////////////////////////////////

  /**
   * Read a request in UDP binary format.
   */
  public void readBinary(InputStream in)
    throws IOException
  {
    int magic = WebUtil.readU2(in);   // magic
    if (magic != 0x6836) throw new IOException("Bad magic:0x"+Integer.toHexString(magic));
    method = WebUtil.decompressMethod(in.read());
    uri = WebUtil.readStr(in);
    readHeadersBinary(in);

    // read body - in.read() does not seem to block here
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    int i = in.read();
    while (i >= 0)
    {
      b.write(i);
      i = in.read();
    }
    body = b.toByteArray();
  }

  /**
   * Write a request in UDP binary format.
   */
  public void writeBinary(OutputStream out)
    throws IOException
  {
    WebUtil.writeU2(0x6836, out);
    out.write(WebUtil.compressMethod(method));
    WebUtil.writeStr(uri, out);
    writeHeadersBinary(out);
    if (body != null) out.write(body, 0, body.length);
    out.flush();
  }
}
