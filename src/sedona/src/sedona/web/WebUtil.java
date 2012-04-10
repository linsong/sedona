//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Apr 09  Craig Gemmill  Creation
//

package sedona.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class WebUtil
{


////////////////////////////////////////////////////////////////
// I/O
////////////////////////////////////////////////////////////////

  /**
   * Read a null-terminated string from the input stream.
   */
  public static String readStr(InputStream in)
    throws IOException
  {
    StringBuffer sb = new StringBuffer();
    char c;
    do
    {
      c = (char)in.read();
      if (c > 0x7F) throw new IOException("invalid char:"+c);
      if (c > 0)
        sb.append(c);
    } while (c > 0);
    return sb.toString();
  }

  /**
   * Read a two-byte unsigned integer from the input stream.
   */
  public static int readU2(InputStream in)
    throws IOException
  {
    return (in.read() << 8) | in.read();
  }

  /**
   * Write a null-terminated string to the output stream.
   */
  public static void writeStr(String s, OutputStream out)
    throws IOException
  {
    try
    {
      byte[] b = s.getBytes("UTF-8");
      out.write(b, 0, b.length);
    }
    catch (Exception e) { throw new IOException(e.getMessage()); }
    out.write(0);
  }

  /**
   * Write a two-byte unsigned integer to the output stream.
   */
  public static void writeU2(int i, OutputStream out)
    throws IOException
  {
    out.write((i>>8) & 0xFF);
    out.write(i & 0xFF);
  }


////////////////////////////////////////////////////////////////
// Public API
////////////////////////////////////////////////////////////////

  /**
   * Read a compressed header from the input stream and
   * decompress it to the HTTP/1.1 header value.
   * @param hcode the compressed header code
   * @param in    the input stream
   * @returns     the decompressed header value
   * @throws IOException if anything goes wrong
   */
  public static String readHeader(int hcode, InputStream in)
    throws IOException
  {
    Compressor comp = getCompressor(hcode & ~HDR_STR);
    if (comp != null)
    {
      if ((hcode & HDR_STR) != 0)
      {
        return comp.decompress(readStr(in));
      }
      else
      {
        return comp.decompress(readU2(in));
      }
    }
    else
    {
      return readStr(in);
    }
  }

  /**
   * Write an HTTP/1.1 header to the output stream by first
   * compressing it to binary format and using the compressed
   * code if possible.
   * Note: doesn't handle uncompressed u2 headers yet...
   * @param name  the header name
   * @param value the header value
   * @param out   the output stream
   * throws IOException if anything goes wrong
   */
  public static void writeHeader(Object name, Object value, OutputStream out)
    throws IOException
  {
    Compressor comp = getCompressor(name.toString());

    // if no compressor, strip the header
    if (comp == null) return;

    int hc = compressHeaderName(name.toString());
    int ival = -1;
    try { ival = comp.compress(value.toString()); }
    catch (IOException e)
    {
      System.out.println("Cannot compress header: "+name+": "+value+" >>>"+e.toString());
      return;
    }

    if (hc < 0)
    {
      out.write(HDR_STR_UNC);
      writeStr(name.toString(), out);
      writeStr(value.toString(), out);
    }
    if (ival >= 0)
    {
      out.write(hc);
      writeU2(ival, out);
    }
    else
    {
      out.write(hc | HDR_STR);
      writeStr(value.toString(), out);
    }
  }

  /**
   * Compress an HTTP/1.1 header name to the binary code.
   */
  public static int compressHeaderName(String headerName)
  {
    Integer i = (Integer)hdrCodesByName.get(headerName);
    return (i != null) ? i.intValue() : -1;
  }

  /**
   * Decompress a binary coded header to HTTP/1.1.
   */
  public static String decompressHeaderName(int hcode)
  {
    return (String)hdrNamesByCode.get(new Integer(hcode & ~HDR_STR));
  }

  /**
   * Compress an HTTP/1.1 method name to the binary code.
   */
  public static int compressMethod(String mname)
  {
    Integer mcode = (Integer)mCodesByName.get(mname);
    return (mcode != null) ? mcode.intValue() : -1;
  }

  /**
   * Decompress a binary coded method name to HTTP/1.1.
   */
  public static String decompressMethod(int mcode)
  {
    return (String)mNamesByCode.get(new Integer(mcode));
  }

  /**
   * Compress an HTTP/1.1 status code to one byte.
   */
  public static int compressStatusCode(int scode)
  {
    return ((scode / 100) << 5) | ((scode % 100) & 0x1F);
  }

  /**
   * Decompress a one-byte status code to HTTP/1.1.
   */
  public static int decompressStatusCode(int sc)
  {
    sc &= 0xFF;
    return ((sc >> 5) * 100) + (sc & 0x1F);
  }

  /**
   * Convert an HTTP code to the corresponding reason text.
   */
  public static String httpCodeToReason(int code)
  {
    String s = (String)httpReasonsByCode.get(new Integer(code));
    return (s != null) ? s : "-";
  }


////////////////////////////////////////////////////////////////
// Convenience
////////////////////////////////////////////////////////////////

  public static long compressMaxAge(String cacheControl)
    throws IOException
  {
    return MaxAgeCompressor.INST.compress(cacheControl) * 1000L;
  }
  
  public static int compressInt(String val)
    throws IOException
  {
    return IntCompressor.INST.compress(val);
  }
  
  private static Compressor getCompressor(String headerName)
  {
    Compressor c = (Compressor)compsByName.get(headerName);
    return c;
//    return (c != null) ? c : Compressor.INST;
  }

  private static Compressor getCompressor(int headerCode)
  {
    Compressor c = (Compressor)compsByCode.get(new Integer(headerCode));
    return c;
//    return (c != null) ? c : Compressor.INST;
  }


////////////////////////////////////////////////////////////////
// Compressors
////////////////////////////////////////////////////////////////

  static class Compressor
  {
    int compress(String val) throws IOException { return -1; }
    String decompress(int i) throws IOException { return String.valueOf(i); }
    String decompress(String s) throws IOException { return s; }
    public static final Compressor INST = new Compressor();
  }

  static class AvoidCompressor extends Compressor
  {
    int compress(String val)
    {
      System.out.println("Warning: use max-age directive instead!");
      return -1;
    }
    String decompress(int i) { return String.valueOf(i); }
    String decompress(String s) { return s; }
    public static final Compressor INST = new AvoidCompressor();
  }

  static class IntCompressor extends Compressor
  {
    public int compress(String val) throws IOException
    {
      try
      {
        int i = Integer.decode(val).intValue();
        return ((i < 0) || (i > 0xFFFF)) ? -1 : i;
      }
      catch (NumberFormatException e) { throw new IOException(e.getMessage()); }
    }
    public static final IntCompressor INST = new IntCompressor();
  }

  static class MaxAgeCompressor extends Compressor
  {
    int compress(String val) throws IOException
    {
      try
      {
        int ndx = val.indexOf("max-age=");
        if (ndx >= 0)
          return Integer.parseInt(val.substring(ndx+8));
        else
          return -1;
      }
      catch (NumberFormatException e) { throw new IOException(e.getMessage()); }
    }
    String decompress(int i)
    {
      return "max-age="+i;
    }
    public static final Compressor INST = new MaxAgeCompressor();
  }

  static class MimeTypeCompressor extends Compressor
  {
    public int compress(String val)
    {
      int ndx = val.indexOf(",");
      String name = (ndx >= 0) ? val.substring(0,ndx) : val;
      Integer type = (Integer)mimeTypesByName.get(name);
      return (type != null) ? type.intValue() : -1;
    }
    public String decompress(int i)
    {
      String name = (String)mimeTypesByCode.get(new Integer(i));
      return name != null ? name : "";
    }
    public static final Compressor INST = new MimeTypeCompressor();
  }

  static class UnsupportedCompressor extends Compressor
  {
    int compress(String val) throws IOException { throw new IOException("Unsupported Header"); }
    String decompress(int i) throws IOException { throw new IOException("Unsupported Header"); }
    String decompress(String s) throws IOException { throw new IOException("Unsupported Header"); }
    public static final Compressor INST = new UnsupportedCompressor();
  }





////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  private static final int HDR_STR = 0x80;
  private static final int HDR_STR_UNC = 0xFF;
  private static HashMap hdrCodesByName     = new HashMap();
  private static HashMap hdrNamesByCode     = new HashMap();
  private static HashMap mimeTypesByName    = new HashMap();
  private static HashMap mimeTypesByCode    = new HashMap();
  private static HashMap compsByName        = new HashMap();
  private static HashMap compsByCode        = new HashMap();
  private static HashMap mNamesByCode       = new HashMap();
  private static HashMap mCodesByName       = new HashMap();
  private static HashMap httpReasonsByCode  = new HashMap();



////////////////////////////////////////////////////////////////
// Initialization
////////////////////////////////////////////////////////////////

  static
  {
    hdrCodesByName.put("Accept"                 , new Integer(0x01) );
    hdrCodesByName.put("Accept-Charset"         , new Integer(0x02) );
    hdrCodesByName.put("Accept-Encoding"        , new Integer(0x03) );
    hdrCodesByName.put("Accept-Language"        , new Integer(0x04) );
    hdrCodesByName.put("Accept-Ranges"          , new Integer(0x05) );
    hdrCodesByName.put("Age"                    , new Integer(0x06) );
    hdrCodesByName.put("Allow"                  , new Integer(0x07) );
    hdrCodesByName.put("Authorization"          , new Integer(0x08) );
    hdrCodesByName.put("Awake-Time"             , new Integer(0x09) );
    hdrCodesByName.put("Cache-Control"          , new Integer(0x0A) );
    hdrCodesByName.put("Connection"             , new Integer(0x0B) );
    hdrCodesByName.put("Content-Encoding"       , new Integer(0x0C) );
    hdrCodesByName.put("Content-Language"       , new Integer(0x0D) );
    hdrCodesByName.put("Content-Length"         , new Integer(0x0E) );
    hdrCodesByName.put("Content-Location"       , new Integer(0x0F) );
    hdrCodesByName.put("Content-MD5"            , new Integer(0x10) );
    hdrCodesByName.put("Content-Type"           , new Integer(0x11) );
    hdrCodesByName.put("Cookie"                 , new Integer(0x12) );
    hdrCodesByName.put("Date"                   , new Integer(0x13) );
    hdrCodesByName.put("ETag"                   , new Integer(0x14) );
    hdrCodesByName.put("Expect"                 , new Integer(0x15) );
    hdrCodesByName.put("Expires"                , new Integer(0x16) );
    hdrCodesByName.put("From"                   , new Integer(0x17) );
    hdrCodesByName.put("Host"                   , new Integer(0x18) );
    hdrCodesByName.put("If-Match"               , new Integer(0x19) );
    hdrCodesByName.put("If-Modified-Since"      , new Integer(0x1A) );
    hdrCodesByName.put("If-None-Match"          , new Integer(0x1B) );
    hdrCodesByName.put("If-Range"               , new Integer(0x1C) );
    hdrCodesByName.put("If-Unmodified-Since"    , new Integer(0x1D) );
    hdrCodesByName.put("Last-Modified"          , new Integer(0x1E) );
    hdrCodesByName.put("Location"               , new Integer(0x1F) );
    hdrCodesByName.put("Max-Forwards"           , new Integer(0x20) );
    hdrCodesByName.put("Pragma"                 , new Integer(0x21) );
    hdrCodesByName.put("Proxy-Authenticate"     , new Integer(0x22) );
    hdrCodesByName.put("Proxy-Authorization"    , new Integer(0x23) );
    hdrCodesByName.put("Range"                  , new Integer(0x24) );
    hdrCodesByName.put("Referer"                , new Integer(0x25) );
    hdrCodesByName.put("Retry-After"            , new Integer(0x26) );
    hdrCodesByName.put("Server"                 , new Integer(0x27) );
    hdrCodesByName.put("Set-Cookie"             , new Integer(0x28) );
    hdrCodesByName.put("Sleep-Time"             , new Integer(0x29) );
    hdrCodesByName.put("TE"                     , new Integer(0x2A) );
    hdrCodesByName.put("Transaction-Id"         , new Integer(0x2B) );
    hdrCodesByName.put("Trailer"                , new Integer(0x2C) );
    hdrCodesByName.put("Transfer-Encoding"      , new Integer(0x2D) );
    hdrCodesByName.put("Upgrade"                , new Integer(0x2E) );
    hdrCodesByName.put("User-Agent"             , new Integer(0x2F) );
    hdrCodesByName.put("Vary"                   , new Integer(0x30) );
    hdrCodesByName.put("Via"                    , new Integer(0x31) );
    hdrCodesByName.put("Warning"                , new Integer(0x32) );
    hdrCodesByName.put("WWW-Authenticate"       , new Integer(0x33) );

    hdrNamesByCode.put(new Integer(0x01) , "Accept"                 );
    hdrNamesByCode.put(new Integer(0x02) , "Accept-Charset"         );
    hdrNamesByCode.put(new Integer(0x03) , "Accept-Encoding"        );
    hdrNamesByCode.put(new Integer(0x04) , "Accept-Language"        );
    hdrNamesByCode.put(new Integer(0x05) , "Accept-Ranges"          );
    hdrNamesByCode.put(new Integer(0x06) , "Age"                    );
    hdrNamesByCode.put(new Integer(0x07) , "Allow"                  );
    hdrNamesByCode.put(new Integer(0x08) , "Authorization"          );
    hdrNamesByCode.put(new Integer(0x09) , "Awake-Time"             );
    hdrNamesByCode.put(new Integer(0x0A) , "Cache-Control"          );
    hdrNamesByCode.put(new Integer(0x0B) , "Connection"             );
    hdrNamesByCode.put(new Integer(0x0C) , "Content-Encoding"       );
    hdrNamesByCode.put(new Integer(0x0D) , "Content-Language"       );
    hdrNamesByCode.put(new Integer(0x0E) , "Content-Length"         );
    hdrNamesByCode.put(new Integer(0x0F) , "Content-Location"       );
    hdrNamesByCode.put(new Integer(0x10) , "Content-MD5"            );
    hdrNamesByCode.put(new Integer(0x11) , "Content-Type"           );
    hdrNamesByCode.put(new Integer(0x12) , "Cookie"                 );
    hdrNamesByCode.put(new Integer(0x13) , "Date"                   );
    hdrNamesByCode.put(new Integer(0x14) , "ETag"                   );
    hdrNamesByCode.put(new Integer(0x15) , "Expect"                 );
    hdrNamesByCode.put(new Integer(0x16) , "Expires"                );
    hdrNamesByCode.put(new Integer(0x17) , "From"                   );
    hdrNamesByCode.put(new Integer(0x18) , "Host"                   );
    hdrNamesByCode.put(new Integer(0x19) , "If-Match"               );
    hdrNamesByCode.put(new Integer(0x1A) , "If-Modified-Since"      );
    hdrNamesByCode.put(new Integer(0x1B) , "If-None-Match"          );
    hdrNamesByCode.put(new Integer(0x1C) , "If-Range"               );
    hdrNamesByCode.put(new Integer(0x1D) , "If-Unmodified-Since"    );
    hdrNamesByCode.put(new Integer(0x1E) , "Last-Modified"          );
    hdrNamesByCode.put(new Integer(0x1F) , "Location"               );
    hdrNamesByCode.put(new Integer(0x20) , "Max-Forwards"           );
    hdrNamesByCode.put(new Integer(0x21) , "Pragma"                 );
    hdrNamesByCode.put(new Integer(0x22) , "Proxy-Authenticate"     );
    hdrNamesByCode.put(new Integer(0x23) , "Proxy-Authorization"    );
    hdrNamesByCode.put(new Integer(0x24) , "Range"                  );
    hdrNamesByCode.put(new Integer(0x25) , "Referer"                );
    hdrNamesByCode.put(new Integer(0x26) , "Retry-After"            );
    hdrNamesByCode.put(new Integer(0x27) , "Server"                 );
    hdrNamesByCode.put(new Integer(0x28) , "Set-Cookie"             );
    hdrNamesByCode.put(new Integer(0x29) , "Sleep-Time"             );
    hdrNamesByCode.put(new Integer(0x2A) , "TE"                     );
    hdrNamesByCode.put(new Integer(0x2B) , "Transaction-Id"         );
    hdrNamesByCode.put(new Integer(0x2C) , "Trailer"                );
    hdrNamesByCode.put(new Integer(0x2D) , "Transfer-Encoding"      );
    hdrNamesByCode.put(new Integer(0x2E) , "Upgrade"                );
    hdrNamesByCode.put(new Integer(0x2F) , "User-Agent"             );
    hdrNamesByCode.put(new Integer(0x30) , "Vary"                   );
    hdrNamesByCode.put(new Integer(0x31) , "Via"                    );
    hdrNamesByCode.put(new Integer(0x32) , "Warning"                );
    hdrNamesByCode.put(new Integer(0x33) , "WWW-Authenticate"       );

    mimeTypesByName.put("text/plain"                , new Integer(0xB001) );
    mimeTypesByName.put("text/html"                 , new Integer(0xB002) );
    mimeTypesByName.put("text/xml"                  , new Integer(0xB003) );
    mimeTypesByName.put("text/csv"                  , new Integer(0xB004) );
    mimeTypesByName.put("application/octet-stream"  , new Integer(0xA001) );

    mimeTypesByCode.put(new Integer(0xA001) ,  "application/octet-stream"  );
    mimeTypesByCode.put(new Integer(0xB001) ,  "text/plain"                );
    mimeTypesByCode.put(new Integer(0xB002) ,  "text/html"                 );
    mimeTypesByCode.put(new Integer(0xB003) ,  "text/xml"                  );
    mimeTypesByCode.put(new Integer(0xB004) ,  "text/csv"                  );

    compsByName.put("Accept"                 , MimeTypeCompressor.INST );
//    compsByName.put("Accept-Charset"         , Compressor.INST );
//    compsByName.put("Accept-Encoding"        , Compressor.INST );
//    compsByName.put("Accept-Language"        , Compressor.INST );
//    compsByName.put("Accept-Ranges"          , Compressor.INST );
    compsByName.put("Age"                    , IntCompressor.INST );
//    compsByName.put("Allow"                  , Compressor.INST );
//    compsByName.put("Authorization"          , Compressor.INST );
    compsByName.put("Awake-Time"             , IntCompressor.INST );
    compsByName.put("Cache-Control"          , MaxAgeCompressor.INST );
//    compsByName.put("Connection"             , UnsupportedCompressor.INST );
//    compsByName.put("Content-Encoding"       , Compressor.INST );
//    compsByName.put("Content-Language"       , Compressor.INST );
    compsByName.put("Content-Length"         , IntCompressor.INST );
//    compsByName.put("Content-Location"       , Compressor.INST );
//    compsByName.put("Content-MD5"            , Compressor.INST );
    compsByName.put("Content-Type"           , MimeTypeCompressor.INST );
//    compsByName.put("Cookie"                 , Compressor.INST );
//    compsByName.put("Date"                   , Compressor.INST );
    compsByName.put("ETag"                   , IntCompressor.INST );
    compsByName.put("Expect"                 , IntCompressor.INST );
//    compsByName.put("Expires"                , AvoidCompressor.INST );
//    compsByName.put("From"                   , Compressor.INST );
//    compsByName.put("Host"                   , Compressor.INST );
    compsByName.put("If-Match"               , IntCompressor.INST );
//    compsByName.put("If-Modified-Since"      , AvoidCompressor.INST );
    compsByName.put("If-None-Match"          , IntCompressor.INST );
//    compsByName.put("If-Range"               , Compressor.INST );
//    compsByName.put("If-Unmodified-Since"    , AvoidCompressor.INST );
//    compsByName.put("Last-Modified"          , AvoidCompressor.INST );
//    compsByName.put("Location"               , Compressor.INST );
    compsByName.put("Max-Forwards"           , IntCompressor.INST );
//    compsByName.put("Pragma"                 , UnsupportedCompressor.INST );
//    compsByName.put("Proxy-Authenticate"     , Compressor.INST );
//    compsByName.put("Proxy-Authorization"    , Compressor.INST );
//    compsByName.put("Range"                  , Compressor.INST );
//    compsByName.put("Referer"                , Compressor.INST );
    compsByName.put("Retry-After"            , IntCompressor.INST );
//    compsByName.put("Server"                 , Compressor.INST );
//    compsByName.put("Set-Cookie"             , Compressor.INST );
    compsByName.put("Sleep-Time"             , IntCompressor.INST );
//    compsByName.put("TE"                     , Compressor.INST );
    compsByName.put("Transaction-Id"         , IntCompressor.INST );
//    compsByName.put("Trailer"                , UnsupportedCompressor.INST );
//    compsByName.put("Transfer-Encoding"      , Compressor.INST );
//    compsByName.put("Upgrade"                , Compressor.INST );
//    compsByName.put("User-Agent"             , Compressor.INST );
//    compsByName.put("Vary"                   , Compressor.INST );
//    compsByName.put("Via"                    , Compressor.INST );
    compsByName.put("Warning"                , IntCompressor.INST );
//    compsByName.put("WWW-Authenticate"       , Compressor.INST );

    compsByCode.put(new Integer(0x01) , MimeTypeCompressor.INST );
//    compsByCode.put(new Integer(0x02) , Compressor.INST );
//    compsByCode.put(new Integer(0x03) , Compressor.INST );
//    compsByCode.put(new Integer(0x04) , Compressor.INST );
//    compsByCode.put(new Integer(0x05) , Compressor.INST );
    compsByCode.put(new Integer(0x06) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x07) , Compressor.INST );
//    compsByCode.put(new Integer(0x08) , Compressor.INST );
    compsByCode.put(new Integer(0x09) , IntCompressor.INST );
    compsByCode.put(new Integer(0x0A) , MaxAgeCompressor.INST );
//    compsByCode.put(new Integer(0x0B) , UnsupportedCompressor.INST );
//    compsByCode.put(new Integer(0x0C) , Compressor.INST );
//    compsByCode.put(new Integer(0x0D) , Compressor.INST );
    compsByCode.put(new Integer(0x0E) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x0F) , Compressor.INST );
