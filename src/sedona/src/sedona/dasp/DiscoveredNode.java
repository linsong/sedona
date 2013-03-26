//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Jan 12  Elizabeth McKenney
//
package sedona.dasp;

import java.net.InetAddress;

/**
 * Encapsulates information from a discovered node.
 * 
 * @author Elizabeth McKenney
 * @creation Jan 25, 2012
 *
 */
public class DiscoveredNode 
{
  DiscoveredNode(InetAddress host, String plat)
  {
    this.addr       = host;
    this.platformId = plat;
  }

  /**
   * Get the address of the host. 
   */
  public InetAddress addr() { return addr; }


  /**
   * Get the platform ID of the host. 
   */
  public String platformId() { return platformId; }


  /**
   * String representation.
   */
  public String toString()
  {
    return "DiscoveredNode : " + addr + " (" + platformId + ")";
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  // Inet address of host
  InetAddress addr;

  // Platform ID of host
  String platformId;

}
