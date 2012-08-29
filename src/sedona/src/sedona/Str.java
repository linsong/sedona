//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 07  Brian Frank  Creation
//

package sedona;

import java.io.IOException;

import sedona.util.TextUtil;

/**
 * Str represents a null terminated ASCII string.
 */
public final class Str
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Str make(String val)
  {
    if (val.length() == 0) return empty;
    return new Str(val);
  }

  private Str(String val) { this.val = val; }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public int typeId()
  {
    return Type.strId;
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof Str)
      return val.equals(((Str)obj).val);
    return false;
  }                 

  public int hashCode()
  {
    return val.hashCode();
  }
  
////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////
  
  /**
   * Are the str contents 7-bit ASCII.
   */
  public boolean isAscii()
  {  
    for (int i=0; i<val.length(); ++i)
    {                   
      int c = val.charAt(i);
      if (c > 0x7f) return false;
    }
    return true;
  }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public String encodeString()
  {
    return TextUtil.toLiteral(val);
  }

  public Value decodeString(String s)
  {
    return make(TextUtil.fromLiteral(s));
  }

  public void encodeBinary(Buf out)
  {
    // same as Buf (assume ASCII for now)
    out.u2(val.length()+1);
    for (int i=0; i<val.length(); ++i)
      out.u1(val.charAt(i));
    out.u1(0);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    int size = in.u2();
    if (size == 0) return empty;
    char[] buf = new char[size-1];
    for (int i=0; i<size-1; ++i)
      buf[i] = (char)in.u1();
    String s = new String(buf);
    if (in.u1() != 0) throw new IOException("Str not null terminated: " + s);
    return make(s);
  }

  public String toCode()
  {
    return '"' + TextUtil.toLiteral(val) + '"';
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  static final Str empty = new Str("");

  public final String val;
}

