//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.parser.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * ResolveExpr computes the type of every expr in the AST.
 * We also use this step to setup the local variable scope
 * and resolve goto labels.  We also do constant folding
 * in this step.
 */
public class ResolveExpr
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public ResolveExpr(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  ResolveExpr");
    walkAst(WALK_TO_EXPRS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// AstVisitor
//////////////////////////////////////////////////////////////////////////

  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    numLocalsInScope = 0;
    maxLocals = 0;      
    labels = null;    
    gotos = null;
  }

  public void enterBlock(Block b)
  {
    enterScope(b);
  }

  public void exitBlock(Block b)
  {
    exitScope(b);
  }

  public void exitMethod(MethodDef m)
  {
    m.maxLocals = maxLocals;   
    resolveGotos();
  }

  public void enterStmt(Stmt s)
  {
    if (s instanceof LocalScope) enterScope((LocalScope)s);
    if (s.id == Stmt.FOREACH) ++foreachDepth;
    
    mapLabel(s);

    switch (s.id)
    {
      case Stmt.EXPR_STMT: exprStmt((Stmt.ExprStmt)s); break;
      case Stmt.LOCAL_DEF: localDefStmt((Stmt.LocalDef)s); break;
      case Stmt.RETURN:    returnStmt((Stmt.Return)s); break;
      case Stmt.FOR:       forStmt((Stmt.For)s); break;
      case Stmt.GOTO:      gotoStmt((Stmt.Goto)s); break;
    }
  }

  public void exitStmt(Stmt s)
  {
    if (s instanceof LocalScope) exitScope((LocalScope)s);
    if (s.id == Stmt.FOREACH) --foreachDepth;
  }

  private void exprStmt(Stmt.ExprStmt stmt)
  {
    stmt.expr.leave = false;
  }

  private void localDefStmt(Stmt.LocalDef stmt)
  {
    stmt.declared = true;
  }

  private void returnStmt(Stmt.Return stmt)
  {
    stmt.foreachDepth = foreachDepth;
  }

  private void forStmt(Stmt.For stmt)
  {
    if (stmt.update != null) stmt.update.leave = false;
  }

////////////////////////////////////////////////////////////////
// Labels/Gotos
////////////////////////////////////////////////////////////////

  private void mapLabel(Stmt stmt)
  { 
    String label = stmt.label;
    if (label == null) return;
    
    if (labels == null) 
      labels = new HashMap(13);
      
    if (labels.get(label) != null)
      err("Duplicate label '" + label + "'", stmt.loc);
    else
      labels.put(label, stmt);
  }

  private void gotoStmt(Stmt.Goto stmt)
  {                              
    if (gotos == null) gotos = new ArrayList(8);
    gotos.add(stmt);
  }

  private void resolveGotos()
  {                         
    if (gotos == null) return;
    if (labels == null) labels = new HashMap();
    for (int i=0; i<gotos.size(); ++i)
    {
      Stmt.Goto stmt = (Stmt.Goto)gotos.get(i);
      stmt.destStmt = (Stmt)labels.get(stmt.destLabel);
      if (stmt.destStmt == null)
        err("Unknown label '" + stmt.destLabel + "'", stmt.loc);
    }
  }                           

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr)
  {
    switch (expr.id)
    {
      // unary
      case Expr.NEGATE:
      case Expr.BIT_NOT:
      case Expr.PRE_INCR:
      case Expr.PRE_DECR:
      case Expr.POST_INCR:
      case Expr.POST_DECR:  expr.type = ((Expr.Unary)expr).operand.type; break;

      // cond
      case Expr.COND_NOT:
      case Expr.COND_OR:
      case Expr.COND_AND:   expr.type = ns.boolType; break;

      // binary compares
      case Expr.EQ:
      case Expr.NOT_EQ:
      case Expr.GT:
      case Expr.GT_EQ:
      case Expr.LT:
      case Expr.LT_EQ:      expr.type = ns.boolType; break;

      // binary typed by lhs
      case Expr.ADD:        return resolveAdd((Expr.Binary)expr);                               
      case Expr.MUL:
      case Expr.DIV:
      case Expr.MOD:
      case Expr.SUB:
      case Expr.BIT_OR:
      case Expr.BIT_XOR:
      case Expr.BIT_AND:
      case Expr.LSHIFT:
      case Expr.RSHIFT:
      case Expr.ASSIGN:
      case Expr.ASSIGN_ADD:
      case Expr.ASSIGN_SUB:
      case Expr.ASSIGN_MUL:
      case Expr.ASSIGN_DIV:
      case Expr.ASSIGN_MOD:
      case Expr.ASSIGN_BIT_AND:
      case Expr.ASSIGN_BIT_OR:
      case Expr.ASSIGN_BIT_XOR:
      case Expr.ASSIGN_LSHIFT:
      case Expr.ASSIGN_RSHIFT:
      case Expr.PROP_ASSIGN:
      case Expr.ELVIS:          expr.type = ((Expr.Binary)expr).lhs.type; break;

      // misc
      case Expr.TERNARY:    expr.type = ((Expr.Ternary)expr).trueExpr.type; break;
      case Expr.NAME:       expr = resolveName((Expr.Name)expr); break;
      case Expr.THIS:       expr.type = curType; break;
      case Expr.SUPER:      expr.type = curType.base; break;
      case Expr.INDEX:      expr = resolveIndex((Expr.Index)expr); break;
      case Expr.CALL:       expr = resolveCall((Expr.Call)expr); break;
      case Expr.INIT_ARRAY: expr.type = ns.voidType; break;
      case Expr.NEW:        resolveNew((Expr.New)expr); break;
      case Expr.DELETE:     expr.type = ns.voidType; break;
    }

    if (expr.type == null)
      throw err("Expr not typed: " + expr, expr.loc);

    return expr;
  }

