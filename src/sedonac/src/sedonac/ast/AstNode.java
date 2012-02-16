//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import sedonac.*;

/**
 * Base class for Abstract syntax tree nodes.
 */
public abstract class AstNode
{

  public AstNode(Location loc)
  {
    this.loc = loc;
  }

  public abstract void write(AstWriter out);

  public void dump()
  {
    AstWriter out = new AstWriter(System.out);
    write(out);
    out.flush();
  }

  public final Location loc;
}
