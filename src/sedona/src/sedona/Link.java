//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Jun 07  Brian Frank  Creation
//

package sedona;

/**
 * Link models a link between two Components.
 */
public class Link
{
  
  public Link(Component fromComp, Slot fromSlot, Component toComp, Slot toSlot)
  {                  
    this.fromCompId = fromComp.id();
    this.fromSlotId = fromSlot.id;
    this.toCompId   = toComp.id();
    this.toSlotId   = toSlot.id;
  }

  public Link(int fromCompId, int fromSlotId, int toCompId, int toSlotId)
  {                  
    this.fromCompId = fromCompId;
    this.fromSlotId = fromSlotId;
    this.toCompId   = toCompId;
    this.toSlotId   = toSlotId;
  }

  public Link()
  {
  }                         
  
  public int hashCode()
  {
    return ((fromCompId << 16) * 66) ^ ((fromSlotId << 16) * 13) ^ 
            (toCompId * 7) ^ toSlotId;
  }

  public boolean equals(Object obj)
  {
    if (!(obj instanceof Link)) return false;
    Link x = (Link)obj;
    return fromCompId == x.fromCompId &&
           fromSlotId == x.fromSlotId &&
           toCompId   == x.toCompId &&
           toSlotId   == x.toSlotId;
  }
  
  public String toString()
  {
    return fromCompId + "." + fromSlotId + "->" + toCompId + "." + toSlotId;
  }

  public static final Link[] none = new Link[0];

  public int fromCompId;
  public int fromSlotId;
  public int toCompId;
  public int toSlotId;

}

