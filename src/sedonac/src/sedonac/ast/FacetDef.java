//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 May 09  Brian Frank  Creation
//

package sedonac.ast;

import java.util.*;
import sedona.Facets;
import sedonac.*;
import sedonac.namespace.*;
import sedonac.ir.*;

/**
 * FacetDef
 */
public class FacetDef
  extends AstNode
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public FacetDef(Location loc, String name, Expr val)
  {
    super(loc);
    this.name = name;
    this.val  = val;
  }

//////////////////////////////////////////////////////////////////////////
// AST Node
//////////////////////////////////////////////////////////////////////////

  public void write(AstWriter out)
  {
    out.w("@").w(name).w("=");
    val.write(out);
  }

  public void walk(AstVisitor visitor, int depth)
  {
    if (depth >= AstVisitor.WALK_TO_EXPRS)
      val = val.walk(visitor);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static final FacetDef[] empty = new FacetDef[0];

  public final String name;
  public Expr val;


}
