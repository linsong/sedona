//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   20 Apr 09  Craig Gemmill  Creation
//

package sedona.web;

import sedona.util.*;

public class CacheItem
{
  public CacheItem(byte[] value, long lastTicks)
  {
    this.value = value;
    this.lastTicks = lastTicks;
    this.staleTicks = 0;
  }

  public boolean stale() { return staleTicks > 0; }
  public void stale(long staleTime) { staleTicks = staleTime; }
  
  public String toString()
  {
    try { return new String(value,"UTF-8") + " ["+lastTicks+"]"; }
    catch (Exception e) { return TextUtil.toHexString(value) + " ["+lastTicks+"]"; }

  }

  public byte[] value;
  public long   lastTicks;
  public long   staleTicks;
}