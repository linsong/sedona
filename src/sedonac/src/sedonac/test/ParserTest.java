//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.Compiler;

/**
 * ParserTest
 */
public class ParserTest
  extends Test
{

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public void testExpr()
  {
    // literals
    verifyLiteral(expr("true"),   Expr.TRUE_LITERAL,  Boolean.TRUE);
    verifyLiteral(expr("false"),  Expr.FALSE_LITERAL, Boolean.FALSE);
    verifyLiteral(expr("0xab"),   Expr.INT_LITERAL,   new Integer(0xab));
    verifyLiteral(expr("2.4"),    Expr.FLOAT_LITERAL, new Float(2.4f));
    verifyLiteral(expr("\"hi\""), Expr.STR_LITERAL,   "hi");
    verifyLiteral(expr("null"),   Expr.NULL_LITERAL,  null);

    // name
    verifyName(expr("foo"), "foo");
    verifyName(verifyName(expr("foo.bar"), "bar"), "foo");
    verifyName(verifyName(verifyName(expr("foo.bar.roo"), "roo"), "bar"), "foo");

    // unary
    verifyName(verifyUnary(expr("-x"), Expr.NEGATE),     "x");
    verifyName(verifyUnary(expr("!x"), Expr.COND_NOT),   "x");
    verifyName(verifyUnary(expr("~x"), Expr.BIT_NOT),    "x");
    verifyName(verifyUnary(expr("++x"), Expr.PRE_INCR),  "x");
    verifyName(verifyUnary(expr("--x"), Expr.PRE_DECR),  "x");
    verifyName(verifyUnary(expr("x++"), Expr.POST_INCR), "x");
    verifyName(verifyUnary(expr("x--"), Expr.POST_DECR), "x");

    // binary
    verifyBinary(expr("a = b"),  Expr.ASSIGN);
    verifyBinary(expr("a == b"), Expr.EQ);
    verifyBinary(expr("a != b"), Expr.NOT_EQ);
    verifyBinary(expr("a > b"),  Expr.GT);
    verifyBinary(expr("a >= b"), Expr.GT_EQ);
    verifyBinary(expr("a < b"),  Expr.LT);
    verifyBinary(expr("a <= b"), Expr.LT_EQ);
    verifyBinary(expr("a | b"),  Expr.BIT_OR);
    verifyBinary(expr("a ^ b"),  Expr.BIT_XOR);
    verifyBinary(expr("a & b"),  Expr.BIT_AND);
    verifyBinary(expr("a << b"), Expr.LSHIFT);
    verifyBinary(expr("a >> b"), Expr.RSHIFT);
    verifyBinary(expr("a * b"),  Expr.MUL);
    verifyBinary(expr("a / b"),  Expr.DIV);
    verifyBinary(expr("a % b"),  Expr.MOD);
    verifyBinary(expr("a + b"),  Expr.ADD);
    verifyBinary(expr("a - b"),  Expr.SUB);

    // cond
    verifyCond(expr("a || b"), Expr.COND_OR);
    verifyCond(expr("a && b"), Expr.COND_AND);
    verifyCond(expr("a && b && c"), Expr.COND_AND);
    verifyCond(expr("a || b || c || d"), Expr.COND_OR);

    // ternary
    Expr.Ternary ternary = (Expr.Ternary)expr("c ? t : f");
    verifyEq(ternary.id, Expr.TERNARY);
    verifyName(ternary.cond, "c");
    verifyName(ternary.trueExpr, "t");
    verifyName(ternary.falseExpr, "f");

    // call
    verifyCall(expr("c()"), "c", new String[] {});
    verifyCall(expr("c(a)"), "c", new String[] {"a"});
    verifyCall(expr("c(a, b)"), "c", new String[] {"a", "b"});
    verifyCall(expr("c(a, b, c)"), "c", new String[] {"a", "b", "c"});
    verifyCall(verifyName(expr("c(a, b).foo"), "foo"), "c", new String[] {"a", "b"});
    verifyCall(verifyCall(verifyCall(expr("x().y().z()"), "z", null), "y", null), "x", null);

    // index
    Expr.Index index = (Expr.Index)expr("x[i][j]");
    verifyEq(index.id, Expr.INDEX);
    verifyName(index.index,  "j");
    verifyEq(index.target.id, Expr.INDEX);
    verifyName(((Expr.Index)index.target).target, "x");
    verifyName(((Expr.Index)index.target).index, "i");

    // precedence
    Expr.Cond top       = (Expr.Cond)expr("a+b*c|d&&e*(f+g)");
    Expr.Binary abcd    = (Expr.Binary)top.operands.get(0);
      Expr.Binary abc   = (Expr.Binary)abcd.lhs;
        Expr.Name a     = (Expr.Name)abc.lhs;   verifyName(a, "a");
        Expr.Binary bc  = (Expr.Binary)abc.rhs;
          Expr.Name b   = (Expr.Name)bc.lhs;    verifyName(b, "b");
          Expr.Name c   = (Expr.Name)bc.rhs;    verifyName(c, "c");
      Expr.Name d       = (Expr.Name)abcd.rhs;  verifyName(d, "d");
    Expr.Binary efg     = (Expr.Binary)top.operands.get(1);
      Expr.Name e       = (Expr.Name)efg.lhs;   verifyName(e, "e");
      Expr.Binary fg    = (Expr.Binary)efg.rhs;
        Expr.Name f     = (Expr.Name)fg.lhs;    verifyName(f, "f");
        Expr.Name g     = (Expr.Name)fg.rhs;    verifyName(g, "g");
  }

  void verifyLiteral(Expr expr, int id, Object value)
  {
    Expr.Literal x = (Expr.Literal)expr;
    verifyEq(x.id, id);
    verifyEq(x.value, value);
  }

  Expr verifyName(Expr expr, String name)
  {
    Expr.Name x = (Expr.Name)expr;
    verifyEq(x.id, Expr.NAME);
    verifyEq(x.name, name);
    return x.target;
  }

  Expr verifyUnary(Expr expr, int id)
  {
    Expr.Unary x = (Expr.Unary)expr;
    verifyEq(x.id, id);
    return x.operand;
  }

  void verifyBinary(Expr expr, int id)
  {
    Expr.Binary x = (Expr.Binary)expr;
    verifyEq(x.id, id);
    verifyName(x.lhs, "a");
    verifyName(x.rhs, "b");
  }

  void verifyCond(Expr expr, int id)
  {
    Expr.Cond x = (Expr.Cond)expr;
    verifyEq(x.id, id);
    for (int i=0; i<x.operands.size(); ++i)
      verifyName((Expr)x.operands.get(i), "" + (char)('a'+i));
  }

  Expr verifyCall(Expr expr, String name, String[] args)
  {
    if (args == null) args = new String[0];
    Expr.Call x = (Expr.Call)expr;
    verifyEq(x.id, Expr.CALL);
    verifyEq(x.name, name);
    verifyEq(x.args.length, args.length);
    for (int i=0; i<args.length; ++i)
      verifyEq(((Expr.Name)x.args[i]).name, args[i]);
    return x.target;
  }

  Expr expr(String s)
  {
    Parser p = new Parser(new Compiler(), s);
    p.readTokens();
    return p.expr();
  }

}
