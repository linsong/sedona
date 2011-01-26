//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 07  Brian Frank  Creation
//

package sedona;

import java.io.IOException;

/**
 * Int represents an signed 32 bit integer value
 */
public final class Int
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Int make(int val)
  {
    if (0 <= val && val < predefined.length)
      return predefined[val];
    return new Int(val);
  }

  private Int(int val) { this.val = val; }

  static final Int[] predefined = new Int[256];
  static
  {
    for (int i=0; i<predefined.length; ++i)
      predefined[i] = new Int(i);
  }
  static final Int ZERO = predefined[0];

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public int typeId()
  {
    return Type.intId;
  }

  public int hashCode()
  {
    return val;
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof Int)
      return val == ((Int)obj).val;
    return false;
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
    out.i4(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.i4());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final int val;
}

