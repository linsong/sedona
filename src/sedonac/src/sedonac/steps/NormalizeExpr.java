//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   27 Jun 07  Brian Frank  Creation
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
 * NormalizeExpr is a second normalization step which occurs
 * after ResolveExpr and all the expressions have been resolved
 * and error checked.
 */
public class NormalizeExpr
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public NormalizeExpr(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  NormalizeExpr");
    walkAst(WALK_TO_EXPRS);
    quitIfErrors();
  }
  
//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  public void enterStmt(Stmt s)
  {
    super.enterStmt(s);
    switch (s.id)
    {
      case Stmt.SWITCH: normalizeSwitch((Stmt.Switch)s); break;
    }
  }
  
  private void normalizeSwitch(Stmt.Switch stmt)
  {
    // normalize cases, by removing any cases 
    // which fall-thru to default
    if (stmt.defaultBlock != null)
    {
      int trim = stmt.cases.length-1;
      while (trim >= 0) 
      {
        if (stmt.cases[trim].block != null) break;
        trim--;
      }
      Stmt.Case[] trimmed = new Stmt.Case[trim+1];
      System.arraycopy(stmt.cases, 0, trimmed, 0, trim+1);
      stmt.cases = trimmed;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr)
  {                         
    // if inside an initializer, we just do a raw
    // field set, there is no need to go thru setter
    if (curMethod == null || curMethod.isInstanceInit() || curMethod.isStaticInit()) 
      return expr;
    
    // if we have assignment to a property field
    if (expr.isAssign() && isProp(((Expr.Binary)expr).lhs))
    {   
      if ((expr.id == Expr.ASSIGN) || (expr.id == Expr.PROP_ASSIGN))
        return assign((Expr.Binary)expr);
      else
        return compoundAssign((Expr.Binary)expr);
    }                       

    // if we are using ++/-- on property field
    if (expr.isIncrDecr() && isProp(((Expr.Unary)expr).operand))
    {
      return incrDecr((Expr.Unary)expr);
    }
    
    if (expr.id == Expr.EQ || expr.id == Expr.NOT_EQ)
      return optimizeSlotLiteralComparison((Expr.Binary)expr);

    // no changes needed
    return expr;       
  }

  private boolean isProp(Expr expr)
  {
    return expr.id == Expr.FIELD && ((Expr.Field)expr).field.isProperty();
  }

//////////////////////////////////////////////////////////////////////////
// Slot Literal Optimization
//////////////////////////////////////////////////////////////////////////
  
  private Expr optimizeSlotLiteralComparison(final Expr.Binary expr)
  {
    // optimizes conditional equality tests in the form
    //    (slotRef == Type.slot)
    // by changing to form
    //    (slotRef.id == Type.slot.id)
    // so that during scode image generation the RHS can be replaced with
    // slot id integer constant.
    
    if (expr.id != Expr.EQ && expr.id != Expr.NOT_EQ) throw new IllegalStateException("must be '==' expr");

    Expr.Literal slotLiteral;
    Expr slotExpr;
    if (expr.lhs.id == Expr.SLOT_LITERAL)
    {
      slotLiteral = (Expr.Literal)expr.lhs;
      slotExpr = expr.rhs;
    }
    else if (expr.rhs.id == Expr.SLOT_LITERAL)
    {
      slotLiteral = (Expr.Literal)expr.rhs;
      slotExpr = expr.lhs;
    }
    else
    {
      return expr;
    }
    
    String idQname;
    if (slotExpr instanceof Expr.NameDef)
    {
      final VarDef def = ((Expr.NameDef)slotExpr).def();
      idQname = def.type().slot("id").qname();
    }
    else if (slotExpr instanceof Expr.Field)
    {
      Expr.Field f = (Expr.Field)slotExpr;
      idQname = f.type.slot("id").qname();
    }
    else
      throw new IllegalStateException("unexpected slot id expression type: " + slotExpr.id);

    final Expr slotId = new Expr.Field(slotExpr.loc, slotExpr, ns.resolveField(idQname));
    final Expr slotIdLiteral =
        new Expr.Literal(slotLiteral.loc, Expr.SLOT_ID_LITERAL, ns.byteType, slotLiteral);
    return new Expr.Binary(expr.loc, expr.op, slotId, slotIdLiteral);
  }

//////////////////////////////////////////////////////////////////////////
// Normalization
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Handle straight assignment to a property: x = val
   */
  private Expr assign(Expr.Binary expr)
  {
    Expr.Field fieldExpr = (Expr.Field)expr.lhs;
    Expr valExpr = expr.rhs;    
    
    return setProp(expr, fieldExpr, valExpr);
  }

  /**
   * Handle compound assignment to a property: x += val
   */
  private Expr compoundAssign(Expr.Binary expr)
  {     
    Expr.Field fieldExpr = (Expr.Field)expr.lhs;
    Expr rhs = expr.rhs;                    
        
    Expr.Binary valExpr = new Expr.Binary(
      rhs.loc,  
      new Token(expr.op.loc, expr.op.assignToBinary()),
      new Expr.Field(rhs.loc, dupTarget(fieldExpr, expr.op), fieldExpr.field), 
      rhs);
    
    return setProp(expr, fieldExpr, valExpr);
  }

  /**
   * Handle ++/-- assignment to a property: x++
   */
  private Expr incrDecr(Expr.Unary expr)
  {    
    // can't prefix leaves right now - it would require allocating 
    // a new local variable to store the old value (or reloading
    // the field and then doing the opposide incr)          
    if (expr.leave && expr.isPostfix())
    {
      err("Cannot use postfix " + expr.op + " operator on property assignment with chaining", expr.loc);
      return expr;
    } 
    
    Location loc = expr.loc;
    Expr.Field fieldExpr = (Expr.Field)expr.operand;

    // get 1 literal of correct type
    Expr one;
    Type t = expr.type;
    if (t.isInteger())     one = new Expr.Literal(loc, ns, Expr.INT_LITERAL,    new Integer(1));
    else if (t.isLong())   one = new Expr.Literal(loc, ns, Expr.LONG_LITERAL,   new Long(1));
    else if (t.isFloat())  one = new Expr.Literal(loc, ns, Expr.FLOAT_LITERAL,  new Float(1));
    else if (t.isDouble()) one = new Expr.Literal(loc, ns, Expr.DOUBLE_LITERAL, new Double(1));
    else { err("Cannot use " + expr.op + " operator on " + t, loc); return expr; }
    
    Expr.Binary valExpr = new Expr.Binary(
      loc,  
      new Token(loc, expr.isIncr() ? Token.PLUS : Token.MINUS),
      new Expr.Field(loc, dupTarget(fieldExpr, expr.op), fieldExpr.field), 
      one);
      
    return setProp(expr, fieldExpr, valExpr);                               
  }
  
  /**
   * Given a target for a field set, return a copy of that
   * target to use in a compound assignment or ++/--.
   */
  private Expr dupTarget(Expr.Field fieldExpr, Token op)
  {                
    Expr target = fieldExpr.target;
    switch (target.id)
    {
      case Expr.PARAM: return target;
      case Expr.LOCAL: return target;
      case Expr.THIS:  return target;
    }                 
    
    // if we support anything without side effects,
    // we need to allocate a new local variable
    err("Cannot use " + op + " operator to assign to property '" + 
        fieldExpr.field.name() + "' against chained base expr", fieldExpr.loc);
    return target;      
  }
    
////////////////////////////////////////////////////////////////
// Set Prop
////////////////////////////////////////////////////////////////
  
  /**
   * Replace an expression which assigns to a 
   * property field with a call to setXXX.
   */
  private Expr setProp(Expr orig, Expr.Field fieldExpr, Expr valExpr)
  {                             
    Location loc = orig.loc;
    Field field = fieldExpr.field;              
    Expr slot = new Expr.Literal(loc, ns, Expr.SLOT_LITERAL, field);
    Expr[] args = {slot, valExpr};

    // map field type to set method
    Type t = field.type();
    Method m = null;          
    if (t.isBool())    m = ns.resolveMethod("sys::Component.setBool");
    if (t.isInteger()) m = ns.resolveMethod("sys::Component.setInt");
    if (t.isLong())    m = ns.resolveMethod("sys::Component.setLong");
    if (t.isFloat())   m = ns.resolveMethod("sys::Component.setFloat");
    if (t.isDouble())  m = ns.resolveMethod("sys::Component.setDouble");
    if (m == null) throw err("Property doesn't map to setter: " + t.qname(), loc);

    // map field to call
    Expr.Call call = new Expr.Call(loc, Expr.PROP_SET, fieldExpr.target, m, args);
    
    // if we need to leave a value, then we need to use the 
    // stack value of the second (args[1]), this is handled 
    // specially in CodeAsm on PROP_SET
    call.type  = valExpr.type;
    call.leave = orig.leave;

    return call;
  }                                        

}
