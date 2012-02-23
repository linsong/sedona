//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   20 Apr 09  Craig Gemmill  Creation
//

package sedona.web;

import java.util.HashMap;

public class Cache
{
  public synchronized CacheItem get(String uri)
  {
    return (CacheItem)table.get(uri);
  }
  
  public synchronized void put(String uri, CacheItem item)
  {
    table.put(uri, item);
  }
//  public synchronized byte[] get(String uri, long maxAge, long now)
//  {
//    // Get the cache item.
//    d("Cache.get('"+uri+"', "+maxAge+", "+now+")");
//    CacheItem item = (CacheItem)table.get(uri);
//    d("item="+item);
//
//    // If it exists, check its age
//    if (item != null)
//    {
//      long life = item.lastTicks + maxAge - now;
//      d("item.lastTicks="+item.lastTicks+", plus maxAge "+maxAge
//        +"->expiration="+(item.lastTicks + maxAge)+", now="+now+"; remaining life="+(life<0 ? " 0!!" : ""+life));
//      if ((item.lastTicks + maxAge) < now)
//      {
//        // Too old
//        d("too old! - remove item from cache...");
//        remove(uri);
//        return null;
//      }
//      else
//      {
//        // Still fresh
//        d("age ok! - returning item value '"+s(item.value)+"'");
//        return item.value;
//      }
//    }
//    else
//    {
//      // No cache item
//      d("no cached item for uri '"+uri+"'");
//      return null;
//    }
//  }
//
//  public synchronized void put(String uri, byte[] value, long lastTicks)
//  {
//    table.put(uri, new CacheItem(value, lastTicks));
//  }

  public synchronized void remove(String uri)
  {
    table.remove(uri);
  }

//  private String s(byte[] o)
//  {
//    try { return new String(o, "UTF-8"); }
//    catch (Exception e) { return TextUtil.toHexString(o); }
//  }
//  private static boolean debug = true;
//  private static void d(Object o) { if (debug) System.out.println(o); }
  private HashMap table = new HashMap();
}