//    compsByCode.put(new Integer(0x10) , Compressor.INST );
    compsByCode.put(new Integer(0x11) , MimeTypeCompressor.INST );
//    compsByCode.put(new Integer(0x12) , Compressor.INST );
//    compsByCode.put(new Integer(0x13) , Compressor.INST );
    compsByCode.put(new Integer(0x14) , IntCompressor.INST );
    compsByCode.put(new Integer(0x15) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x16) , AvoidCompressor.INST );
//    compsByCode.put(new Integer(0x17) , Compressor.INST );
//    compsByCode.put(new Integer(0x18) , Compressor.INST );
    compsByCode.put(new Integer(0x19) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x1A) , AvoidCompressor.INST );
    compsByCode.put(new Integer(0x1B) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x1C) , Compressor.INST );
//    compsByCode.put(new Integer(0x1D) , AvoidCompressor.INST );
//    compsByCode.put(new Integer(0x1E) , AvoidCompressor.INST );
//    compsByCode.put(new Integer(0x1F) , Compressor.INST );
    compsByCode.put(new Integer(0x20) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x21) , UnsupportedCompressor.INST );
//    compsByCode.put(new Integer(0x22) , Compressor.INST );
//    compsByCode.put(new Integer(0x23) , Compressor.INST );
//    compsByCode.put(new Integer(0x24) , Compressor.INST );
//    compsByCode.put(new Integer(0x25) , Compressor.INST );
    compsByCode.put(new Integer(0x26) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x27) , Compressor.INST );
