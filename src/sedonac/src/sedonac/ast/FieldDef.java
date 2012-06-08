//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Mar 07  Brian Frank  Creation
//

package sedonac.ast;

import sedona.Facets;
import sedonac.*;
import sedonac.namespace.*;

/**
 * FieldDef
 */
public class FieldDef
  extends SlotDef
  implements Field
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public FieldDef(Location loc, TypeDef parent, int flags, String name, 
                  FacetDef[] facets, Type type, Expr init, Expr[] ctorArgs)
  {
    super(loc, parent, flags, name, facets);
    this.type = type;
    this.init = init;
    this.ctorArgs = ctorArgs;
  }

//////////////////////////////////////////////////////////////////////////
// Field
//////////////////////////////////////////////////////////////////////////

  public boolean isField() { return true; }
  public boolean isMethod() { return false; }

  public Type type() { return type; }
  public Expr.Literal define() { return (Expr.Literal)init; }
  public int ctorLengthParam() { return ctorLengthParam; }

//////////////////////////////////////////////////////////////////////////
// AstNode
//////////////////////////////////////////////////////////////////////////

  public String toString()
  {
    StringBuffer s = new StringBuffer();
    if (isStatic()) s.append("static ");
    if (isInline()) s.append("inline ");
    s.append(type);
    if (ctorArgs != null)
    {
      s.append("(");
      for (int i=0; i<ctorArgs.length; ++i)
      {
        if (i > 0) s.append(",");
        s.append(ctorArgs[i]);
      }
      s.append(")");
    }
    s.append(" ").append(name);
    if (init != null) s.append(" = ").append(init);
    if (ctorLengthParam > 0) s.append(" // ctorLengthParam=").append(ctorLengthParam);
    return s.toString();
  }

  public void walk(AstVisitor visitor, int depth)
  {
    visitor.enterField(this);

    walkFacets(visitor, depth);

    type = visitor.type(type);

    if (depth >= AstVisitor.WALK_TO_EXPRS)
    {
      if (init != null)
        init = init.walk(visitor);
      if (ctorArgs != null)
        for (int i=0; i<ctorArgs.length; ++i)
          ctorArgs[i] = ctorArgs[i].walk(visitor);
    }

    visitor.exitField(this);
  }

  public void write(AstWriter out)
  {
    out.indent().w(this).nl();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public Type type;
  public Expr init;
  public Expr[] ctorArgs;

  // if this field is an inline unsized array, then we allow the
  // developer specify the length as one of the constructor parameters;
  // this identifies the parameter index of the length (index 0 is
  // implicit this).
  public int ctorLengthParam = -1;

}
