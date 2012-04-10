//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 May 08  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * ConstFolding is used to fold constant expressions such as "1+2" into "3".
 */
public class ConstFolding
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public ConstFolding(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {                        
    log.debug("  ConstFolding");
    walkAst(WALK_TO_EXPRS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Visit Expr
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr)
  {
    try
    { 
      // always fold unary for negatives
      if (expr instanceof Expr.Unary)
        return fold((Expr.Unary)expr);
      
      // maybe fold interpolation...
      if (expr instanceof Expr.Interpolation)
        return fold((Expr.Interpolation)expr);
      
      // maybe fold cast...
      if (expr instanceof Expr.Cast)
        return fold((Expr.Cast)expr);

      // if optimize is turned off then bail
      if (!compiler.optimize) return expr;
        
      if (expr instanceof Expr.Binary)
        return fold((Expr.Binary)expr);
    }
    catch (Exception e)
    {
      // ignore any errors which should be caught in CheckErrors
    }
      
    return expr;
  }

////////////////////////////////////////////////////////////////
// Unary
////////////////////////////////////////////////////////////////

  private Expr fold(Expr.Unary expr)
  {
    if (!expr.operand.isLiteral()) return expr;
    Expr.Literal literal = (Expr.Literal)expr.operand;

    if (expr.id == Expr.NEGATE) 
      return literal.negate();
      
    return expr;
  }

////////////////////////////////////////////////////////////////
// Interpolation
////////////////////////////////////////////////////////////////

  /**
   * Only fold interpolations if optimizing or if in a non-printing context. We
   * know we are in a non-printing context if {@link Expr.Interpolation#callOk}
   * is false, since it only gets set when the interpolation is an argument
   * to a method that returns a Sys::OutStream.
   */
  private Expr fold(Expr.Interpolation expr)
  {
    if (!compiler.optimize && expr.callOk)
      return expr;
    
    ArrayList folded = new ArrayList();
    folded.add(expr.first());
    for (int i = 1; i < expr.parts.size(); ++i)
    {
      int topIndex = folded.size() - 1;
      Expr top = (Expr)folded.get(topIndex);
      Expr cur = (Expr)expr.parts.get(i);
      if (top.id == Expr.STR_LITERAL && cur.id == Expr.STR_LITERAL)
      {
        String concat = top.toLiteral().asString() + cur.toLiteral().asString();
        folded.set(topIndex, new Expr.Literal(top.loc, ns, Expr.STR_LITERAL, concat));
        continue;
      }
      folded.add(cur);
    }
    expr.parts = folded;
    // size can only be 1 if folded to a single STR_LITERAL
    return folded.size() == 1 ? (Expr)folded.get(0) : expr;
  }
 
////////////////////////////////////////////////////////////////
// Cast
////////////////////////////////////////////////////////////////

  private Expr fold(Expr.Cast expr)
  {
    if (!expr.target.isLiteral()) return expr;

    if (expr.type.isInteger()) 
    {
      // (byte) and (short) casts are really casts to (int) since the svm
      // will always expand byte/short fields to 32-bit int when loading them.
      // This behavior is sort of weird, so we're only going to do it when
      // optimization is on.
      if (!compiler.optimize && (expr.type.isByte() || expr.type.isShort()))
        return expr;
      
      Integer i = new Integer(((Number)expr.target.toLiteral().value).intValue());
      return new Expr.Literal(expr.loc, ns, Expr.INT_LITERAL, i);
    }
    
    if (expr.type.isLong())
    {
      Long l = new Long(((Number)expr.target.toLiteral().value).longValue());
      return new Expr.Literal(expr.loc, ns, Expr.LONG_LITERAL, l);
    }
    
    if (expr.type.isFloat())
    {
      Float f = new Float(((Number)expr.target.toLiteral().value).floatValue());
      return new Expr.Literal(expr.loc, ns, Expr.FLOAT_LITERAL, f);
    }
    
    if (expr.type.isDouble())
    {
      Double d = new Double(((Number)expr.target.toLiteral().value).doubleValue());
      return new Expr.Literal(expr.loc, ns, Expr.DOUBLE_LITERAL, d);
    }
    
    return expr;
  }

////////////////////////////////////////////////////////////////
// Binary
////////////////////////////////////////////////////////////////

  private Expr fold(Expr.Binary expr)
  {          
    Expr lhs = expr.lhs;
    Expr rhs = expr.rhs;
    
    // don't try fold unless lhs and rhs have same 
    // type (most likely an error to catch in CheckErrors)
    if (lhs.type != rhs.type && !expr.op.isShift()) 
      return expr;
    
    // if both lhs and rhs are literals  
    if (lhs.isLiteral() && rhs.isLiteral())
      return foldLiterals(expr);
    
    // if lhs is a literal and rhs is zero
    if (rhs.isLiteral() && ((Expr.Literal)rhs).isZero())
      return foldZero(expr);
    
    // we're out of ideas
    return expr;
  }

  
////////////////////////////////////////////////////////////////
// Binary - Two Literals
////////////////////////////////////////////////////////////////

  private Expr foldLiterals(Expr.Binary expr)
  {
    if (!expr.lhs.isLiteral() || !expr.rhs.isLiteral()) 
      throw new IllegalStateException();
    
    Location loc = expr.loc;                      
    int id = expr.lhs.id;
    Type type = expr.lhs.type;
    Number rhs = (Number)((Expr.Literal)expr.rhs).value;
    Number lhs = (Number)((Expr.Literal)expr.lhs).value;   
    
      // binary typed by lhs
    switch (expr.id)
    {
      case Expr.ADD:     return new Expr.Literal(loc, id, type, add(lhs, rhs));                               
      case Expr.MUL:     return new Expr.Literal(loc, id, type, mul(lhs, rhs));
      case Expr.DIV:     return new Expr.Literal(loc, id, type, div(lhs, rhs));
      case Expr.MOD:     return new Expr.Literal(loc, id, type, mod(lhs, rhs));
      case Expr.SUB:     return new Expr.Literal(loc, id, type, sub(lhs, rhs));
      case Expr.BIT_OR:  return new Expr.Literal(loc, id, type, bitOr(lhs, rhs));
      case Expr.BIT_XOR: return new Expr.Literal(loc, id, type, bitXor(lhs, rhs));
      case Expr.BIT_AND: return new Expr.Literal(loc, id, type, bitAnd(lhs, rhs)); 
      case Expr.LSHIFT:  return new Expr.Literal(loc, id, type, lshift(lhs, rhs));
      case Expr.RSHIFT:  return new Expr.Literal(loc, id, type, rshift(lhs, rhs));
    }
    
    return expr;
  }                
  
  private Object add(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue()   + b.intValue());
      case LONG:   return new Long(a.longValue()     + b.longValue());
      case FLOAT:  return new Float(a.floatValue()   + b.floatValue());
      case DOUBLE: return new Double(a.doubleValue() + b.doubleValue());
    }      
    throw new RuntimeException();
  }

  private Object mul(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue()   * b.intValue());
      case LONG:   return new Long(a.longValue()     * b.longValue());
      case FLOAT:  return new Float(a.floatValue()   * b.floatValue());
      case DOUBLE: return new Double(a.doubleValue() * b.doubleValue());
    }      
    throw new RuntimeException();
  }

  private Object div(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue()   / b.intValue());
      case LONG:   return new Long(a.longValue()     / b.longValue());
      case FLOAT:  return new Float(a.floatValue()   / b.floatValue());
      case DOUBLE: return new Double(a.doubleValue() / b.doubleValue());
    }      
    throw new RuntimeException();
  }

  private Object mod(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue()   % b.intValue());
      case LONG:   return new Long(a.longValue()     % b.longValue());
      case FLOAT:  return new Float(a.floatValue()   % b.floatValue());
      case DOUBLE: return new Double(a.doubleValue() % b.doubleValue());
    }      
    throw new RuntimeException();
  }

  private Object sub(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue()   - b.intValue());
      case LONG:   return new Long(a.longValue()     - b.longValue());
      case FLOAT:  return new Float(a.floatValue()   - b.floatValue());
      case DOUBLE: return new Double(a.doubleValue() - b.doubleValue());
    }      
    throw new RuntimeException();
  }

  private Object bitOr(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:    return new Integer(a.intValue() | b.intValue());
      case LONG:   return new Long(a.longValue()   | b.longValue());
    }      
    throw new RuntimeException();
  }

  private Object bitAnd(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:  return new Integer(a.intValue() & b.intValue());
      case LONG: return new Long(a.longValue()   & b.longValue());
    }      
    throw new RuntimeException();
  }

  private Object bitXor(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:  return new Integer(a.intValue() ^ b.intValue());
      case LONG: return new Long(a.longValue()   ^ b.longValue());
    }      
    throw new RuntimeException();
  }

  private Object lshift(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:  return new Integer(a.intValue() << b.intValue());
      case LONG: return new Long(a.longValue()   << b.intValue());
    }      
    throw new RuntimeException();
  }

  private Object rshift(Number a, Number b)
  {                  
    switch (valType(a))
    {
      case INT:  return new Integer(a.intValue() >> b.intValue());
      case LONG: return new Long(a.longValue()   >> b.intValue());
    }      
    throw new RuntimeException();
  }

////////////////////////////////////////////////////////////////
// Fold Zero RHS
////////////////////////////////////////////////////////////////

  private Expr foldZero(Expr.Binary expr)
  {                              
    // identity
    switch (expr.id)
    {
      case Expr.ADD:     
      case Expr.SUB:
      case Expr.BIT_OR:
      case Expr.BIT_XOR:
      case Expr.LSHIFT:
      case Expr.RSHIFT:  return expr.lhs;
    }
    
    // always zero
    switch (expr.id)
    {
      case Expr.MUL:
      case Expr.BIT_AND: return expr.rhs; 
    }
    
    // bad   
    if (expr.id == Expr.DIV)
    {
      throw err("Divide by zero", expr.loc);    
    }                 
    
    return expr;
  }
  
////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  private int valType(Object x)
  {
    if (x instanceof Integer) return INT;
    if (x instanceof Long)    return LONG;
    if (x instanceof Float)   return FLOAT;
    if (x instanceof Double)  return DOUBLE;
    return -1;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  private static final int INT    = 1;
  private static final int LONG   = 2;
  private static final int FLOAT  = 3;
  private static final int DOUBLE = 4;

}
