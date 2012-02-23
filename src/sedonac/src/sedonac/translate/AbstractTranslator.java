//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 07  Brian Frank  Creation
//           

// this code is in a vegetative state

package sedonac.translate;

import java.io.*;
import java.util.*;
import sedona.Env;
import sedona.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * AbstractTranslator
 */
public abstract class AbstractTranslator
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public AbstractTranslator(Compiler compiler, TypeDef type)
  {
    super(compiler);
    this.type = type;
    this.outDir = compiler.translation.outDir;
  }

//////////////////////////////////////////////////////////////////////////
// Translate
//////////////////////////////////////////////////////////////////////////

  public void translate()
    throws IOException
  {
    File file = toFile();
    log.debug("    Translate [" + file + "]");
    out = new PrintWriter(new FileWriter(file));
    try
    {
      header();
      doTranslate();
    }
    finally
    {
      out.close();
    }
  }

  public abstract File toFile();

  public abstract void doTranslate();

//////////////////////////////////////////////////////////////////////////
// Header
//////////////////////////////////////////////////////////////////////////

  public void header()
  {
    w("//").nl();
    w("// sedonac translation").nl();
    w("// " + Env.timestamp()).nl();
    w("//").nl();
    nl();
  }

//////////////////////////////////////////////////////////////////////////
// Statement
//////////////////////////////////////////////////////////////////////////

  public void block(Block block)
  {
    indent().w("{").nl();
    ++indent;
    stmts(block.stmts);
    --indent;
    indent().w("}").nl();
  }

  public void stmts(ArrayList stmts)
  {
    for (int i=0; i<stmts.size(); ++i)
      stmt((Stmt)stmts.get(i));
  }

  public void stmt(Stmt stmt) { stmt(stmt, true); }
  public void stmt(Stmt stmt, boolean standAlone)
  {
    switch (stmt.id)
    {
      case Stmt.EXPR_STMT: exprStmt((Stmt.ExprStmt)stmt, standAlone); break;
      case Stmt.LOCAL_DEF: localDef((Stmt.LocalDef)stmt, standAlone); break;
      case Stmt.RETURN:    returnStmt((Stmt.Return)stmt); break;
      case Stmt.IF:        ifStmt((Stmt.If)stmt); break;
      case Stmt.FOR:       forStmt((Stmt.For)stmt); break;
      case Stmt.WHILE:     whileStmt((Stmt.While)stmt); break;
      case Stmt.BREAK:     breakStmt((Stmt.Break)stmt); break;
      case Stmt.CONTINUE:  continueStmt((Stmt.Continue)stmt); break;
      case Stmt.ASSERT:    assertStmt((Stmt.Assert)stmt); break;
      default: new IllegalStateException(stmt.toString());
    }
  }

  public void exprStmt(Stmt.ExprStmt stmt, boolean standAlone)
  {
    if (standAlone) indent();
    expr(stmt.expr, true);
    if (standAlone) w(";").nl();
  }

  public void localDef(Stmt.LocalDef stmt, boolean standAlone)
  {
    if (standAlone) indent();
    wtype(stmt.type).w(" ").w(stmt.name);
    if (stmt.init != null) { w(" = "); expr(stmt.init, true); }
    if (standAlone) w(";").nl();
  }

  public void returnStmt(Stmt.Return stmt)
  {
    indent().w("return");
    if (stmt.expr != null) { w(" "); expr(stmt.expr, true); }
    w(";").nl();
  }

  public void ifStmt(Stmt.If stmt)
  {
    indent().w("if (");
    expr(stmt.cond, true);
    w(")").nl();
    block(stmt.trueBlock);
    if (stmt.falseBlock != null)
    {
      indent().w("else").nl();
      block(stmt.falseBlock);
    }
  }

  public void forStmt(Stmt.For stmt)
  {
    indent().w("for (");
    if (stmt.init != null) stmt(stmt.init, false);
    w("; ");
    if (stmt.cond != null) expr(stmt.cond, true);
    w("; ");
    if (stmt.update != null) expr(stmt.update, true);
    w(")").nl();
    block(stmt.block);
  }

  public void whileStmt(Stmt.While stmt)
  {
    indent().w("while (");
    expr(stmt.cond, true);
    w(")").nl();
    block(stmt.block);
  }

  public void breakStmt(Stmt.Break stmt)
  {
    indent().w("break;").nl();
  }

  public void continueStmt(Stmt.Continue stmt)
  {
    indent().w("continue;").nl();
  }

  public void assertStmt(Stmt.Assert stmt)
  {
    indent().w("assert(");
    expr(stmt.cond, true);
    w(");").nl();
  }

