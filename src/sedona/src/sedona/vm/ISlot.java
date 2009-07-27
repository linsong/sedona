//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Nov 08  Brian Frank  Creation
//

package sedona.vm;

/**
 * ISlot is inherited into the sys::Slot class.
 */
public interface ISlot
{    
    
  /**
   * Get the index of the slot.
   */                     
  public int id();

  /**
   * Get the slot name.
   */                     
  public String name();

  /**
   * Get the type id of the slot.
   */                     
  public IType type();

  /**
   * Get the runtime flags.
   */                     
  public int flags();

  /**
   * Get the SlotAccessor object used to efficiently 
   * get/set this field reflectively.
   */                     
  public SlotAccessor accessor();
  
}

