//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 07  Brian Frank  Creation
//

package sedona;

import java.io.*;
import sedona.util.*;

/**
 * Value is the Java representation of a Sedona Obj or
 * primitive which can be used as the value of a slot.
 * Value types are immutable (except Buf - which should be
 * treated as immutable).
 */
public abstract class Value
  extends Obj
{

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Return string encoding.
   */
  public String toString() { return encodeString(); }

  /**
   * Get the type id.
   */
  public abstract int typeId();

  /**
   * Return if two values are equal.
   */
  public abstract boolean equals(Object obj);
  
  /**
   * Return the hash code.
   */
  public abstract int hashCode();

  /**
   * Is this the special value which represents null.
   */
  public boolean isNull() { return false; }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode to string.
   */
  public abstract String encodeString();

  /**
   * Decode to string and return new Value.
   */
  public abstract Value decodeString(String s);

  /**
   * Encode to binary format.
   */
  public abstract void encodeBinary(Buf out);

  /**
   * Decode from binary format and return new Value.
   */
  public abstract Value decodeBinary(Buf in)
    throws IOException;

  /**
   * Encode to Java/C code string.
   */
  public String toCode()
  {
    return encodeString();
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  /**
   * Given a sys type id, return the default Value instance.
   */
  public static Value defaultForType(int typeId)
  {
    switch (typeId)
    {
      case Type.boolId:   return Bool.FALSE;
      case Type.byteId:   return Byte.ZERO;
      case Type.shortId:  return Short.ZERO;
      case Type.intId:    return Int.ZERO;
      case Type.longId:   return Long.ZERO;
      case Type.floatId:  return Float.ZERO;
      case Type.doubleId: return Double.ZERO;
      case Type.bufId:    return new Buf();
      case Type.strId:    return Str.empty;
      default: throw new IllegalStateException("Value.defaultForType(" + typeId + ")");
    }
  }

}