//    compsByCode.put(new Integer(0x28) , Compressor.INST );
    compsByCode.put(new Integer(0x29) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x2A) , Compressor.INST );
    compsByCode.put(new Integer(0x2B) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x2C) , UnsupportedCompressor.INST );
//    compsByCode.put(new Integer(0x2D) , Compressor.INST );
//    compsByCode.put(new Integer(0x2E) , Compressor.INST );
//    compsByCode.put(new Integer(0x2F) , Compressor.INST );
//    compsByCode.put(new Integer(0x30) , Compressor.INST );
//    compsByCode.put(new Integer(0x31) , Compressor.INST );
    compsByCode.put(new Integer(0x32) , IntCompressor.INST );
//    compsByCode.put(new Integer(0x33) , Compressor.INST );

    mNamesByCode.put(new Integer(0x44), "DELETE"    );
    mNamesByCode.put(new Integer(0x47), "GET"       );
    mNamesByCode.put(new Integer(0x48), "HEAD"      );
    mNamesByCode.put(new Integer(0x4F), "OPTIONS"   );
    mNamesByCode.put(new Integer(0x50), "POST"      );
    mNamesByCode.put(new Integer(0x55), "PUT"       );
    mNamesByCode.put(new Integer(0x54), "TRACE"     );

    mCodesByName.put("DELETE"   , new Integer(0x44) );
    mCodesByName.put("GET"      , new Integer(0x47) );
    mCodesByName.put("HEAD"     , new Integer(0x48) );
    mCodesByName.put("OPTIONS"  , new Integer(0x4F) );
    mCodesByName.put("POST"     , new Integer(0x50) );
    mCodesByName.put("PUT"      , new Integer(0x55) );
    mCodesByName.put("TRACE"    , new Integer(0x54) );

    httpReasonsByCode.put(new Integer(200), "OK"                      );
    httpReasonsByCode.put(new Integer(202), "Accepted"                );
    httpReasonsByCode.put(new Integer(204), "No Content"              );
    httpReasonsByCode.put(new Integer(301), "Moved Permanently"       );
    httpReasonsByCode.put(new Integer(304), "Not Modified"            );
    httpReasonsByCode.put(new Integer(307), "Temporary Redirect"      );
    httpReasonsByCode.put(new Integer(400), "Bad Request"             );
    httpReasonsByCode.put(new Integer(401), "Unauthorized"            );
    httpReasonsByCode.put(new Integer(404), "Not Found"               );
    httpReasonsByCode.put(new Integer(408), "Request Timeout"         );
    httpReasonsByCode.put(new Integer(500), "Internal Server Error"   );
    httpReasonsByCode.put(new Integer(501), "Not Implemented"         );
    httpReasonsByCode.put(new Integer(502), "Bad Gateway"             );
    httpReasonsByCode.put(new Integer(504), "Gateway Timeout"         );

  }




