//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Nov 08  Brian Frank  Creation
//

package sedona.vm;

/**
 * IType is inherited into the sys::Type class.
 */
public interface IType
{    

  /**
   * Get the kit.
   */                     
  public IKit kit();

  /**
   * Get the type id.
   */                     
  public int id();

  /**
   * Get the type name.
   */                     
  public String name();

  /**
   * Get the type base.
   */                     
  public IType base();

  /**
   * Get the list of inherited and declared slots.
   */                     
  public ISlot[] slots();
  
}

