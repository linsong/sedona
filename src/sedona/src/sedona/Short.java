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
 * Short represents an unsigned 16-bit byte value
 */
public final class Short
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static Short make(int val)
  {
    if (val < 0 || val > MAX.val)
      throw new IllegalArgumentException("Valid Short range [0-"+MAX.val+"]: val = " + val);
    if (val < predefined.length)
      return predefined[val];
    return new Short(val);
  }

  private Short(int val) { this.val = val; }

  static final Short[] predefined = new Short[256];
  static
  {
    for (int i=0; i<predefined.length; ++i)
      predefined[i] = new Short(i);
  }
  static final Short ZERO = predefined[0];
  static final Short MAX  = new Short(Character.MAX_VALUE);

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public int typeId()
  {
    return Type.shortId;
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof Short)
      return val == ((Short)obj).val;
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
    out.u2(val);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {
    return make(in.u2());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final int val;

}