//////////////////////////////////////////////////////////////////////////
// Add Resolution
//////////////////////////////////////////////////////////////////////////

  public Expr resolveAdd(Expr.Binary expr)
  {   
    Expr lhs = expr.lhs;                 
    Expr rhs = expr.rhs;             
    expr.type = lhs.type;
    
    // check for string interpolation
    if (lhs.id == Expr.STR_LITERAL || lhs.id == Expr.INTERPOLATION)
    {
      Expr.Interpolation si = new Expr.Interpolation(lhs.loc);
      si.type = ns.strType;
      if (lhs.id == Expr.STR_LITERAL)
        si.parts.add(lhs);
      else
        si.parts.addAll(((Expr.Interpolation)lhs).parts);
      
      if (rhs.id == Expr.INTERPOLATION)
        si.parts.addAll(((Expr.Interpolation)rhs).parts);
      else
        si.parts.add(rhs);
      return si;
    }
    
    return expr;
  }

//////////////////////////////////////////////////////////////////////////
// Index Resolution
//////////////////////////////////////////////////////////////////////////

  public Expr resolveIndex(Expr.Index expr)
  {
    Type targetType = expr.target.type;
    if (!targetType.isArray())
    {
      if (targetType != Namespace.error)
        err("Cannot use [] operator on '" + targetType + "'", expr.loc);
      expr.type = Namespace.error;
      return expr;
    }

    expr.type = targetType.arrayOf();
    return expr;
  }

//////////////////////////////////////////////////////////////////////////
// New
//////////////////////////////////////////////////////////////////////////

  public void resolveNew(Expr.New expr)
  {                  
    if (expr.arrayLength == null)
      expr.type = expr.of;
    else
      expr.type = new ArrayType(expr.loc, expr.of, new ArrayType.UnresolvedLen(expr.arrayLength.toString()));
  }