////////////////////////////////////////////////////////////////
// Test
////////////////////////////////////////////////////////////////

  private static int verifyCount = 0;
  private static void verify(Object o1, Object o2)
  { if (!o1.equals(o2)) throw new RuntimeException(o1 + " != " + o2); verifyCount++; }
  private static void verify(int i1, int i2)
  { if (i1 != i2) throw new RuntimeException(i1 + " != " + i2); verifyCount++; }

  public static void main(String[] args)
  {
d("hdrCodesByName:");
Iterator it = hdrCodesByName.keySet().iterator();
while (it.hasNext())
{
  String k = (String)it.next();
  d(k+" --> "+hdrCodesByName.get(k));
}

    // method
    verify(compressMethod("DELETE")    , 0x44);
    verify(compressMethod("GET")       , 0x47);
    verify(compressMethod("HEAD")      , 0x48);
    verify(compressMethod("OPTIONS")   , 0x4F);
    verify(compressMethod("POST")      , 0x50);
    verify(compressMethod("PUT")       , 0x55);
    verify(compressMethod("TRACE")     , 0x54);
    verify(decompressMethod(0x44) , "DELETE" );
    verify(decompressMethod(0x47) , "GET"    );
    verify(decompressMethod(0x48) , "HEAD"   );
    verify(decompressMethod(0x4F) , "OPTIONS");
    verify(decompressMethod(0x50) , "POST"   );
    verify(decompressMethod(0x55) , "PUT"    );
    verify(decompressMethod(0x54) , "TRACE"  );

    // status code
    verify(compressStatusCode(200)      , 0x40      );
    verify(decompressStatusCode(0x40)   , 200       );
    verify(compressStatusCode(400)      , 0x80      );
    verify(decompressStatusCode(0x80)   , 400       );
    verify(compressStatusCode(404)      , 0x84      );
    verify(decompressStatusCode(0x84)   , 404       );
    verify(compressStatusCode(202)      , 0x42      );
    verify(decompressStatusCode(0x42)   , 202       );
    verify(compressStatusCode(100)      , 0x20      );
    verify(decompressStatusCode(0x20)   , 100       );
    verify(compressStatusCode(500)      , 0xA0      );
    verify(decompressStatusCode(0xA0)   , 500       );

    // mime type
//    ByteArrayOutputStream out = new ByteArrayOutputStream();
//    writeHeader("accept", "application/octet-stream,text/plain,text/html", out);
//    byte[] b = out.toByteArray();

    try
    {
      int chn = compressHeaderName("Accept");
      d("chn="+chn);
      verify(chn, 0x01);
      Compressor comp = getCompressor("Accept");
      int c = comp.compress("application/octet-stream,text/plain,text/html");
      d("c="+c);
      verify(c, 0xA001);
    }
    catch (Exception e) { e.printStackTrace(); }

    System.out.println("total verifies: "+verifyCount);
  }

  private static final void d(Object o) { System.out.println(o); }
}