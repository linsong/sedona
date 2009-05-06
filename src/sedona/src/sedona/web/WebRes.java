//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Apr 09  Brian Frank  Creation
//

package sedona.web;

import java.io.*;
import java.net.*;

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
  }

  /**
   * Write a response in HTTP text format.
   */
  public void writeText(OutputStream out)
    throws IOException
  {                     
    writeLine(out, "HTTP/" + version + " " + code + " " + reason);
    writeHeadersText(out);
    out.flush();
  }
  
}
