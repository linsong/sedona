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
 * Float represents a 32-bit float value
 */
public final class Float
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Float make(float val)
  {
    if (val == 0f) return ZERO;
    if (java.lang.Float.isNaN(val)) return NULL;
    return new Float(val);
  }

  private Float(float val) { this.val = val; }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public boolean isNull()
  {
    return java.lang.Float.isNaN(val);
  }

  public int typeId()
  {
    return Type.floatId;
  }

  public int hashCode()
  {
    return java.lang.Float.floatToIntBits(val);
  }

  public boolean equals(Object obj)
  {                  
    if (obj instanceof Float)
      return equals(val, ((Float)obj).val);
    return false;
  }

  public static boolean equals(float a, float b)
  {
    if (java.lang.Float.isNaN(a))
      return java.lang.Float.isNaN(b);
    else
      return a == b;
  }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public String encodeString()
  {                        
    if (isNull()) return "null";
    return Env.floatFormat(val);
  }

  public Value decodeString(String s)
  {
    if (s.equals("null")) return NULL;
if (s.equals("nullfloat")) return NULL;  // TODO obsolete 1.0.7+
    return Float.make(java.lang.Float.parseFloat(s));
  }

  public void encodeBinary(Buf out)
  {
    out.f4(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.f4());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static Float NULL = new Float(java.lang.Float.NaN);
  public static Float ZERO = new Float(0f);

  public final float val;
}

