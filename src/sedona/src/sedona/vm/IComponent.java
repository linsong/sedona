//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Nov 08  Brian Frank  Creation
//

package sedona.vm;         

import sedona.*;

/**
 * IComponent is inherited into the sys::Component class.
 */
public interface IComponent
{    

  /**
   * Get the declared type.
   */                     
  public IType type();

  /**
   * Get the component id.
   */                     
  public int id();
      
}

