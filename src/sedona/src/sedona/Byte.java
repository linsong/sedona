//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Jun 07  Brian Frank  Creation
//

package sedona;

import java.io.IOException;

/**
 * Byte represents an unsigned 8-bit byte value
 */
public final class Byte
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Byte make(int val)
  {
    if (val < 0 || val > MAX.val)
      throw new IllegalArgumentException("Valid Byte range [0-"+MAX.val+"]: val = " + val);
    return predefined[val];
  }

  private Byte(int val) { this.val = val; }

  static final Byte[] predefined = new Byte[256];
  static
  {
    for (int i=0; i<predefined.length; ++i)
      predefined[i] = new Byte(i);
  }
  static final Byte ZERO = predefined[0];
  static final Byte MAX  = predefined[255];

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public int typeId()
  {
    return Type.byteId;
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof Byte)
      return val == ((Byte)obj).val;
    return false;
  }

  public int hashCode()
  {
    return val;
  }
  
//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public String encodeString()
  {
    return String.valueOf(val);
  }

  public Value decodeString(String s)
  {
    return make(Integer.decode(s).intValue());
  }

  public void encodeBinary(Buf out)
  {
    out.u1(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.u1());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final int val;
}

