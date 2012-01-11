//
// This code licensed to public domain
//
// History:
//   6 Apr 02  Brian Frank  Creation
//

package sedona.xml;

/**
 * XText is the XContent element child used to store character
 * data.  XText is used to model both CDATA sections and normal
 * character data.
 */
public final class XText
  extends XContent
{

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////


  /**
   * Create text with specified char buffer and length.
   */
  public XText(char[] data, int length)
  {
    this.data = data;
    this.length = length;
  }

  /**
   * Create text with specified char buffer using
   * length of data.length.
   */
  public XText(char[] data)
  {
    this.data = data;
    this.length = data.length;
  }

  /**
   * Create text with specified String.
   */
  public XText(String string)
  {
    this.string = string;
  }

  /**
   * Create empty text.
   */
  public XText()
  {
    this.data = noData;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  /**
   * Return true if this XText models a CDATA section of data.
   */
  public final boolean isCDATA()
  {
    return cdata;
  }

  /**
   * Set the CDATA flag.
   */
  public final void setCDATA(boolean cdata)
  {
    this.cdata = cdata;
  }

////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////

  /**
   * Get the character data as a string.
   */
  public final String string()
  {
    if (string == null) string = new String(data, 0, length);
    return string;
  }

  /**
   * Get the length of the character data.
   */
  public final int length()
  {
    if (data != null) return length;
    else return string.length();
  }

  /**
   * Get a direct reference to the character data buffer.
   * Only the range of characters from 0 to length()-1 are
   * valid.  Do not modify the buffer directly, rather use
   * the dedicated modification method.
   */
  public final char[] data()
  {
    if (data == null)
    {
      data = string.toCharArray();
      length = data.length;
    }
    return data;
  }

////////////////////////////////////////////////////////////////
// Modification
////////////////////////////////////////////////////////////////

  /**
   * Append the specified character to the end of
   * the character data.
   */
  public final void append(int c)
  {
    if (length+1 > data.length)
    {
      int resize = length*2;
      if (resize < 256) resize = 256;
      char[] temp = new char[resize];
      System.arraycopy(data, 0, temp, 0, length);
      data = temp;
    }

    data[length++] = (char)c;
    string = null;
  }

  /**
   * Append the specified string to the end of
   * the character data.
   */
  public final void append(String s)
  {
    int slen = s.length();
    if (length+slen > data.length)
    {
      int resize = Math.max(length*2, length+slen);
      if (resize < 256) resize = 256;
      char[] temp = new char[resize];
      System.arraycopy(data, 0, temp, 0, length);
      data = temp;
    }

    int off = length;
    for(int i=0; i<slen; ++i)
      data[off+i] = s.charAt(i);
    length += slen;
    string = null;
  }

  /**
   * Append the specified string to the end of
   * the character data.
   */
  public final void append(char[] buf, int off, int len)
  {
    if (length+len > data.length)
    {
      int resize = Math.max(length*2, length+len);
      if (resize < 256) resize = 256;
      char[] temp = new char[resize];
      System.arraycopy(data, 0, temp, 0, length);
      data = temp;
    }

    int myoff = length;
    for(int i=0; i<len; ++i)
      data[myoff+i] = buf[off+i];
    length += len;
    string = null;
  }

  /**
   * Set the character at the specified index.
   * @throws ArrayIndexOutOfBoundsException if index >= length().
   */
  public final void set(int index, int c)
  {
    if (index >= length) throw new ArrayIndexOutOfBoundsException();
    data[index] = (char)c;
    string = null;
  }

  /**
   * Set the length of valid characters in the buffer.
   */
  public final void setLength(int length)
  {
    this.length = length;
    if (length > data.length)
    {
      char[] temp = new char[length];
      System.arraycopy(data, 0, temp, 0, length);
      data = temp;
    }
    string = null;
  }

  /**
   * Make a new cloned copy of this XText instance.
   */
  public final XText copy()
  {
    int len = length;
    XText copy = new XText();
    copy.length = len;
    copy.data   = new char[len];
    copy.string = string;
    copy.cdata  = cdata;
    System.arraycopy(data, 0, copy.data, 0, len);
    return copy;
  }

////////////////////////////////////////////////////////////////
// Formatting
////////////////////////////////////////////////////////////////

  /**
   * Write to the specified XWriter stream.
   */
  public void write(XWriter out)
  {
    int length = this.length;
    char[] data = this.data;

    if (cdata)
    {
      out.w("<![CDATA[");
      if (data == null)
      {
        out.w(string);
      }
      else
      {
        for(int i=0; i<length; ++i)
          out.w(data[i]);
      }
      out.w("]]>");
    }
    else
    {
      if (data == null)
      {
        out.safe(string, false);
      }
      else
      {
        for(int i=0; i<length; ++i)
          out.safe(data[i], false);
      }
    }
  }

  /**
   * Return <code>string()</code>.
   */
  public String toString()
  {
    return string();
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static char[] noData = new char[0];

  char[] data;
  int length;
  String string;
  boolean cdata;

}