//////////////////////////////////////////////////////////////////////////
// Name Resolution
//////////////////////////////////////////////////////////////////////////

  private void enterScope(LocalScope scope)
  {
    // check for dup names and assign scope indices
    Stmt.LocalDef[] locals = scope.getLocals();
    HashMap dupsInThisScope = new HashMap(locals.length*3);  
    int scopeSize = 0;
    for (int i=0; i<locals.length; ++i)
    {
      Stmt.LocalDef local = locals[i];

      // check for duplicate names in scope
      String name = local.name;
      if (resolveVar(name) != null || dupsInThisScope.get(name) != null)
        err("Variable '" + name + "' already defined in scope", local.loc);

      // assign index
      scopeSize++;
      local.index = numLocalsInScope++;
      if (local.type.isWide()) { numLocalsInScope++; scopeSize++; }
      local.declared = false;     
      
      // check if we've defined the new max local
      int thisMax = local.index+1;
      if (local.type.isWide()) thisMax++;
      if (thisMax > maxLocals)  maxLocals = thisMax;
      
      dupsInThisScope.put(name, local);
    }

    // push onto stack
    scopeStack.push(new ScopeStackItem(scope, scopeSize));
  }

  private void exitScope(LocalScope scope)
  {
    ScopeStackItem item = (ScopeStackItem)scopeStack.pop();
    if (item.scope != scope) throw new IllegalStateException();
    numLocalsInScope -= item.numLocals;
  }

  public Expr resolveName(Expr.Name expr)
  {
    Location loc = expr.loc;
    String name = expr.name;
    Expr target = expr.target;

    // if target, this must be a field on the target type
    if (target != null)
    {
      Type base = target.type;
      Slot slot = base.slot(name);

      // check for special type literals
      if (target.id == Expr.STATIC_TYPE)
      {
        // TypeName.type is the syntax for type literal
        if (name.equals("type"))
          return new Expr.Literal(target.loc, ns, Expr.TYPE_LITERAL, target.type);

        // TypeName.sizeof is the syntax for sizeof literal
        if (name.equals("sizeof"))
          return new Expr.Literal(target.loc, ns, Expr.SIZE_OF, target.type);

        // check for TypeName.slot which is the syntax for slot literal
        if (slot != null && slot.isReflective())
          return new Expr.Literal(target.loc, ns, Expr.SLOT_LITERAL, slot);
      }

      // maps to a field access
      if (slot instanceof Field)
      {
        Field f = (Field)slot;

        // Catch a slot ID literal... will treat it specially later
        if ((target.id==Expr.SLOT_LITERAL) && slot.qname().equals("sys::Slot.id"))
          return new Expr.Literal(loc, Expr.SLOT_ID_LITERAL, f.type(), target);

        return new Expr.Field(loc, target, f, expr.safeNav);
      }

      if (target.type != Namespace.error)
        err("Unknown field: " + base.signature() + "." + name, expr.loc);
      expr.type = Namespace.error;
      return expr;
    }

    // check for a param or local binding
    VarDef var = resolveVar(name);
    if (var != null)
    {
      if (var.isParam())
        return new Expr.Param(loc, name, (ParamDef)var);
      else
        return new Expr.Local(loc, (Stmt.LocalDef)var);
    }

    // check for a field on my current type
    Slot slot = curType.slot(name);
    if (slot instanceof Field)
    {
      // add implicit this if needed
      target = slot.isStatic() ? null : new Expr.This(loc);
      return new Expr.Field(loc, target, (Field)slot, expr.safeNav);
    }

    // unknown variable name
    err("Unknown var: " + name, expr.loc);
    expr.type = Namespace.error;
    return expr;
  }

  public VarDef resolveVar(String name)
  {
    // first try to find in params
    if (curMethod == null) return null;
    ParamDef[] params = curMethod.params;
    for (int i=0; i<params.length; ++i)
      if (params[i].name.equals(name)) return params[i];

    // check block stack for locals in scope
    for (int i=scopeStack.size()-1; i>=0; --i)
    {
      ScopeStackItem item = (ScopeStackItem)scopeStack.get(i);
      Stmt.LocalDef local = item.scope.resolveLocal(name);
      if (local != null && local.declared) return local;
    }

    return null;
  }

  static class ScopeStackItem
  {
    ScopeStackItem(LocalScope s, int n) { scope = s; numLocals = n; }
    LocalScope scope;
    int numLocals;
  }

//////////////////////////////////////////////////////////////////////////
// Call Resolution
//////////////////////////////////////////////////////////////////////////

  public Expr resolveCall(Expr.Call call)
  {
    // get target type
    Type target = curType;
    if (call.target != null)
      target = call.target.type;

    // lookup slot
    String name = call.name;
    Slot slot = target.slot(name);
    if (slot == null)
    {
      if (target != Namespace.error)
        err("Unknown method '" + target + "." + name + "'", call.loc);
      call.type = Namespace.error;
      return call;
    }

    // check not field
    if (slot.isField())
    {
      err("Cannot call field '" + target + "." + name + "' as method", call.loc);
      call.type = Namespace.error;
      return call;
    }

    // resolved (we do error checking in later step)
    Method method = (Method)slot;
    call.type = method.returnType();
    call.method = method;   
    
    // if the argument is an interpolated string, then
    // check that this method returns an OutStream
    if (call.args.length == 1 && call.args[0].id == Expr.INTERPOLATION)
    { 
      Type os = ns.resolveType("sys::OutStream");
      if (method.returnType().is(os))
        ((Expr.Interpolation)call.args[0]).callOk = true;
      else
        err("String interpolation requires that '" + method.qname() + "' return OutStream", call.loc);
    }
    
    // add implicit this if needed
    if (call.target == null && !method.isStatic())
      call.target = new Expr.This(call.loc);

    return call;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  int numLocalsInScope = 0;
  int maxLocals = 0;
  Stack scopeStack = new Stack();
  int foreachDepth;
  HashMap labels;    // String -> Stmt
  ArrayList gotos;   // Stmt.Goto
}
