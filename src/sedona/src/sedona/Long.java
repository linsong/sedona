//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Nov 07  Brian Frank  Creation
//

package sedona;

import java.io.IOException;

/**
 * Long represents an signed 64 bit integer value
 */
public final class Long
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Long make(long val)
  {
    if (0 <= val && val < predefined.length)
      return predefined[(int)val];
    return new Long(val);
  }

  private Long(long val) { this.val = val; }

  static final Long[] predefined = new Long[256];
  static
  {
    for (int i=0; i<predefined.length; ++i)
      predefined[i] = new Long(i);
  }
  static final Long ZERO = predefined[0];

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public int typeId()
  {
    return Type.longId;
  }

  public int hashCode()
  {
   return (int)(val ^ (val >>> 32));
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof Long)
      return val == ((Long)obj).val;
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
    return Long.make(java.lang.Long.decode(s).longValue());
  }

  public void encodeBinary(Buf out)
  {                  
    out.i8(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.i8());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final long val;
}

