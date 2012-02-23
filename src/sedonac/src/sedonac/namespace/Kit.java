//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

/**
 * Kit is the interface for classes used to represent Sedona kits
 * at compile time such as IrKit or KitDef.
 *
 * Not to be confused with sedona.Kit used to represent kits
 * in Java at runtime.
 */
public interface Kit
{

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Get globally unique name of kit.
   */
  public String name();

//////////////////////////////////////////////////////////////////////////
// Types
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the list of types defined by kit.
   */
  public Type[] types();

  /**
   * Get a type by its name or return null if no type with specified name.
   */
  public Type type(String name);

}