//////////////////////////////////////////////////////////////////////////
// Expression
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr) { return expr(expr, false); }
  public Expr expr(Expr expr, boolean top)
  {
    switch (expr.id)
    {
      case Expr.TRUE_LITERAL:   trueLiteral(); break;
      case Expr.FALSE_LITERAL:  falseLiteral(); break;
      case Expr.INT_LITERAL:    intLiteral(((Expr.Literal)expr).asInt()); break;
      case Expr.FLOAT_LITERAL:  floatLiteral(((Expr.Literal)expr).asFloat()); break;
      case Expr.NULL_LITERAL:   nullLiteral();  break;
      case Expr.NEGATE:
      case Expr.BIT_NOT:
      case Expr.COND_NOT:       unary((Expr.Unary)expr); break;
      case Expr.PRE_INCR:
      case Expr.PRE_DECR:
      case Expr.POST_INCR:
      case Expr.POST_DECR:      increment((Expr.Unary)expr); break;
      case Expr.COND_OR:
      case Expr.COND_AND:       cond((Expr.Cond)expr, top); break;
      case Expr.EQ:
      case Expr.NOT_EQ:
      case Expr.GT:
      case Expr.GT_EQ:
      case Expr.LT:
      case Expr.LT_EQ:
      case Expr.BIT_OR:
      case Expr.BIT_XOR:
      case Expr.BIT_AND:
      case Expr.LSHIFT:
      case Expr.RSHIFT:
      case Expr.MUL:
      case Expr.DIV:
      case Expr.MOD:
      case Expr.ADD:
      case Expr.SUB:            binary((Expr.Binary)expr, top); break;
      case Expr.ASSIGN:         assign((Expr.Binary)expr, top); break;
      case Expr.TERNARY:        ternary((Expr.Ternary)expr, top); break;
      case Expr.PARAM:          param((Expr.Param)expr); break;
      case Expr.LOCAL:          local((Expr.Local)expr); break;
      case Expr.FIELD:          field((Expr.Field)expr); break;
      case Expr.INDEX:          index((Expr.Index)expr); break;
      case Expr.CALL:           call((Expr.Call)expr); break;
      case Expr.CAST :          cast((Expr.Cast)expr); break;
      case Expr.STATIC_TYPE:    staticType((Expr.StaticType)expr); break;
      default:                  throw err("AbstractTranslator not done: " + expr.id + " " + expr.toString(), expr.loc);
    }
    return expr;
  }

  public void trueLiteral() { w("true"); }
  public void falseLiteral() { w("false"); }
  public void intLiteral(int v) { w(v); }
  public void floatLiteral(float v) { w(Env.floatFormat(v)).w("f"); }
  public void nullLiteral() { w("null"); }

  public void unary(Expr.Unary expr)
  {
    w(expr.op);
    w("(");
    expr(expr.operand);
    w(")");
  }

  public void increment(Expr.Unary expr)
  {
    if (!expr.isPostfix()) w(expr.op);
    w("(");
    expr(expr.operand);
    w(")");
    if (expr.isPostfix()) w(expr.op);
  }

  public void cond(Expr.Cond expr, boolean top)
  {
    if (!top) w("(");
    for (int i=0; i<expr.operands.size(); ++i)
    {
      if (i > 0) w(expr.op);
      expr((Expr)expr.operands.get(i));
    }
    if (!top) w(")");
  }

  public void binary(Expr.Binary expr, boolean top)
  {
    if (!top) w("(");
    expr(expr.lhs);
    w(" ").w(expr.op).w(" ");
    expr(expr.rhs);
    if (!top) w(")");
  }

  public void assign(Expr.Binary expr, boolean top)
  {
    if (!top) w("(");
    expr(expr.lhs);
    w(" ").w(expr.op).w(" ");
    assignNarrow(expr.lhs.type, expr.rhs);
    if (!top) w(")");
  }

  public void assignNarrow(Type lhs, Expr rhs)
  {
    if (lhs.isByte())
    {
      w("(");
      expr(rhs);
      w(" & 0xFF)");
    }
    else if (lhs.isShort())
    {
      w("(");
      expr(rhs);
      w(" & 0xFFFF)");
    }
    else
    {
      expr(rhs);
    }
  }

  public void ternary(Expr.Ternary expr, boolean top)
  {
    if (!top) w("(");
    expr(expr.cond);
    w(" ? ");
    expr(expr.trueExpr);
    w(" : ");
    expr(expr.falseExpr);
    if (!top) w(")");
  }

  public void param(Expr.Param expr)
  {
    w(expr.name);
  }

  public void local(Expr.Local expr)
  {
    // use def b/c we might change name for C
    w(expr.def.name);
  }

  public void field(Expr.Field expr)
  {
    if (expr.target != null)
    {
      expr(expr.target);
      w(".");
    }

    w(expr.name);
  }

  public void index(Expr.Index expr)
  {
    expr(expr.target);
    w("[");
    expr(expr.index);
    w("]");
  }

  public void call(Expr.Call expr)
  {
    if (expr.target != null)
    {
      expr(expr.target);
      w(".");
    }

    w(expr.name);
    callArgs(expr);
  }

  public void callArgs(Expr.Call expr)
  {
    w("(");
    for (int i=0; i<expr.args.length; ++i)
    {
      if (i > 0) w(", ");
      expr(expr.args[i]);
    }
    w(")");
  }

  public void cast(Expr.Cast expr)
  {
    w("((");
    wtype(expr.type);
    w(")");
    expr(expr.target);
    w(")");
  }

  public void staticType(Expr.StaticType expr)
  {
    wtype(expr.type);
  }

//////////////////////////////////////////////////////////////////////////
// Typing
//////////////////////////////////////////////////////////////////////////

  public abstract String toType(Type t);

//////////////////////////////////////////////////////////////////////////
// Write
//////////////////////////////////////////////////////////////////////////

  public AbstractTranslator w(Object s)   { out.print(s); return this; }
  public AbstractTranslator w(int i)      { out.print(i); return this; }
  public AbstractTranslator wtype(Type t) { out.print(toType(t)); return this; }
  public AbstractTranslator indent()      { out.print(TextUtil.getSpaces(indent*2)); return this; }
  public AbstractTranslator nl()          { out.println(); return this; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public TypeDef type;
  public File outDir;
  public PrintWriter out;
  public int indent;

}
