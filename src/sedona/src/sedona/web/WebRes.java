//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Apr 09  Brian Frank  Creation
//

package sedona.web;

import java.io.*;

import sedona.util.ArrayUtil;

/**
 * WebRes models a web response.
 */
public class WebRes extends WebMsg
{

////////////////////////////////////////////////////////////////
// Status Line
////////////////////////////////////////////////////////////////

  /**
   * Version of response - either "1.0" or "1.1".
   * Default is "1.0".
   */
  public String version = "1.0";

  /**
   * HTTP status code for response.
   * Default is 200.
   */
  public int code = 200;

  /**
   * HTTP reason phrase which should match response code.
   * Default is "-".
   */
  public String reason = "-";

  /**
   * Message Body.
   */
  public byte[] body = null;


////////////////////////////////////////////////////////////////
// Text I/O
////////////////////////////////////////////////////////////////

  /**
   * Read a response in HTTP text format.
   */
  public void readText(InputStream in)
    throws IOException
  {
    String line = readLine(in);
    if (line.startsWith("HTTP/1.0 ") || line.startsWith("HTTP/1.1"))
      throw new IOException("Invalid HTTP response: " + line);
    this.version  = line.substring(5, 8);
    line = line.substring(8).trim();
    int sp = line.indexOf(' ');
    this.code = Integer.parseInt(line.substring(0, sp));
    this.reason = line.substring(sp+1).trim();
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
   * Write a response in HTTP text format.
   */
  public void writeText(OutputStream out)
    throws IOException
  {
    writeLine(out, "HTTP/" + version + " " + code + " " + reason);
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
    WebUtil.readU2(in);   // magic
    code = WebUtil.decompressStatusCode(in.read());
    reason = WebUtil.httpCodeToReason(code);
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
    WebUtil.writeU2(0x4836, out);
    out.write(WebUtil.compressStatusCode(code));
    writeHeadersBinary(out);
    if (body != null) out.write(body, 0, body.length);
    out.flush();
  }

}
