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
 * Double represents a 64-bit float value
 */
public final class Double
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Double make(double val)
  {
    if (val == 0.0) return ZERO;
    if (java.lang.Double.isNaN(val)) return NULL;
    return new Double(val);
  }

  private Double(double val) { this.val = val; }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public boolean isNull()
  {
    return java.lang.Double.isNaN(val);
  }

  public int typeId()
  {
    return Type.doubleId;
  }

  public int hashCode()
  {
    long hash = java.lang.Double.doubleToLongBits(val);
    return (int)(hash ^ (hash >>> 32));
  }

  public boolean equals(Object obj)
  {                  
    if (obj instanceof Double)
      return equals(val, ((Double)obj).val);
    return false;
  }

  public static boolean equals(double a, double b)
  {
    if (java.lang.Double.isNaN(a))
      return java.lang.Double.isNaN(b);
    else
      return a == b;
  }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public String encodeString()
  {                        
    if (isNull()) return "null";
    return Env.doubleFormat(val);
  }

  public Value decodeString(String s)
  {
    if (s.equals("null")) return NULL;
    return make(java.lang.Double.parseDouble(s));
  }

  public void encodeBinary(Buf out)
  {
    out.f8(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.f8());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static Double NULL = new Double(java.lang.Double.NaN);
  public static Double ZERO = new Double(0.0);

  public final double val;
}

