//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import sedonac.*;
import sedonac.namespace.*;

/**
 * ParamDef
 */
public class ParamDef
  extends AstNode
  implements VarDef
{

  public ParamDef(Location loc, int index, Type type, String name)
  {
    super(loc);
    this.index = index;
    this.type = type;
    this.name = name;
  }

  public int index()  { return index; }
  public Type type() { return type; }
  public String name() { return name; }
  public boolean isParam() { return true; }
  public boolean isLocal() { return false; }

  public String toString()
  {
    return type + " " + name;
  }

  public void write(AstWriter out)
  {
    out.w(this);
  }

  public void walk(AstVisitor visitor)
  {
    type = visitor.type(type);
  }

  public int index;
  public Type type;
  public String name;

}
