//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Nov 08  Brian Frank  Creation
//

package sedona.vm;

/**
 * IKit is inherited into the sys::Kit class.
 */
public interface IKit
{    

  /**
   * Get the kit id.
   */                     
  public int id();
    
  /**
   * Get the kit name.
   */                     
  public String name();
  
  /**
   * Get the kit's type.
   */                     
  public IType[] types();
  
}

