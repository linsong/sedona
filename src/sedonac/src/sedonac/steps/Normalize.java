//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   8 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.File;

import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.namespace.*;

/**
 * Normalize fixes up the AST:
 *   - check for unsized array length assignment in constructor
 *   - add return if no return explicit statement
 *   - map field inits to instance/static initializers
 */
public class Normalize
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public Normalize(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    walkAst(WALK_TO_SLOTS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public void enterType(TypeDef t)
  {
    super.enterType(t);
    MethodDef ctor = (MethodDef)t.slot(Method.INSTANCE_INIT);
    if (ctor != null) processCtor(ctor);
  }

  private void processCtor(MethodDef m)
  {
    checkArrayLengthAssignment(m);
  }

  private void checkArrayLengthAssignment(MethodDef m)
  {
    // scan the statements looking for:
    //   {this}.array.length = param
    for (int i=0; i<m.code.stmts.size(); ++i)
    {
      Stmt stmt = (Stmt)m.code.stmts.get(i);

      // skip if not assignment statement
      if (stmt.id != Stmt.EXPR_STMT) continue;
      Expr expr = ((Stmt.ExprStmt)stmt).expr;
      if ((expr.id != Expr.ASSIGN) && (expr.id != Expr.PROP_ASSIGN)) continue;
      Expr.Binary assign = (Expr.Binary)expr;

      // skip if lhs is assigment to "something.length"
      if (assign.lhs.id != Expr.NAME) continue;
      Expr.Name lhs = (Expr.Name)assign.lhs;
      if (lhs.target == null || !lhs.name.equals("length")) continue;

      // check if the "something" target is an array field
      if (lhs.target.id != Expr.NAME) continue;
      Expr.Name target = (Expr.Name)lhs.target;
      Slot slot = curType.slot(target.name);
      if (slot == null || !(slot instanceof FieldDef)) continue;
      FieldDef f = (FieldDef)slot;
      if (!f.type.isArray()) continue;

      // sanity checking
      if (f.isStatic())
      {
        err("Cannot specify length of static field", assign.loc);
        continue;
      }
      if (target.target != null && target.target.id != Expr.THIS)
      {
        err("Cannot specify length of field not owned by this instance", assign.loc);
        continue;
      }

      // now check that the right hand side is a parameter name
      if (assign.rhs.id == Expr.NAME)
      {
        Expr.Name rhs = (Expr.Name)assign.rhs;
        for (int j=0; j<m.params.length; ++j)
        {
          // if we found a match, then this statement
          // is a compile-time directive to match the
          // parameter to the inline array allocation
          if (m.params[j].name.equals(rhs.name))
          {
            f.ctorLengthParam = j+1;  // 0 is implicit this
            m.code.stmts.remove(i);
            return;
          }
        }
      }
      err("Right hand side of array length assignment must be parameter", assign.loc);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Field
//////////////////////////////////////////////////////////////////////////

  public void enterField(FieldDef f)
  {
    super.enterField(f);
    if (f.init != null && !f.isDefine())
    {
      if (f.init.id == Expr.INIT_ARRAY)
      {
        initFieldArray(f);
      }
      else if (f.init.id == Expr.STR_LITERAL)
      {
        initFieldInstance(f);
        initFieldStr(f);
      }
      else if (f.init.id == Expr.BUF_LITERAL)
      {
        initFieldBuf(f);
      }
      else
      {
        initFieldExpr(f);
      }
    }
    else if (f.isInline() && !f.type.isArray())
    {
      initFieldInstance(f);
    }        
    
  }
  
  private void initFieldExpr(FieldDef f)
  {
    // append "field = f.init"
    Location loc = f.init.loc;
    Expr lhs = toFieldExpr(loc, f);
    Expr rhs = f.init;
    Expr assign = new Expr.Binary(loc, new Token(loc, Token.ASSIGN), lhs, rhs);

    MethodDef m = makeInitMethod(f);
    m.code.add(m.initStmtIndex++, assign.toStmt());
  }
  
  private void initFieldBuf(FieldDef f)
  {
    // sanity check - bail and catch later in CheckErrors
    if (!f.type.isBuf() || f.init.id != Expr.BUF_LITERAL)
      return;

    if (f.isStatic())
    {
      // Buf literal: static Buf literalA = 0x[cafe babe]
      initFieldExpr(f);
    }
    else if (f.isInline())
    {
      // inlined Buf: inline Buf(4) buf = 0x[cafe babe]
      initFieldInstance(f);
      
      Location loc = f.init.loc;
      Integer len = f.ctorArgs[0].toIntLiteral();
      if (len == null) return;
      Method copyFromBuf = (Method)f.type.slot("copyFromBuf");
      Expr call = new Expr.Call(loc, toFieldExpr(loc, f), copyFromBuf, new Expr[] {f.init});
      
      MethodDef m = makeInitMethod(f);
      m.code.add(m.initStmtIndex++, call.toStmt());
    }
  }

  private void initFieldStr(FieldDef f)
  {
    // sanity check - bail and catch later in CheckErrors
    if (!(f.type.isBuf() || f.type.isStr()) || !f.isInline() || f.init.id != Expr.STR_LITERAL)
      return;
    
    Location loc = f.init.loc;
    
    if (f.ctorArgs == null || f.ctorArgs.length == 0) return;
    
    Method copyFromStr = (Method)f.type.slot("copyFromStr");
    Expr call = null;
    if (f.type.isBuf())
    {
      call = new Expr.Call(loc, toFieldExpr(loc, f), copyFromStr, new Expr[] {f.init});
    }
    else
    {
      Expr max = f.ctorArgs[0];
      call = new Expr.Call(loc, toFieldExpr(loc, f), copyFromStr, new Expr[] {f.init, max});
    }

    MethodDef m = makeInitMethod(f);
    m.code.add(m.initStmtIndex++, call.toStmt());
  }

  private void initFieldArray(FieldDef f)
  {
    // if not array of refs just bail - we'll catch later in CheckErrors
    if (!f.type.isArray() || !f.type.arrayOf().isRef())
      return;
    
    Location loc = f.init.loc;
    Type of = f.type.arrayOf();
    ArrayType.Len arrayLen = f.type.arrayLength();
    Method ofInit = (Method)of.slot(Method.INSTANCE_INIT);
    
    // take into account implicit 'this' parameter
    if (ofInit.numParams() > 1)
    {
      err("Cannot create an initialized field array of type '" + of + "' because its constructor takes arguments", f.loc);
      return ;
    }

    // append "ArrayInit field length sizeof(of)"
    Expr.InitArray init = (Expr.InitArray)f.init;
    init.field = toFieldExpr(loc, f);
    if (arrayLen instanceof ArrayType.LiteralLen)
    {
      ArrayType.LiteralLen len = (ArrayType.LiteralLen)arrayLen;
      init.length = new Expr.Literal(loc, ns, Expr.INT_LITERAL, new Integer(len.val));
    }
    else
    {
      ArrayType.DefineLen len = (ArrayType.DefineLen)arrayLen;
      init.length = new Expr.Field(loc, null, len.field);
    }

    // append "foreach(Type _item : array) _item._iInit()"
    Stmt.Foreach loop = new Stmt.Foreach(loc);
    loop.local = new Stmt.LocalDef(loc, of, "_item");
    loop.array = toFieldExpr(loc, f);
    loop.block = new Block(loc);
    loop.block.add(new Expr.Call(loc, new Expr.Local(loc, loop.local), ofInit, new Expr[0]).toStmt());

    // append two statements
    MethodDef m = makeInitMethod(f);
    m.code.add(m.initStmtIndex++, init.toStmt());
    m.code.add(m.initStmtIndex++, loop);
  }

  private void initFieldInstance(FieldDef f)
  {
    // append "field._iInit()"
    Location loc = f.loc;
    Type type = f.type;

    // lookup instance initializer method
    Method iInit = (Method)type.slot(Method.INSTANCE_INIT);
    if (iInit == null)
    {
      System.out.println("WARNING: missing instance init: " + type);
      return;
    }

    Expr target = toFieldExpr(loc, f);
    Expr[] args = f.ctorArgs;
    if (args == null) args = new Expr[0];
    Expr.Call call = new Expr.Call(loc, target, iInit, args);

    MethodDef m = makeInitMethod(f);
    m.code.add(m.initStmtIndex++, call.toStmt());
  }

  private MethodDef makeInitMethod(FieldDef f)
  {
    return f.isStatic() ? curType.makeStaticInit(ns) : curType.makeInstanceInit(ns);
  }

  private Expr toFieldExpr(Location loc, FieldDef f)
  {
    Expr target = f.isStatic() ? null : new Expr.This(loc);
    return new Expr.Field(loc, target, f);
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    checkReturn(m);
  }

  private void checkReturn(MethodDef m)
  {
    if (m.code == null) return;
    if (m.code.isExit()) return;
    m.code.add(new Stmt.Return(m.loc));
  }

}
