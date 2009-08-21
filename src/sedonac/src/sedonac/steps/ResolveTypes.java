//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   8 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * ResolveTypes maps the AST types into the namespace and walks the
 * AST to resolve UnresolvedTypes to their resolved Type in the
 * namespace.
 */
public class ResolveTypes
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public ResolveTypes(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  ResolveTypes");
    walkAst(WALK_TO_EXPRS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// AstVisitor
//////////////////////////////////////////////////////////////////////////

  public void exitField(FieldDef field)
  {
    super.exitField(field);
    if (field.isConst() && field.type.isArray())
    {
      ArrayType a = (ArrayType)field.type;
      field.type = new ArrayType(a.loc, a.of, a.len, true);
    }
  }

  public Type type(Type type)
  {
    // if array, then resolve base type and potentially
    // array length  if it is a define
    if (type instanceof ArrayType)
    {
      ArrayType array = (ArrayType)type;
      array.of = type(array.of);
      array.len = arrayLength(array.len, array.loc);
      return array;
    }

    // if stub type
    if (type instanceof StubType)
    {
      StubType stub = (StubType)type;
      Type resolved = ns.resolveType(stub.qname);
      if (resolved == null) throw new IllegalStateException("Using StubType: " + stub.qname);
      return resolved;
    }

    // if not unresolved we are done
    if (!(type instanceof UnresolvedType)) return type;

    // attempt to resolve
    UnresolvedType ref = (UnresolvedType)type;
    Type resolved = resolveType(ref.name, ref.loc, true);
    if (resolved != null) return resolved;
    return ref;
  }

  public Expr expr(Expr expr)
  {
    // if an unresolved name with no target, might
    // be a Type name such as Sys.ticks()
    if (expr.id == Expr.NAME)
    {
      Expr.Name nameExpr = (Expr.Name)expr;
      if (nameExpr.target == null)
      {
        Type type = resolveType(nameExpr.name, nameExpr.loc, false);
        if (type != null)
          return new Expr.StaticType(expr.loc, type);
      }
    }
    return expr;
  }

  private Type resolveType(String name, Location loc, boolean reportErr)
  {
    // for right now we match type name to all the
    // types imported from our dependency
    Type[] matches = ns.resolveTypeBySimpleName(name);
    
    // if no matches
    if (matches.length == 0)
    {
      Type qnameType = null;
      if ((name.indexOf("::") > 0) && (qnameType = ns.resolveType(name)) != null)
        return qnameType;
      
      if (reportErr) err("Unknown type '" + name + "'", loc);
      return qnameType;
    }

    // if more than one match
    if (matches.length > 1)
    {
      err("Ambiguous type '" + matches[0] + "' and '" + matches[1] + "'", loc);
      return null;
    }

    return matches[0];
  }

  public ArrayType.Len arrayLength(ArrayType.Len len, Location loc)
  {
    // we only care about unresolved lengths
    if (len instanceof ArrayType.UnresolvedLen)
    {
      String id = ((ArrayType.UnresolvedLen)len).id;
      Field define = resolveDefine(id, loc);
      if (define != null && !define.type().isInt())
        err("Array length define must be int type", loc);
      return new ArrayType.DefineLen(define);
    }
    else
    {
      return len;
    }
  }

  public Field resolveDefine(String id, Location loc)
  {
    Type base = curType;
    String slotName;

    // check if we have a fully qualified define
    int dot = id.indexOf('.');
    if (dot < 0)
    {
      slotName = id;
    }
    else
    {
      slotName = id.substring(dot+1);
      String t = id.substring(0, dot);
      int colon = t.indexOf(':');
      if (colon < 0)
      {
        base = resolveType(t, loc, false);
      }
      else
      {
        base = ns.resolveType(t);
      }
      if (base == null)
      {
        err("Unresolved base type in define '" + id + "'", loc);
        return null;
      }
    }

    // resolve slot
    Slot slot = base.slot(slotName);
    if (slot == null)
    {
      err("Unresolved define '" + id + "'", loc);
      return null;
    }

    // check that it's a field
    if (!(slot instanceof Field))
    {
      err("Expected define field, not method '" + id + "'", loc);
      return null;
    }

    // check that it's a define
    Field field = (Field)slot;
    if (!field.isDefine())
    {
      err("Expected '" + id + "' to be a define", loc);
      return null;
    }

    return field;
  }

}
