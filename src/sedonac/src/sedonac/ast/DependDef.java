//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 Mar 07  Brian Frank  Creation
//

package sedonac.ast;

import sedona.*;
import sedonac.*;

/**
 * Depend models a kit dependency
 */
public class DependDef
{

  public DependDef(Location loc, Depend depend)
  {
    this.loc = loc;
    this.depend = depend;
  }

  public String toString()
  {
    return depend.toString();
  }

  public final Location loc;
  public final Depend depend;

}
