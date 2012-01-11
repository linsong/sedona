//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import sedonac.namespace.*;

/**
 * AstVisitor
 */
public class AstVisitor
{

  public void enterType(TypeDef t)  {}
  public void exitType(TypeDef t) {}

  public void enterMethod(MethodDef m) {}
  public void exitMethod(MethodDef m) {}

  public void enterField(FieldDef m) {}
  public void exitField(FieldDef m) {}

  public void enterBlock(Block b) {}
  public void exitBlock(Block b) {}

  public void enterStmt(Stmt s) {}
  public void exitStmt(Stmt s) {}

  public Expr expr(Expr expr) { return expr; }

  public Type type(Type t) { return t; }

  public static final int WALK_TO_TYPES = 1;
  public static final int WALK_TO_SLOTS = 2;
  public static final int WALK_TO_EXPRS = 3;

}
