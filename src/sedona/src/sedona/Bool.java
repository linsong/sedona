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
 * Bool represents a true or false condition.
 */
public final class Bool
  extends Value
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public static final Bool TRUE  = new Bool(true);
  public static final Bool FALSE = new Bool(false);
  public static final Bool NULL  = new Bool(true);

  public static Bool make(boolean val)
  {
    return val ? TRUE : FALSE;
  }

  private Bool(boolean val) { this.val = val; }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public boolean isNull()
  {
    return this == NULL;
  }

  public int typeId()
  {
    return Type.boolId;
  }

  public boolean equals(Object obj)
  {
    return this == obj;
  }
  
  public int hashCode()
  {
    return encodeString().hashCode();
  }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public String encodeString() 
  { 
    if (isNull()) return "null";
    return val ? "true" : "false"; 
  }

  public Value decodeString(String s)
  {
    if (s.equals("true")) return TRUE;
    if (s.equals("false")) return FALSE;
    if (s.equals("null")) return NULL;
if (s.equals("nullbool")) return NULL; // TODO obsolete from 1.0.7+
    throw new IllegalArgumentException("Invalid Bool syntax: " + s);
  }

  public void encodeBinary(Buf out)
  {        
    if (isNull()) out.u1(2);
    else out.u1(val ? 1 : 0);
  }

  public Value decodeBinary(Buf in)
    throws IOException
  {                      
    int x = in.u1();
    if (x == 2) return NULL;
    if (x == 0) return FALSE;
    return TRUE;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final boolean val;
}

