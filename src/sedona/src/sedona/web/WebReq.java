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
    readHeadersText(in);
  }

  /**
   * Write a request in HTTP text format.
   */
  public void writeText(OutputStream out)
    throws IOException
  {                     
    writeLine(out, method + " " + uri + " HTTP/" + version);
    writeHeadersText(out);
    out.flush();
  }
  
}
