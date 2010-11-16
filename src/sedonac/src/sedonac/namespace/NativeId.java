//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   29 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedonac.*;

/**
 * NativeId models the id used to reference a native method function.
 */
public class NativeId
{

  public static NativeId parse(Location loc, String id)
  {
    try
    {
      int colon = id.indexOf(':');
      if (id.charAt(colon+1) != ':') return null;
      int kitId = Integer.parseInt(id.substring(0, colon));
      int methodId = Integer.parseInt(id.substring(colon+2));
      return new NativeId(loc, kitId, methodId);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public NativeId(Location loc, int kitId, int methodId)
  {
    this.loc = loc;
    this.kitId = kitId;
    this.methodId = methodId;
    this.string = kitId + "::" + methodId;
  }

  public int hashCode()
  {
    return string.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof NativeId)
      return obj.toString().equals(string);
    else
      return false;
  }

  public String toString()
  {
    return string;
  }

  public final Location loc;
  public final int kitId;
  public final int methodId;
  public final String string;

}
