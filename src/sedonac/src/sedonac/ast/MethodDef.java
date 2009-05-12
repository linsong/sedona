//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import sedona.Facets;
import sedonac.*;
import sedonac.namespace.*;

/**
 * MethodDef
 */
public class MethodDef
  extends SlotDef
  implements Method
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public MethodDef(Location loc, TypeDef parent, int flags, String name, 
                   FacetDef[] facets, Type ret, ParamDef[] params, Block code)
  {
    super(loc, parent, flags, name, facets);
    this.ret    = ret;
    this.params = params;
    this.code   = code;
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public boolean isField() { return false; }
  public boolean isMethod() { return true; }

  public Type returnType() { return ret; }

  public Type[] paramTypes()
  {
    Type[] t = new Type[params.length];
    for (int i=0; i<params.length; ++i)
      t[i] = params[i].type;
    return t;
  }

  public Type actionType(Namespace ns) 
  { 
    if (params.length == 0) return ns.voidType;
    return params[0].type;
  }

  public int numParams() { return TypeUtil.numParams(this); }

  public boolean isInstanceInit() { return name.equals(INSTANCE_INIT); }
  public boolean isStaticInit() { return name.equals(STATIC_INIT); }

//////////////////////////////////////////////////////////////////////////
// AstNode
//////////////////////////////////////////////////////////////////////////

  public String toString()
  {
    StringBuffer s = new StringBuffer();
    if (isStatic()) s.append("static ");
    s.append(ret).append(' ').append(name).append('(');
    for (int i=0; i<params.length; ++i)
    {
      if (i > 0) s.append(", ");
      s.append(params[i]);
    }
    return s.append(')').toString();
  }

  public void walk(AstVisitor visitor, int depth)
  {
    visitor.enterMethod(this);

    // facets
    walkFacets(visitor, depth);

    // types
    ret = visitor.type(ret);
    for (int i=0; i<params.length; ++i)
      params[i].walk(visitor);

    // code
    if (depth >= AstVisitor.WALK_TO_EXPRS && code != null)
      code.walk(visitor);

    visitor.exitMethod(this);
  }

  public void write(AstWriter out)
  {
    out.indent().w(this).nl();
    if (code != null) code.write(out);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public ParamDef[] params;
  public Type ret;
  public Block code;
  public int maxLocals;
  public NativeId nativeId;
  public int initStmtIndex = 0;  // used to order iInit statements

}
