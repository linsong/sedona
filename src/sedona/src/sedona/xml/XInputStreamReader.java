//
// This code licensed to public domain
//
// History:
//   19 Apr 05  Brian Frank  Creation
//

package sedona.xml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UTFDataFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * XInputStreamRead is used to read a XML byte stream into a stream
 * of unicode characters.  Mapping the byte stream into a charset
 * encoding is implemented according XML 1.0 Appendix F - Autodetection
 * of Character Encodings.  XInputStream also automatically handles
 * reading a PKZIP zipped XML.  Character encodings supported:
 * <ul>
 *  <li>UTF-16 big-endian with 0xFEFF byte order mark;</li>
 *  <li>UTF-16 little-endian with 0xFFFE byte order mark;</li>
 *  <li>UTF-8 with 0xEFBBBF byte order mark;</li>
 *  <li>PKZIP containing any other format with 0x504b0304 byte
 *      order mark (assumes text is first zip entry)</li>
 *  <li>Anything else assumes UTF-8;</li>
 * </ul>
 */
public class XInputStreamReader
  extends Reader
{

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  /**
   * Construct writer for specified file.
   */
  public XInputStreamReader(InputStream in)
    throws IOException
  {
    if (in.markSupported())
    {
      this.in = in;
    }
    else
    {
      this.in = new BufferedInputStream(in);
    }
  }

////////////////////////////////////////////////////////////////
// Reader
////////////////////////////////////////////////////////////////

  /**
   * Get the character encoding being used.
   */
  public String getEncoding()
    throws IOException
  {
    if (!autoDetected) autoDetect();
    return ENCODINGS[encoding];
  }

  /**
   * Return if the stream was zipped.
   */
  public boolean isZipped()
    throws IOException
  {
    if (!autoDetected) autoDetect();
    return zipped;
  }

  /**
   * Read one character.
   */
  public int read()
    throws IOException
  {
    if (!autoDetected) autoDetect();

    // do our own UTF decoding, since java.io.InputStreamReader
    // is deathly slow (tests indicate it can more than double
    // the time of the rest of the XML parsing combined)
    switch(encoding)
    {
      case UTF_8:
        // handle ASCII 99% case inline for performance
        int c = in.read();
        if (c < 0) return -1;
        if ((c & 0x80) == 0) return c;
        return readUtf8(c);
      case UTF_16_BE:
        return readUtf16be();
      case UTF_16_LE:
        return readUtf16le();
    }

    throw new IllegalStateException();
  }

  /**
   * Read a block of characters into the specified buffer.
   */
  public int read(char[] buf, int off, int len)
    throws IOException
  {
    if (!autoDetected) autoDetect();
    for(int i=0; i<len; ++i)
    {
      int c = read();
      if (c < 0) return i == 0 ? -1 : i;
      buf[off+i] = (char)c;
    }
    return len;
  }

  /**
   * Read a block of characters into the specified buffer.
   */
  public int read(char[] buf)
    throws IOException
  {
    return read(buf, 0, buf.length);
  }

  /**
   * Close the underlying input stream.
   */
  public void close()
    throws IOException
  {
    in.close();
  }

////////////////////////////////////////////////////////////////
// Auto-detect
////////////////////////////////////////////////////////////////

  /**
   * Auto-detect the character encoding.
   */
  private void autoDetect()
    throws IOException
  {
    // read first four bytes and then reset stream
    int[] sig = new int[4];
    in.mark(4);
    sig[0] = in.read();
    sig[1] = in.read();
    sig[2] = in.read();
    sig[3] = in.read();
    in.reset();

    // if first four bytes are 0x504b0304, then this is a PKZIP
    // file and we assume that the first entry is the XML document
    // to be parsed; reset input stream to first entry and then
    // recurse to auto-detect the text stream
    if (match(sig, 0x50, 0x4b, 0x03, 0x04))
    {
      ZipInputStream unzip = new ZipInputStream(in);
      ZipEntry entry = unzip.getNextEntry();
      this.zipped = true;
      this.in = new BufferedInputStream(unzip);
      autoDetect();
      return;
    }

    // check the bytes to look for BOM (byte order mark); we only
    // support UTF-16 with a BOM and UTF-8 with and without BOM
    int encoding;
    if (match(sig, 0xFE, 0xFF))
    {
      in.read(); in.read();
      encoding = UTF_16_BE;
    }
    else if (match(sig, 0xFF, 0xFE))
    {
      in.read(); in.read();
      encoding = UTF_16_LE;
    }
    else if (match(sig, 0xEF, 0xBB, 0xBF))
    {
      in.read(); in.read(); in.read();
      encoding = UTF_8;
    }
    else
    {
      // always fallback to utf-8
      encoding = UTF_8;
    }

    // create the appropiate InputStreamReader with char encoding
    this.encoding = encoding;
    this.autoDetected = true;
  }

  private boolean match(int[] sig, int b0, int b1, int b2, int b3)
  {
    return sig[0] == b0 && sig[1] == b1 && sig[2] == b2 &&  sig[3] == b3;
  }

  private boolean match(int[] sig, int b0, int b1, int b2)
  {
    return sig[0] == b0 && sig[1] == b1 && sig[2] == b2;
  }

  private boolean match(int[] sig, int b0, int b1)
  {
    return sig[0] == b0 && sig[1] == b1;
  }

////////////////////////////////////////////////////////////////
// UTF
////////////////////////////////////////////////////////////////

  private int readUtf8(int c0)
    throws IOException
  {
    // at this point 0xxx xxxx (ASCII) is already handled
    // since it is inlined into read() itself

    int c1, c2, c3;
    switch (c0 >> 4)
    {
      case 12:
      case 13:
        // 110x xxxx   10xx xxxx
        c1 = in.read();
        if ((c1 & 0xC0) != 0x80)
          throw new UTFDataFormatException();
        return ((c0 & 0x1F) << 6) | ((c1 & 0x3F) << 0);
      case 14:
        // 1110 xxxx  10xx xxxx  10xx xxxx
        c1 = in.read();
        c2 = in.read();
        if (((c1 & 0xC0) != 0x80) || ((c2 & 0xC0) != 0x80))
          throw new UTFDataFormatException();
        return ((c0  & 0x0F) << 12) | ((c1 & 0x3F) << 6)  | ((c2 & 0x3F) << 0);
      case 15:
        // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
        /* I think this is valid, but Java doesn't seem to output
           characters this high - so cap things below this
        c1 = in.read();
        c2 = in.read();
        c3 = in.read();
        if (((c1 & 0xC0) != 0x80) || ((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80))
          throw new UTFDataFormatException();
        return ((c0  & 0x07) << 18) | ((c1 & 0x3F) << 12) | ((c2 & 0x3F) << 6)  | ((c3 & 0x3F) << 0);
        */
        throw new UTFDataFormatException();
      default:
        throw new UTFDataFormatException();
    }
  }

  private int readUtf16be()
    throws IOException
  {
    int c0 = in.read();
    int c1 = in.read();
    if (c0 < 0) return -1;
    return ((c0 & 0xFF) << 8) | ((c1 & 0xFF) << 0);
  }

  private int readUtf16le()
    throws IOException
  {
    int c0 = in.read();
    int c1 = in.read();
    if (c0 < 0) return -1;
    return ((c1 & 0xFF) << 8) | ((c0 & 0xFF) << 0);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static final int UTF_8     = 0;
  static final int UTF_16_BE = 1;
  static final int UTF_16_LE = 2;
  static final String[] ENCODINGS =
  {
    "UTF-8",     // 0
    "UTF-16BE",  // 1
    "UTF-16LE",  // 2
  };

  private InputStream in;        // raw InputStream
  private boolean autoDetected;  // have we run autoDetect() yet
  private boolean zipped;        // was stream a zip file
  private int encoding = -1;     // encoding constant

}
