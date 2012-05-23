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

package sedonac.asm;

import java.text.*;
import java.util.*;
import sedona.Env;
import sedona.util.*;
import sedonac.*;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.scode.*;
import sedonac.ir.*;
import sedonac.util.*;

/**
 * CodeAsm
 */
public class CodeAsm
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public CodeAsm(TypeAsm parent)
  {
    super(parent.compiler);
    this.parent = parent; 
    this.self = parent.ast;
  }

//////////////////////////////////////////////////////////////////////////
// Assemble
//////////////////////////////////////////////////////////////////////////

  public IrOp[] assemble(Block block)
  {
    block(block);
    finish();
    return (IrOp[])code.toArray(new IrOp[code.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  private void block(Block block)
  {
    this.loc = block.loc;
    for (int i=0; i<block.stmts.size(); ++i)
      stmt((Stmt)block.stmts.get(i));
  }

  private void stmt(Stmt stmt)
  {
    this.loc = stmt.loc;   
    stmt.mark = mark();
    switch (stmt.id)
    {
      case Stmt.EXPR_STMT:  exprStmt((Stmt.ExprStmt)stmt); break;
      case Stmt.LOCAL_DEF:  localDefStmt((Stmt.LocalDef)stmt); break;
      case Stmt.IF:         ifStmt((Stmt.If)stmt); break;
      case Stmt.RETURN:     returnStmt((Stmt.Return)stmt); break;
      case Stmt.FOR:        forStmt((Stmt.For)stmt); break;
      case Stmt.FOREACH:    foreachStmt((Stmt.Foreach)stmt); break;
      case Stmt.WHILE:      whileStmt((Stmt.While)stmt); break;
      case Stmt.DO_WHILE:   doWhileStmt((Stmt.DoWhile)stmt); break;
      case Stmt.BREAK:      breakStmt((Stmt.Break)stmt); break;
      case Stmt.CONTINUE:   continueStmt((Stmt.Continue)stmt); break;
      case Stmt.ASSERT:     assertStmt((Stmt.Assert)stmt); break;
      case Stmt.GOTO:       gotoStmt((Stmt.Goto)stmt); break;
      case Stmt.SWITCH:     switchStmt((Stmt.Switch)stmt); break;
      default:              throw err("CodeAsm not done: " + stmt.getClass().getName(), stmt.loc);
    }
  }                                 
  
  private void exprStmt(Stmt.ExprStmt stmt)
  {
    Expr expr = stmt.expr;
    
    // check if this is a logging call
    if (expr.id == Expr.CALL)
    {             
      Expr.Call call = (Expr.Call)expr;
      String m = call.method.qname();
      if (m.equals("sys::Log.error") ||
          m.equals("sys::Log.warning") ||
          m.equals("sys::Log.message") ||
          m.equals("sys::Log.trace"))
      {
        logStmt(call);
        return;
      }
    }          
    
    expr(expr);
  }

  private void localDefStmt(Stmt.LocalDef stmt)
  {
    if (stmt.init != null)
    {
      expr(stmt.init);
      storeLocal(stmt);
    }
  }

  private void returnStmt(Stmt.Return stmt)
  {
    // if we are jumping directly out of one or more foreach
    // loops, then we have to pop the working variables from
    // the stack to keep the stack balanced
    for (int i=0; i<stmt.foreachDepth; ++i)
      op(SCode.Pop3);

    if (stmt.expr == null)
    {
      op(SCode.ReturnVoid);
    }
    else
    {
      expr(stmt.expr);
      if (stmt.expr.type.isWide())
        op(SCode.ReturnPopWide);
      else
        op(SCode.ReturnPop);
    }
  }

  private void ifStmt(Stmt.If stmt)
  {
    IrOp endJump = null;
    Cond c = new Cond();

    // NOTE: we cannot optimize the cases:
    // 1) if (true)
    // 2) if (false)
    // because there might be goto labels inside the seemingly "dead" block

    // check condition - if the condition is itself a Expr.Cond
    // then we just have it branch directly to the true/false
    // block rather than wasting instructions to push true/false
    // onto the stack
    if (stmt.cond instanceof Expr.Cond)
    {
      cond((Expr.Cond)stmt.cond, c);
    }
    else
    {
      expr(stmt.cond);
      c.falseJumps.add(jump(SCode.JumpZero));
    }

    // true block
    for (int i=0; i<c.trueJumps.size(); ++i)
      backpatch((IrOp)c.trueJumps.get(i));
    block(stmt.trueBlock);
    if (stmt.falseBlock != null && !stmt.trueBlock.isExit())
      endJump = jump(SCode.Jump);

    // false block
    for (int i=0; i<c.falseJumps.size(); ++i)
      backpatch((IrOp)c.falseJumps.get(i));
    if (stmt.falseBlock != null)
      block(stmt.falseBlock);

    // end
    if (endJump != null) backpatch(endJump);
  }

  private void whileStmt(Stmt.While stmt)
  {
    // push myself onto the loop stack so that breaks
    // and continues can register for backpatching
    Loop loop = new Loop(stmt);
    loopStack.push(loop);

    // assemble the while loop code
    int continueMark = mark();
    expr(stmt.cond);
    IrOp falseJump = jump(SCode.JumpZero);
    block(stmt.block);
    jump(SCode.Jump, continueMark);
    int breakMark = mark();
    backpatch(falseJump, breakMark);

    // backpatch continues
    for (int i=0; i<loop.continues.size(); ++i)
      backpatch((IrOp)loop.continues.get(i), continueMark);

    // backpatch breaks
    for (int i=0; i<loop.breaks.size(); ++i)
      backpatch((IrOp)loop.breaks.get(i), breakMark);

    // pop loop stack
    loopStack.pop();
  }

  private void doWhileStmt(Stmt.DoWhile stmt)
  {
    // push myself onto the loop stack so that breaks
    // and continues can register for backpatching
    Loop loop = new Loop(stmt);
    loopStack.push(loop);

    // assemble the do/while loop code
    int startWhile = mark();
    block(stmt.block);
    int continueMark = mark();
    expr(stmt.cond);
    jump(SCode.JumpNonZero, startWhile);
    int breakMark = mark();

    // backpatch continues
    for (int i=0; i<loop.continues.size(); ++i)
      backpatch((IrOp)loop.continues.get(i), continueMark);

    // backpatch breaks
    for (int i=0; i<loop.breaks.size(); ++i)
      backpatch((IrOp)loop.breaks.get(i), breakMark);

    // pop loop stack
    loopStack.pop();
  }

  private void forStmt(Stmt.For stmt)
  {
    // push myself onto the loop stack so that breaks
    // and continues can register for backpatching
    Loop loop = new Loop(stmt);
    loopStack.push(loop);

    // assemble init if available
    if (stmt.init != null) stmt(stmt.init);

    // assemble the for loop code
    IrOp falseCond = null;
    int condMark = mark();
    if (stmt.cond != null)
    {
      expr(stmt.cond);
      falseCond = jump(SCode.JumpZero);
    }
    block(stmt.block);
    int updateMark = mark();
    if (stmt.update != null) expr(stmt.update);
    jump(SCode.Jump, condMark);
    int endMark = mark();
    if (falseCond != null) backpatch(falseCond, endMark);

    // backpatch continues
    for (int i=0; i<loop.continues.size(); ++i)
      backpatch((IrOp)loop.continues.get(i), updateMark);

    // backpatch breaks
    for (int i=0; i<loop.breaks.size(); ++i)
      backpatch((IrOp)loop.breaks.get(i), endMark);

    // pop loop stack
    loopStack.pop();
  }

  private void foreachStmt(Stmt.Foreach stmt)
  {
    // push myself onto the loop stack so that breaks
    // and continues can register for backpatching
    Loop loop = new Loop(stmt);
    loopStack.push(loop);

    // push array to iterate, length, counter
    ArrayType arrayType = (ArrayType)stmt.array.type;
    expr(stmt.array);
    if (stmt.length == null)
    {
      if (arrayType.len instanceof ArrayType.LiteralLen)
      {
        ArrayType.LiteralLen len = (ArrayType.LiteralLen)arrayType.len;
        loadInt(len.val);
      }
      else
      {
        ArrayType.DefineLen len = (ArrayType.DefineLen)arrayType.len;
        op(SCode.LoadDefine, len.field);
      }
    }
    else
    {
      expr(stmt.length);
    }
    loadInt(-1);

    // assemble the for loop code
    int loopMark = mark();
    IrOp toEnd = jump(SCode.Foreach);
    loadArray(arrayType);
    storeLocal(stmt.local);
    block(stmt.block);
    jump(SCode.Jump, loopMark);
    backpatch(toEnd);
    int endMark = mark();
    op(SCode.Pop3);

    // backpatch continues
    for (int i=0; i<loop.continues.size(); ++i)
      backpatch((IrOp)loop.continues.get(i), loopMark);

    // backpatch breaks
    for (int i=0; i<loop.breaks.size(); ++i)
      backpatch((IrOp)loop.breaks.get(i), endMark);

    // pop loop stack
    loopStack.pop();
  }

  private void breakStmt(Stmt.Break stmt)
  {
    if (loopStack.size() == 0)
      throw err("Break outside of loop", stmt.loc);
      
    // right now we don't have labeled loops,
    // so we must be matched to the current loop
    Loop loop = (Loop)loopStack.peek();

    // jump and register with Loop for backpatch
    IrOp jump = jump(SCode.Jump);
    loop.breaks.add(jump);
  }

  private void continueStmt(Stmt.Continue stmt)
  {
    if (loopStack.size() == 0)
      throw err("Continue outside of loop", stmt.loc);
    
    // right now we don't have labeled loops,
    // so we must be matched to the current loop
    Loop loop = (Loop)loopStack.peek();
    if (Stmt.SWITCH == loop.stmt.id)
      throw err("continue outside of loop", stmt.loc);

    // jump and register with Loop for backpatch
    IrOp jump = jump(SCode.Jump);
    loop.continues.add(jump);
  }

  private void assertStmt(Stmt.Assert stmt)
  {
    expr(stmt.cond);
    op(SCode.Assert, stmt.loc.line);
  }

  private void gotoStmt(Stmt.Goto stmt)
  {                         
    // insert jump instruction
    stmt.op = op(SCode.Jump);
    
    // we'll backpatch later in finish()
    if (gotos == null) gotos = new ArrayList();
    gotos.add(stmt);
  }

  private void switchStmt(Stmt.Switch stmt)
  {
    // push myself onto the loop stack so that breaks
    // and continues can register for backpatching
    Loop loop = new Loop(stmt);
    loopStack.push(loop);
    
    // calcualte the switch range 
    Stmt.Case[] cases = stmt.cases;
    int[] range = calculateRange(stmt);
    int num = cases.length;
    int min = range[0];
    int max = range[1];
    int delta = max - min + 1;
    
    // check if range is contiguous enough
    if (delta > 30 && num*3 < delta)       
    {        
      err("Switch cases too sparse: only " + num + " cases from " + min + " to " +  max + "; use if/else", stmt.loc);
      return;
    }                       
    
    // push cond and if min not zero then normalize to zero
    expr(stmt.cond);
    if (min != 0) 
    {
      op(loadIntOp(min));
      op(SCode.IntSub);
    }          
    
    // now write the switch opcode
    IrOp switchOp = op(SCode.Switch);
    int[] jumps = new int[delta];                

    // default block goes first - it's the switch fall
    // thru, save offset to back patch jump 
    int defStart = mark();
    IrOp defEnd = null;
    if (stmt.defaultBlock != null) 
    {
      block(stmt.defaultBlock);
      if (!stmt.defaultBlock.isExit())
        defEnd = jump(SCode.Jump);
    }
    else
    {  
      defEnd = jump(SCode.Jump);
    }

    // now write each case block
    IrOp[] caseEnds = new IrOp[num];
    for (int i=0; i<cases.length; ++i)
    {
      Stmt.Case c = cases[i];
      int label = c.label.toIntLiteral().intValue();
      jumps[label-min] = mark();
      if (c.block != null) block(c.block);
      if (i == (cases.length - 1))
      {
        // TODO: add static code analysis to determine if fall-through to
        // default block is necessary. The current check is not very smart.
        // It won't catch cases like
        /*
         * ...
         * case 1:
         *   if (true) break
         *   else break
         *   // we will insert a JUMP to the default block here even
         *   // though it won't be reached.
         * default: ...
         */
        // However, in the worst case, we are adding a single unreachable
        // opcode. This seems like a reasonable trade-off for now.
        
        // Last case statement will fall-through to default block.
        if (c.block.stmts()[c.block.stmts().length-1].id != Stmt.BREAK)
          op(SCode.Jump, defStart);
      }
    }
    
    // fill in any no cased jumps to default
    for (int i=0; i<jumps.length; ++i)
      if (jumps[i] == 0) jumps[i] = defStart;

    // backpatch breaks and default block
    int end = mark();
    if (defEnd != null) backpatch(defEnd, end);   
    for (int i=0; i<loop.breaks.size(); ++i)
      backpatch((IrOp)loop.breaks.get(i), end);
    
    // translate jump table into string argument
    StringBuffer s = new StringBuffer();
    for (int i=0; i<jumps.length; ++i)
    {
      if (i > 0) s.append(',');
      s.append(jumps[i]);
    }                    
    switchOp.arg = s.toString();

    // pop loop stack
    loopStack.pop();
  }                                    
  
  private int[] calculateRange(Stmt.Switch stmt)
  { 
    if (stmt.cases.length == 0)
      return new int[] { 0, 0 };
                                
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    
    for (int i=0; i<stmt.cases.length; ++i)
    {                       
      int label = ((Integer)stmt.cases[i].label.toIntLiteral()).intValue();
      if (label < min) min = label;
      if (label > max) max = label;
    }    
    
    return new int[] { min, max };
  }           
  
//////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr)
  {
    this.loc = expr.loc;
    switch (expr.id)
    {
      case Expr.TRUE_LITERAL:
      case Expr.FALSE_LITERAL:
      case Expr.INT_LITERAL:
      case Expr.STR_LITERAL:
      case Expr.BUF_LITERAL:
      case Expr.NULL_LITERAL:
      case Expr.TYPE_LITERAL:
      case Expr.SLOT_LITERAL:
      case Expr.SLOT_ID_LITERAL:
      case Expr.SIZE_OF:        op(loadLiteral((Expr.Literal)expr)); break;
      case Expr.LONG_LITERAL:   loadLong(((Expr.Literal)expr).asLong()); break; 
      case Expr.TIME_LITERAL:   loadLong(((Expr.Literal)expr).asLong()); break;
      case Expr.FLOAT_LITERAL:  loadFloat(((Expr.Literal)expr).asFloat()); break;
      case Expr.DOUBLE_LITERAL: loadDouble(((Expr.Literal)expr).asDouble()); break;
      case Expr.NEGATE:         unary(expr, SCode.IntNeg, SCode.LongNeg, SCode.FloatNeg, SCode.DoubleNeg); break;
      case Expr.BIT_NOT:        unary(expr, SCode.IntNot, SCode.LongNot); break;
      case Expr.COND_NOT:       unary(expr, SCode.EqZero, -1); break;
      case Expr.PRE_INCR:
      case Expr.PRE_DECR:
      case Expr.POST_INCR:
      case Expr.POST_DECR:      increment((Expr.Unary)expr); break;
      case Expr.COND_OR:        or((Expr.Cond)expr, null); break;
      case Expr.COND_AND:       and((Expr.Cond)expr, null); break;
      case Expr.EQ:             binary(expr, SCode.IntEq,    SCode.LongEq,    SCode.FloatEq,    SCode.DoubleEq,    SCode.ObjEq); break;
      case Expr.NOT_EQ:         binary(expr, SCode.IntNotEq, SCode.LongNotEq, SCode.FloatNotEq, SCode.DoubleNotEq, SCode.ObjNotEq); break;
      case Expr.GT:             binary(expr, SCode.IntGt,    SCode.LongGt,    SCode.FloatGt,    SCode.DoubleGt);   break;
      case Expr.GT_EQ:          binary(expr, SCode.IntGtEq,  SCode.LongGtEq,  SCode.FloatGtEq,  SCode.DoubleGtEq); break;
      case Expr.LT:             binary(expr, SCode.IntLt,    SCode.LongLt,    SCode.FloatLt,    SCode.DoubleLt);   break;
      case Expr.LT_EQ:          binary(expr, SCode.IntLtEq,  SCode.LongLtEq,  SCode.FloatLtEq,  SCode.DoubleLtEq); break;
      case Expr.BIT_OR:         binary(expr, SCode.IntOr,    SCode.LongOr);  break;
      case Expr.BIT_XOR:        binary(expr, SCode.IntXor,   SCode.LongXor);  break;
      case Expr.BIT_AND:        binary(expr, SCode.IntAnd,   SCode.LongAnd);  break;
      case Expr.LSHIFT:         binary(expr, SCode.IntShiftL, SCode.LongShiftL); break;
      case Expr.RSHIFT:         binary(expr, SCode.IntShiftR, SCode.LongShiftR); break;
      case Expr.MUL:            binary(expr, SCode.IntMul, SCode.LongMul, SCode.FloatMul, SCode.DoubleMul); break;
      case Expr.DIV:            binary(expr, SCode.IntDiv, SCode.LongDiv, SCode.FloatDiv, SCode.DoubleDiv); break;
      case Expr.MOD:            binary(expr, SCode.IntMod, SCode.LongMod); break;
      case Expr.ADD:            binary(expr, SCode.IntAdd, SCode.LongAdd, SCode.FloatAdd, SCode.DoubleAdd); break;
      case Expr.SUB:            binary(expr, SCode.IntSub, SCode.LongSub, SCode.FloatSub, SCode.DoubleSub); break;
      case Expr.ASSIGN:         assign((Expr.Binary)expr); break;
      case Expr.ASSIGN_BIT_OR:  assign(expr, SCode.IntOr,  SCode.LongOr);  break;
      case Expr.ASSIGN_BIT_XOR: assign(expr, SCode.IntXor, SCode.LongXor); break;
      case Expr.ASSIGN_BIT_AND: assign(expr, SCode.IntAnd, SCode.LongAnd); break;
      case Expr.ASSIGN_LSHIFT:  assign(expr, SCode.IntShiftL, SCode.LongShiftL); break;
      case Expr.ASSIGN_RSHIFT:  assign(expr, SCode.IntShiftR, SCode.LongShiftR); break;
      case Expr.ASSIGN_MUL:     assign(expr, SCode.IntMul, SCode.LongMul, SCode.FloatMul, SCode.DoubleMul); break;
      case Expr.ASSIGN_DIV:     assign(expr, SCode.IntDiv, SCode.LongDiv, SCode.FloatDiv, SCode.DoubleDiv); break;
      case Expr.ASSIGN_MOD:     assign(expr, SCode.IntMod, SCode.LongMod); break;
      case Expr.ASSIGN_ADD:     assign(expr, SCode.IntAdd, SCode.LongAdd, SCode.FloatAdd, SCode.DoubleAdd); break;
      case Expr.ASSIGN_SUB:     assign(expr, SCode.IntSub, SCode.LongSub, SCode.FloatSub, SCode.DoubleSub); break;
      case Expr.ELVIS:          elvis((Expr.Binary)expr); break;
      case Expr.PROP_ASSIGN:    assign((Expr.Binary)expr); break;
      case Expr.TERNARY:        ternary((Expr.Ternary)expr); break;
      case Expr.PARAM:          loadParam(((Expr.Param)expr).def); break;
      case Expr.LOCAL:          loadLocal(((Expr.Local)expr).def); break;
      case Expr.THIS:           op(SCode.LoadParam0).type = self; break;
      case Expr.SUPER:          op(SCode.LoadParam0).type = self.base(); break;
      case Expr.FIELD:          loadField((Expr.Field)expr, false); break;
      case Expr.INDEX:          loadIndex((Expr.Index)expr, false); break;
      case Expr.CALL:           call((Expr.Call)expr); break;
      case Expr.CAST :          cast((Expr.Cast)expr); break;
      case Expr.INIT_ARRAY:     initArray((Expr.InitArray)expr); break;
      case Expr.INIT_VIRT:      initVirt((Expr.InitVirt)expr); break;
      case Expr.INIT_COMP:      initComp((Expr.InitComp)expr); break;
      case Expr.STATIC_TYPE:    break;
      case Expr.PROP_SET:       call((Expr.Call)expr); break;
      case Expr.NEW:            newExpr((Expr.New)expr); break;
      case Expr.DELETE:         deleteExpr((Expr.Delete)expr); break;
      default:                  throw err("CodeAsm not done: " + expr.id + " " + expr.toString(), expr.loc);
    }
    return expr;
  }

//////////////////////////////////////////////////////////////////////////
// Literals
//////////////////////////////////////////////////////////////////////////

  public static IrOp loadLiteral(Expr.Literal expr)
  {
    switch (expr.id)
    {
      case Expr.TRUE_LITERAL:       return loadIntOp(1);
      case Expr.FALSE_LITERAL:      return loadIntOp(0);
      case Expr.INT_LITERAL:        return loadIntOp(expr.asInt());
      case Expr.LONG_LITERAL:       return loadLongOp(expr.asLong());
      case Expr.FLOAT_LITERAL:      return loadFloatOp(expr.asFloat());
      case Expr.DOUBLE_LITERAL:     return loadDoubleOp(expr.asDouble());
      case Expr.TIME_LITERAL:       return loadLongOp(expr.asLong());
      case Expr.STR_LITERAL:        return new IrOp(SCode.LoadStr, expr.toCodeString());
      case Expr.BUF_LITERAL:        return new IrOp(SCode.LoadBuf, expr.toCodeString());
      case Expr.NULL_LITERAL:       return loadNull(expr);
      case Expr.TYPE_LITERAL:       return new IrOp(SCode.LoadType, (Type)expr.value);
      case Expr.SLOT_LITERAL:       return new IrOp(SCode.LoadSlot, (Slot)expr.value);
      case Expr.SLOT_ID_LITERAL:    return new IrOp(SCode.LoadSlotId, expr.toCodeString());
      case Expr.SIZE_OF:            return new IrOp(SCode.SizeOf, expr.toCodeString());
      default:                      throw new IllegalStateException(expr.toString());
    }
  }

  public static IrOp loadNull(Expr.Literal expr)  
  {
    if (expr.type.isRef())    return new IrOp(SCode.LoadNull);
    if (expr.type.isBool())   return new IrOp(SCode.LoadNullBool);
    if (expr.type.isFloat())  return new IrOp(SCode.LoadNullFloat);
    if (expr.type.isDouble()) return new IrOp(SCode.LoadNullDouble);
    throw new IllegalStateException("Invalid type for null: " + expr.type + " [" + expr.loc + "]");
  }

  public static IrOp loadIntOp(int val)
  {
    switch (val)
    {
      case -1: return new IrOp(SCode.LoadIM1);
      case 0:  return new IrOp(SCode.LoadI0);
      case 1:  return new IrOp(SCode.LoadI1);
      case 2:  return new IrOp(SCode.LoadI2);
      case 3:  return new IrOp(SCode.LoadI3);
      case 4:  return new IrOp(SCode.LoadI4);
      case 5:  return new IrOp(SCode.LoadI5);
    }

    if (0 <= val && val <= 0xff)
    {
      return new IrOp(SCode.LoadIntU1, val);
    }
    else if (0 <= val && val <= 0xffff)
    {
      return new IrOp(SCode.LoadIntU2, val);
    }                            
    else
    {
      return new IrOp(SCode.LoadInt, val);
    }
  }

  private void loadInt(int val)
  {
    op(loadIntOp(val));
  }

  public static IrOp loadLongOp(long val)
  {             
    if (val == 0) return new IrOp(SCode.LoadL0);
    if (val == 1) return new IrOp(SCode.LoadL1);
    return new IrOp(SCode.LoadLong, val + "L");
  }

  private void loadLong(long val)
  {
    if (val == 0) { op(SCode.LoadL0); return; }
    if (val == 1) { op(SCode.LoadL1); return; }
    if (-1L <= val && val <= 0xffffL)
    {
      loadInt((int)val);
      op(SCode.IntToLong);
    }
    else
    {
      op(loadLongOp(val));
    }
  }

  public static IrOp loadFloatOp(float f)
  {
    if (f == 0.0f) return new IrOp(SCode.LoadF0);
    if (f == 1.0f) return new IrOp(SCode.LoadF1);
    return new IrOp(SCode.LoadFloat, Env.floatFormat(f) + "F");
  }

  private void loadFloat(float val)
  {      
    if (val == 0.0f) { op(SCode.LoadF0); return; }
    if (val == 1.0f) { op(SCode.LoadF1); return; }  
    int ival = Math.round(val);                 
    if ((float)ival == val && -1 <= ival && ival <= 0xffff)
    {
      loadInt(ival);
      op(SCode.IntToFloat);
    }
    else
    {
      op(loadFloatOp(val));
    }
  }

  public static IrOp loadDoubleOp(double d)
  {
    if (d == 0.0d) return new IrOp(SCode.LoadD0);
    if (d == 1.0d) return new IrOp(SCode.LoadD1);
    return new IrOp(SCode.LoadDouble, Env.doubleFormat(d) + "D");
  }

  private void loadDouble(double val)
  {
    if (val == 0.0d) { op(SCode.LoadD0); return; }
    if (val == 1.0d) { op(SCode.LoadD1); return; }  
    long ival = Math.round(val);                 
    if ((double)ival == val && -1L <= ival && ival <= 0xffffL)
    {
      loadInt((int)ival);
      op(SCode.IntToDouble);
    }
    else
    {
      op(loadDoubleOp(val));
    }
  }

//////////////////////////////////////////////////////////////////////////
// UnaryExpr
//////////////////////////////////////////////////////////////////////////

  private void unary(Expr expr, int intOp, int longOp) { unary(expr, intOp, longOp, -1, -1); }
  private void unary(Expr expr, int intOp, int longOp, int floatOp, int doubleOp)
  {
    Expr.Unary unary = (Expr.Unary)expr;
    Type type = unary.operand.type;
    expr(unary.operand);
    if (type.isInteger() || type.isBool())
    {
      op(intOp); return;
    }
    else if (type.isLong())
    {
      if (longOp > 0) { op(longOp); return; }
    }
    else if (type.isFloat())
    {
      if (floatOp > 0) { op(floatOp); return; }
    }
    else if (type.isDouble())
    {
      if (doubleOp > 0) { op(doubleOp); return; }
    }
    throw err("Invalid unary type: " + type.toString(), expr.loc);
  }

//////////////////////////////////////////////////////////////////////////
// BinaryExpr
//////////////////////////////////////////////////////////////////////////

  private void binary(Expr expr, int intOp, int longOp) { binary(expr, intOp, longOp, -1, -1, -1); }
  private void binary(Expr expr, int intOp, int longOp, int floatOp, int doubleOp) { binary(expr, intOp, longOp, floatOp, doubleOp, -1); }
  private void binary(Expr expr, int intOp, int longOp, int floatOp, int doubleOp, int objOp)
  {
    Expr.Binary binary = (Expr.Binary)expr;
    Type type = binary.lhs.type;

    // assemble lhs and rhs onto stack
    expr(binary.lhs);
    expr(binary.rhs);

    // check for pointer arthmetic
    if (type.isArray())
    {
      if (pointerArithmetic(binary)) return;
    }                     
    
    // check type against opcodes supplied
    if (type.isInteger() || type.isBool())
    {
      op(intOp); return;
    }
    else if (type.isLong())
    {
      if (longOp > 0) { op(longOp); return; }
    }
    else if (type.isFloat())
    {
      if (floatOp > 0) { op(floatOp); return; }
    }
    else if (type.isDouble())
    {
      if (doubleOp > 0) { op(doubleOp); return; }
    }
    else if (type.isRef())
    {
      if (objOp > 0) { op(objOp); return; }
    }

    // something slipped thru the cracks
    throw err("Invalid operation " + expr, expr.loc);
  }

  private boolean pointerArithmetic(Expr.Binary expr)
  {
    // check only add or sub
    // if sub negate offset
    switch (expr.id)
    {
      case Expr.ADD: break;
      case Expr.SUB: op(SCode.IntNeg); break;
      default:       return false;
    }

    // add array opcode
    Type of = expr.type.arrayOf();
    if (of.isRef())
    {
      if (of.isConst())
        throw new IllegalStateException("Const array ptr arithmetic not supported");
      else
        op(SCode.AddRefArray);
    }
    else switch (of.sizeof())
    {
      case 1: op(SCode.Add8BitArray);  break;
      case 2: op(SCode.Add16BitArray); break;
      case 4: op(SCode.Add32BitArray); break;
      case 8: op(SCode.Add64BitArray); break;
      default: throw new IllegalStateException();
    }
    return true;
  }

//////////////////////////////////////////////////////////////////////////
// CondExpr
//////////////////////////////////////////////////////////////////////////

  private void cond(Expr.Cond expr, Cond cond)
  {
    switch (expr.id)
    {
      case Expr.COND_OR:  or(expr, cond);  break;
      case Expr.COND_AND: and(expr, cond); break;
      default: throw new IllegalStateException(expr.toString());
    }
  }

  private void or(Expr.Cond expr, Cond cond)
  {
    // if cond is null this is a top level expr which means
    // the result is to push true or false onto the stack;
    // otherwise our only job is to do the various jumps if
    // true or fall-thru if true (used with if statement)
    // NOTE: this code could be further optimized because
    //   it doesn't optimize "a && b || c && c"
    boolean topLevel = cond == null;
    if (topLevel) cond = new Cond();

    // perform short circuit logical-or
    for (int i=0; i<expr.operands.size(); ++i)
    {
      Expr operand = (Expr)expr.operands.get(i);
      expr(operand);
      if (i < expr.operands.size()-1)
        cond.trueJumps.add(jump(SCode.JumpNonZero));
      else
        cond.falseJumps.add(jump(SCode.JumpZero));
    }

    // if top level push true/false onto stack
    if (topLevel) condEnd(cond);
  }

  private void and(Expr.Cond expr, Cond cond)
  {
    // if cond is null this is a top level expr which means
    // the result is to push true or false onto the stack;
    // otherwise our only job is to do the various jumps if
    // true or fall-thru if true (used with if statement)
    // NOTE: this code could be further optimized because
    //   it doesn't optimize "a && b || c && c"
    boolean topLevel = cond == null;
    if (topLevel) cond = new Cond();

    // perform short circuit logical-and
    for (int i=0; i<expr.operands.size(); ++i)
    {
      Expr operand = (Expr)expr.operands.get(i);
      expr(operand);
      cond.falseJumps.add(jump(SCode.JumpZero));
    }

    // if top level push true/false onto stack
    if (topLevel) condEnd(cond);
  }

  private void condEnd(Cond cond)
  {
    // true if always fall-thru
    for (int i=0; i<cond.trueJumps.size(); ++i)
      backpatch((IrOp)cond.trueJumps.get(i));
    op(SCode.LoadI1);
    IrOp end = jump(SCode.Jump);

    // false
    for (int i=0; i<cond.falseJumps.size(); ++i)
      backpatch((IrOp)cond.falseJumps.get(i));
    op(SCode.LoadI0);

    backpatch(end);
  }

//////////////////////////////////////////////////////////////////////////
// Assign
//////////////////////////////////////////////////////////////////////////

  private void assign(Expr.Binary expr)
  {
    switch (expr.lhs.id)
    {
      case Expr.PARAM: assignParam(expr); break;
      case Expr.LOCAL: assignLocal(expr); break;
      case Expr.FIELD: assignField(expr); break;
      case Expr.INDEX: assignIndex(expr); break;
      default: throw err("CodeAsm IllegalState: " + expr.lhs.getClass() + "; " + expr, expr.loc);
    }
  }

  private void assign(Expr expr, int intOp, int longOp) { assign(expr, intOp, longOp, -1, -1); }
  private void assign(Expr expr, int intOp, int longOp, int floatOp, int doubleOp)
  {
    Expr.Binary binary = (Expr.Binary)expr;
    Expr lhs = binary.lhs;
    Expr rhs = binary.rhs;
    Type type = lhs.type;             
    boolean wide = type.isWide();
    boolean leave = expr.leave;         
    
    // load current value, note for fields/index we pass true to dup
    // the target items on the stack for the store back later
    switch (lhs.id)
    {
      case Expr.PARAM: loadParam(((Expr.Param)lhs).def); break;
      case Expr.LOCAL: loadLocal(((Expr.Local)lhs).def); break;
      case Expr.FIELD: loadField((Expr.Field)lhs, true); break;
      case Expr.INDEX: loadIndex((Expr.Index)lhs, true); break;
      default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
    }

    // push rhs onto stack
    expr(rhs);

    // perform the operation
    // check for pointer arthmetic
    if (type.isArray())
    {
      throw err("Pointer arthimetic not supported yet", expr.loc);
    }

    // check type against opcodes supplied
    if (type.isInteger() || type.isBool())
    {
      op(intOp);
    }
    else if (type.isLong() && longOp > 0)
    {
      op(longOp);
    }
    else if (type.isFloat() && floatOp > 0)
    {
      op(floatOp);
    }
    else if (type.isDouble() && doubleOp > 0)
    {
      op(doubleOp);
    }
    else
    {
      throw err("CodeAsm IllegalState: " + expr, expr.loc);
    }

    // if leave, then dup value so that we have a
    // copy of the result on the stack
    if (leave)
    {                    
      IrOp op;     
      switch (lhs.id)
      {
        case Expr.PARAM: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.LOCAL: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.FIELD: op = op(wide ? SCode.Dup2Down2 : SCode.DupDown2); break;
        case Expr.INDEX: op = op(wide ? SCode.Dup2Down3 : SCode.DupDown3); break;
        default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
      }                    
      op.flags = dupDownFlags(lhs);
    }

    // store current value
    switch (lhs.id)
    {
      case Expr.PARAM: storeParam(((Expr.Param)lhs).def); break;
      case Expr.LOCAL: storeLocal(((Expr.Local)lhs).def); break;
      case Expr.FIELD: storeField((Expr.Field)lhs); break;
      case Expr.INDEX: storeIndex((ArrayType)((Expr.Index)lhs).target.type); break;
      default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
    }
  }

  private void increment(Expr.Unary expr)
  {                                    
    Expr operand = expr.operand;
    boolean wide = expr.type.isWide();
    
    // if we are not leaving on the stack,
    // then use prefix because it is faster
    boolean leave = expr.leave;
    if (!leave)
    {
      if (expr.id == Expr.POST_INCR) expr.id = Expr.PRE_INCR;
      if (expr.id == Expr.POST_DECR) expr.id = Expr.PRE_DECR;
    }

    // is this a postfix leave
    boolean post = (expr.id == Expr.POST_INCR || expr.id == Expr.POST_DECR);

    // load current value, note for fields/index we pass true to dup
    // the target items on the stack for the store back later
    switch (operand.id)
    {
      case Expr.PARAM: loadParam(((Expr.Param)operand).def); break;
      case Expr.LOCAL: loadLocal(((Expr.Local)operand).def); break;
      case Expr.FIELD: loadField((Expr.Field)operand, true); break;
      case Expr.INDEX: loadIndex((Expr.Index)operand, true); break;
      default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
    }

    // if we have a postfix leave, then dup current
    // value so that the we have a copy of the old value
    if (post)
    {                        
      IrOp op;
      switch (operand.id)
      {
        case Expr.PARAM: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.LOCAL: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.FIELD: op = op(wide ? SCode.Dup2Down2 : SCode.DupDown2); break;
        case Expr.INDEX: op = op(wide ? SCode.Dup2Down3 : SCode.DupDown3); break;
        default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
      }           
      op.flags = dupDownFlags(operand);
    }

    // increment or decrement
    if (expr.id == Expr.PRE_INCR || expr.id == Expr.POST_INCR)
    {
      if (expr.type.isInteger())     { op(SCode.IntInc); }
      else if (expr.type.isLong())   { op(SCode.LoadL1); op(SCode.LongAdd); }
      else if (expr.type.isFloat())  { op(SCode.LoadF1); op(SCode.FloatAdd); }
      else if (expr.type.isDouble()) { op(SCode.LoadD1); op(SCode.DoubleAdd); }
      else throw err("Invalid increment type: " + expr.type, expr.loc);
    }
    else
    {
      if (expr.type.isInteger())     { op(SCode.IntDec); }
      else if (expr.type.isLong())   { op(SCode.LoadL1); op(SCode.LongSub); }
      else if (expr.type.isFloat())  { op(SCode.LoadF1); op(SCode.FloatSub); }
      else if (expr.type.isDouble()) { op(SCode.LoadD1); op(SCode.DoubleSub); }
      else throw err("Invalid decrement type: " + expr.type, expr.loc);
    }

    // if prefix leave, then duplicate this value
    if (leave && !post)
    {                      
      IrOp op;
      switch (operand.id)
      {
        case Expr.PARAM: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.LOCAL: op = op(wide ? SCode.Dup2 : SCode.Dup); break;
        case Expr.FIELD: op = op(wide ? SCode.Dup2Down2 : SCode.DupDown2); break;
        case Expr.INDEX: op = op(wide ? SCode.Dup2Down3 : SCode.DupDown3); break;
        default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
      }                       
      op.flags = dupDownFlags(operand);
    }

    // store current value
    switch (operand.id)
    {
      case Expr.PARAM: storeParam(((Expr.Param)operand).def); break;
      case Expr.LOCAL: storeLocal(((Expr.Local)operand).def); break;
      case Expr.FIELD: storeField((Expr.Field)operand); break;
      case Expr.INDEX: storeIndex((ArrayType)((Expr.Index)operand).target.type); break;
      default: throw err("CodeAsm IllegalState: " + expr, expr.loc);
    }
  }

  int dupDownFlags(Expr expr)
  {
    // in Sedona we actually put the static base address
    // on the stack when getting/setting static fields, but 
    // on Java we don't, we need to use different instructions
    // for leaving the value on the stack on dupdown
    if (expr.id == Expr.FIELD && ((Expr.Field)expr).field.isStatic())
      return IrOp.STATIC_FIELD;
    
    return 0;
  }

//////////////////////////////////////////////////////////////////////////
// Params
//////////////////////////////////////////////////////////////////////////

  private void loadParam(ParamDef param)
  {   
    IrOp op = null;
    if (param.type.isWide())
    {
      op = op(SCode.LoadParamWide, param.index);
    }   
    else
    {    
      switch (param.index)
      {
        case 0:  op = op(SCode.LoadParam0); break;
        case 1:  op = op(SCode.LoadParam1); break;
        case 2:  op = op(SCode.LoadParam2); break;
        case 3:  op = op(SCode.LoadParam3); break;
        default: op = op(SCode.LoadParam, param.index); break;
      }          
    }    
    op.type = param.type;
  }

  private void storeParam(ParamDef param)
  {   
    IrOp op;
    if (param.type.isWide())
    {
      op = op(SCode.StoreParamWide, param.index);
    }
    else
    {   
      op = op(SCode.StoreParam, param.index);
    }      
    op.type = param.type;
  }

  private void assignParam(Expr.Binary assign)
  {
    boolean wide = assign.type.isWide();
    expr(assign.rhs);
    if (assign.leave) op(wide ? SCode.Dup2 : SCode.Dup);
    storeParam(((Expr.Param)assign.lhs).def);
  }

//////////////////////////////////////////////////////////////////////////
// Locals
//////////////////////////////////////////////////////////////////////////

  private void loadLocal(Stmt.LocalDef stmt)
  {       
    IrOp op = null;
    if (stmt.type.isWide())
    {
      op = op(SCode.LoadLocalWide, stmt.index);
    }
    else
    {    
      switch (stmt.index)
      {
        case 0:  op = op(SCode.LoadLocal0); break;
        case 1:  op = op(SCode.LoadLocal1); break;
        case 2:  op = op(SCode.LoadLocal2); break;
        case 3:  op = op(SCode.LoadLocal3); break;
        case 4:  op = op(SCode.LoadLocal4); break;
        case 5:  op = op(SCode.LoadLocal5); break;
        case 6:  op = op(SCode.LoadLocal6); break;
        case 7:  op = op(SCode.LoadLocal7); break;
        default: op = op(SCode.LoadLocal, stmt.index); break;
      }          
    }    
    op.type = stmt.type;
  }

  private void storeLocal(Stmt.LocalDef stmt)
  {                          
    IrOp op = null;
    if (stmt.type.isWide())
    {
      op = op(SCode.StoreLocalWide, stmt.index);
    }
    else
    {    
      switch (stmt.index)
      {
        case 0:  op = op(SCode.StoreLocal0); break;
        case 1:  op = op(SCode.StoreLocal1); break;
        case 2:  op = op(SCode.StoreLocal2); break;
        case 3:  op = op(SCode.StoreLocal3); break;
        case 4:  op = op(SCode.StoreLocal4); break;
        case 5:  op = op(SCode.StoreLocal5); break;
        case 6:  op = op(SCode.StoreLocal6); break;
        case 7:  op = op(SCode.StoreLocal7); break;
        default: op = op(SCode.StoreLocal, stmt.index); break;
      }
    }   
    op.type = stmt.type;
  }

  private void assignLocal(Expr.Binary assign)
  {
    boolean wide = assign.type.isWide();
    expr(assign.rhs);
    if (assign.leave) op(wide ? SCode.Dup2 : SCode.Dup);
    storeLocal(((Expr.Local)assign.lhs).def);
  }

//////////////////////////////////////////////////////////////////////////
// Field
//////////////////////////////////////////////////////////////////////////

  private void loadField(Expr.Field expr, boolean readyStoreBack)
  {
    Field field = expr.field;
    Type type = field.type();

    if (field.isDefine())
    {
      op(SCode.LoadDefine, field);
      return;
    }

    if (field.isConst() && field.isStatic())
    {
      if (!type.isRef())
        throw err("static const can only be used with ref fields", expr.loc);
      op(SCode.LoadConstStatic, field);
      return;
    }

    if (field.isStatic())
      op(SCode.LoadDataAddr);
    else
      expr(expr.target);
    if (readyStoreBack) op(SCode.Dup);

    // if safeNav check for null
    IrOp isNull = preSafeNav(expr);

    // must use U1 version for IR
    if (type.isRef())
    {
      if (field.isInline())
       op(SCode.LoadInlineFieldU1, field);
      else if (field.isConst())
       op(SCode.LoadConstFieldU1, field);
      else
        op(SCode.LoadRefFieldU1, field);
    }
    else switch (type.sizeof())
    {
      case 1: op(SCode.Load8BitFieldU1,  field); break;
      case 2: op(SCode.Load16BitFieldU1, field); break;
      case 4: op(SCode.Load32BitFieldU1, field); break;
      case 8: op(SCode.Load64BitFieldU1, field); break;
      default: throw new IllegalStateException();
    }             
    
    // if safeNav handle null case
    postSafeNav(expr, isNull);
  }

  private void storeField(Expr.Field expr)
  {
    Field field = expr.field;
    Type type = field.type();

    if (field.isDefine())
      throw err("Cannot store to a define field", expr.loc);

    // must use U1 version for IR
    if (type.isRef())
    {
      if (field.isInline() || field.isConst())
        throw new IllegalStateException();
      else
        op(SCode.StoreRefFieldU1, field);
    }
    else switch (type.sizeof())
    {
      case 1: op(SCode.Store8BitFieldU1,  field); break;
      case 2: op(SCode.Store16BitFieldU1, field); break;
      case 4: op(SCode.Store32BitFieldU1, field); break;
      case 8: op(SCode.Store64BitFieldU1, field); break;
      default: throw new IllegalStateException();
    }
  }

  private void assignField(Expr.Binary assign)
  {
    Expr.Field lhs = (Expr.Field)assign.lhs;
    Field field = lhs.field;    
    boolean wide = field.type().isWide();

    if (field.isStatic())
      op(SCode.LoadDataAddr);
    else
      expr(lhs.target);

    expr(assign.rhs);
    if (assign.leave) 
    { 
      IrOp op = op(wide ? SCode.Dup2Down2 : SCode.DupDown2);
      op.flags = dupDownFlags(lhs);
    }
    storeField(lhs);
  }

//////////////////////////////////////////////////////////////////////////
// Index
//////////////////////////////////////////////////////////////////////////

  private void loadIndex(Expr.Index expr, boolean readyStoreBack)
  {
    // push target, index
    expr(expr.target);
    expr(expr.index);

    if (readyStoreBack) op(SCode.Dup2);

    loadArray((ArrayType)expr.target.type);
  }

  private void loadArray(ArrayType arrayType)
  {                                                 
    Type of = arrayType.of;
    if (of.isRef())
    {
      if (arrayType.isConst)
        op(SCode.LoadConstArray).type = of;
      else
        op(SCode.LoadRefArray).type = of;
    }
    else switch (arrayType.of.sizeof())
    {
      case 1: op(SCode.Load8BitArray).type = of;  break;
      case 2: op(SCode.Load16BitArray).type = of; break;
      case 4: op(SCode.Load32BitArray).type = of; break;
      case 8: op(SCode.Load64BitArray).type = of; break;
      default: throw new IllegalStateException();
    }
  }

  private void storeIndex(ArrayType arrayType)
  {                       
    IrOp op;
    Type of = arrayType.of;                    
    if (arrayType.of.isRef())
    {
      if (arrayType.isConst)
        throw new IllegalStateException();
      else
        op = op(SCode.StoreRefArray);
    }
    else switch (arrayType.of.sizeof())
    {
      case 1: op = op(SCode.Store8BitArray);  break;
      case 2: op = op(SCode.Store16BitArray); break;
      case 4: op = op(SCode.Store32BitArray); break;
      case 8: op = op(SCode.Store64BitArray); break;
      default: throw new IllegalStateException();
    }              
    op.type = of;
  }

  private void assignIndex(Expr.Binary assign)
  {
    Expr.Index lhs = (Expr.Index)assign.lhs;
    boolean wide = lhs.type.isWide();

    // push target, index
    expr(lhs.target);
    expr(lhs.index);

    // assign
    expr(assign.rhs);                
    if (assign.leave) op(wide ? SCode.Dup2Down3 : SCode.DupDown3);
    storeIndex((ArrayType)lhs.target.type);
  }

//////////////////////////////////////////////////////////////////////////
// Call
//////////////////////////////////////////////////////////////////////////

  private void call(Expr.Call call)
  {
    Method m = call.method;
    Type ret = m.returnType();
    boolean isVoid = ret.isVoid();
    Expr[] args = call.args; 
    Expr.Interpolation interpolation = null;
    
    if (call.target != null)
      expr(call.target); 

    // if safeNav check for null
    IrOp isNull = preSafeNav(call);
    
    if (args.length == 1 && args[0].id == Expr.INTERPOLATION)
    {
      interpolation = (Expr.Interpolation)args[0];  
      expr(interpolation.first());
    }
    else
    {
      for (int i=0; i<args.length; ++i)
        expr(args[i]);
    }

    // a property set is a normal method call to one of the
    // Component.setx() methods; the difference is that if a
    // leave was performed then we need to duplicate the value
    // argument such that it is left on the stack
    if (call.id == Expr.PROP_SET && call.leave)
    {
      call.leave = false;
      op(call.type.isWide() ? SCode.Dup2Down3 : SCode.DupDown3);
    }

    if (m.isVirtual() || m.isOverride())
    {
      if (call.target.id == Expr.SUPER)
        op(SCode.Call, m);  // non-virtual
      else
        op(SCode.CallVirtual, m);
    }
    else if (m.isNative())
    {
      if (isVoid)
        op(SCode.CallNativeVoid, m);
      else if (ret.isWide())
        op(SCode.CallNativeWide, m);
      else
        op(SCode.CallNative, m);
    }
    else
    {
      op(SCode.Call, m);
    }              
    
    // if interpolation, then we need to use the return 
    // OutStream to print the rest of the interpolation parts
    if (interpolation != null)
    {
      for (int i=1; i<interpolation.parts.size(); ++i)
      {
        Expr part = (Expr)interpolation.parts.get(i);
        Method printMethod = interpolation.printMethods[i];
        expr(part);
        op(SCode.Call, printMethod);
      }
    }

    // if safeNav handle null case     
    postSafeNav(call, isNull);

    // pop return if not used
    if (!call.leave && !isVoid)
      op(ret.isWide() ? SCode.Pop2 : SCode.Pop);
  }

//////////////////////////////////////////////////////////////////////////
// Elvis
//////////////////////////////////////////////////////////////////////////

  private void elvis(Expr.Binary expr)
  {
    expr(expr.lhs);
    op(SCode.Dup);               
    op(SCode.LoadNull);
    op(SCode.ObjEq);
    IrOp isNullJump = jump(SCode.JumpNonZero);
    IrOp endJump = jump(SCode.Jump);
    backpatch(isNullJump);
    op(SCode.Pop);
    expr(expr.rhs);
    backpatch(endJump);
  }

//////////////////////////////////////////////////////////////////////////
// Ternary
//////////////////////////////////////////////////////////////////////////

  private void ternary(Expr.Ternary expr)
  {
    expr(expr.cond);
    IrOp falseJump = jump(SCode.JumpZero);
    expr(expr.trueExpr);
    IrOp endJump = jump(SCode.Jump);
    backpatch(falseJump);
    expr(expr.falseExpr);
    backpatch(endJump);
  }

//////////////////////////////////////////////////////////////////////////
// Cast
//////////////////////////////////////////////////////////////////////////

  private void cast(Expr.Cast cast)
  {
    expr(cast.target);
    int castOpcode = castOpcode(cast.target.type, cast.type, cast.loc);
    if (castOpcode != 0) op(castOpcode, cast.type);
  }

  private int castOpcode(Type from, Type to, Location loc)
  {
    if (from.isInteger())
    {
      if (to.isInteger()) return 0;
      if (to.isLong())    return SCode.IntToLong;
      if (to.isFloat())   return SCode.IntToFloat;
      if (to.isDouble())  return SCode.IntToDouble;
      if (to.isRef())     return 0;
    }

    if (from.isLong())
    {
      if (to.isInteger()) return SCode.LongToInt;
      if (to.isLong())    return 0;
      if (to.isFloat())   return SCode.LongToFloat;
      if (to.isDouble())  return SCode.LongToDouble;
    }

    if (from.isFloat())
    {
      if (to.isInteger()) return SCode.FloatToInt;
      if (to.isLong())    return SCode.FloatToLong;
      if (to.isFloat())   return 0;
      if (to.isDouble())  return SCode.FloatToDouble;
    }

    if (from.isDouble())
    {
      if (to.isInteger()) return SCode.DoubleToInt;
      if (to.isLong())    return SCode.DoubleToLong;
      if (to.isFloat())   return SCode.DoubleToFloat;
      if (to.isDouble())  return 0;
    }

    if (from.isRef())
    {
      if (to.isRef()) return SCode.Cast;
    }

    throw err("Cast " + from + " -> " + to, loc);
  }

//////////////////////////////////////////////////////////////////////////
// Safe Nav
//////////////////////////////////////////////////////////////////////////

  private IrOp preSafeNav(Expr.Name expr)
  {
    if (!expr.safeNav) return null;
    if (expr.target == null) throw err("Internal safeNav error", expr.loc);
    op(SCode.Dup);     
    op(SCode.LoadNull);
    op(SCode.ObjEq);
    return jump(SCode.JumpNonZero);
  }

  private void postSafeNav(Expr.Name expr, IrOp isNull)
  {
    if (!expr.safeNav) return;
    
    if (expr instanceof Expr.Call && expr.type.isVoid())
    {
      IrOp end = jump(SCode.Jump);
      backpatch(isNull);
      op(SCode.Pop);    
      backpatch(end);               
    }
    else if (expr.type.isPrimitive()) 
    {
      IrOp end = jump(SCode.Jump);
      backpatch(isNull);
      op(SCode.Pop);     
      op(safeNavLoadZero(expr));    
      backpatch(end);               
    }                
    else
    {
      backpatch(isNull);
    }
  } 
  
  private int safeNavLoadZero(Expr expr)
  {                    
    Type t = expr.type;
    if (t.isFloat())  return SCode.LoadF0;
    if (t.isLong())   return SCode.LoadL0;
    if (t.isDouble()) return SCode.LoadD0;
    return SCode.LoadI0;
  }

//////////////////////////////////////////////////////////////////////////
// Heap Management
//////////////////////////////////////////////////////////////////////////

  private void newExpr(Expr.New expr)
  {   
    int ignore = IrOp.IGNORE_JAVA;         
    if (expr.arrayLength == null)
    {                      
      op(SCode.SizeOf, expr.of).flags = ignore;
      op(SCode.CallNative, ns.sysType.slot("malloc")).type = expr.type;

      // call constructor for newly allocated object
      op(SCode.Dup);
      op(SCode.Call, expr.of.slot(MethodDef.INSTANCE_INIT));
    }
    else
    {
      if (expr.of.isRef())                           
        op(SCode.LoadDefine, ns.sysType.slot("sizeofRef")).flags = ignore;
      else
        op(SCode.SizeOf, expr.of).flags = ignore;
      expr(expr.arrayLength);
      op(SCode.IntMul).flags = ignore;
      op(SCode.CallNative, ns.sysType.slot("malloc")).type = expr.type;
    }
  }

  private void deleteExpr(Expr.Delete expr)
  {                       
    expr(expr.target);
    op(SCode.CallNativeVoid, ns.sysType.slot("free"));
  }

//////////////////////////////////////////////////////////////////////////
// Initializers
//////////////////////////////////////////////////////////////////////////

  private void initArray(Expr.InitArray expr)
  {
    Type of = expr.field.type.arrayOf();
    expr(expr.field);
    expr(expr.length);
    op(SCode.SizeOf, of.qname());
    op(SCode.InitArray);
  }

  private void initVirt(Expr.InitVirt expr)
  {
    op(SCode.LoadParam0).type = self;
    op(SCode.InitVirt, expr.type.qname());
  }

  private void initComp(Expr.InitComp expr)
  {
    op(SCode.LoadParam0).type = self;
    op(SCode.InitComp, expr.type.qname());
  }

//////////////////////////////////////////////////////////////////////////
// Logging
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Assemble a logging statement such as log.error().
   * We wrap a logging call with both a ifdefine (to compile
   * out) and a jump around (to avoid running thru code
   * on a disabled log level).
   */
  private void logStmt(Expr.Call call)
  {                        
    // don't even allow calling a logging method
    // on anything other than the define field itself; 
    // otherwise we don't know how to check if the
    // log is enabled or disabled
    Expr target = call.target;
    if (target.id != Expr.FIELD || !((Expr.Field)target).field.isDefine())
    {
      err("Logging must be called on a define field", call.loc);
      return;
    } 
    Field define = ((Expr.Field)target).field;
    
    // this is the method we call to check 
    // if the level is enable or disabled
    Method isLevel = ns.resolveMethod("sys::Log.is" + TextUtil.capitalize(call.method.name()));    
    
    // sys::OutStream.nl()  method
    Method nl = ns.resolveMethod("sys::OutStream.nl");
    
    op(SCode.LoadDefine, define);          // load define
    op(SCode.Call, isLevel);               // check if level enabled
    IrOp skipJump = jump(SCode.JumpZero);  // jump over if disabled
    call.leave = true;                     // force OutStream to be left on stack
    call(call);                            // make normal call
    op(SCode.Call, nl);                    // force newline
    op(SCode.Pop);                         // now pop OutStream from stack
    backpatch(skipJump);                   // skip jumps to here
  }

//////////////////////////////////////////////////////////////////////////
// Code Buffer
//////////////////////////////////////////////////////////////////////////

  /**
   * Append a opcode with no arguments.
   */
  private IrOp op(int opcode)
  {
    return op(new IrOp(opcode, (String)null));
  }

  /**
   * Append a opcode with integer arg
   */
  private IrOp op(int opcode, int arg)
  {
    return op(new IrOp(opcode, arg));
  }

  /**
   * Append a opcode with arg
   */
  private IrOp op(int opcode, String arg)
  {
    return op(new IrOp(opcode, arg));    
  }

  /**
   * Append a opcode with type arg
   */
  private IrOp op(int opcode, Type arg)
  {                                              
    IrOp op = new IrOp(opcode, arg);
    op(op);
    return op;  
  }

  /**
   * Append a opcode with slot arg
   */
  private IrOp op(int opcode, Slot arg)
  {                                              
    IrOp op = new IrOp(opcode, arg);
    op(op);
    return op;  
  }

  /**
   * Append a opcode
   */
  private IrOp op(IrOp op)
  {
    op.index = code.size();  
    op.loc = loc;
    code.add(op);
    return op;
  }

//////////////////////////////////////////////////////////////////////////
// Jumping
//////////////////////////////////////////////////////////////////////////

  /**
   * Get operation at the specified position.
   */
  public IrOp at(int pos)
  {
    return (IrOp)code.get(pos);
  }

  /**
   * Get index of next instruction.
   */
  private int mark()
  {
    return code.size();
  }

  /**
   * Add jump instruction with operation index.
   * Return op to backpatch.
   */
  private IrOp jump(int opcode) { return jump(opcode, 0); }
  private IrOp jump(int opcode, int mark)
  {
    op(opcode, String.valueOf(mark));
    return at(code.size()-1);
  }

  /**
   * Backpatch a jump offset.
   */
  private void backpatch(IrOp op) { backpatch(op, mark()); }
  private void backpatch(IrOp op, int mark)
  {
    op.arg = String.valueOf(mark);
  }                          
  
  /**
   * Finish the code and jumps.
   */
  private void finish()
  {                            
    // backpatch any gotos
    for (int i=0; gotos != null && i<gotos.size(); ++i)
    {                                                 
      Stmt.Goto stmt = (Stmt.Goto)gotos.get(i);
      backpatch(stmt.op, stmt.destStmt.mark);
    }
  }

//////////////////////////////////////////////////////////////////////////
// LoopStack
//////////////////////////////////////////////////////////////////////////

  static class Cond
  {
    ArrayList trueJumps  = new ArrayList(4);  // jumps to backpatch
    ArrayList falseJumps = new ArrayList(4);  // jumps to backpatch
  }

//////////////////////////////////////////////////////////////////////////
// LoopStack
//////////////////////////////////////////////////////////////////////////

  static class Loop
  {
    Loop(Stmt stmt) { this.stmt = stmt; }

    Stmt stmt;  //  Stmt.While or Stmt.For
    ArrayList breaks    = new ArrayList(4);  // IrOp jumps to patch
    ArrayList continues = new ArrayList(4);  // IrOp jumps to patch
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public TypeAsm parent;
  public Type self;
  public Location loc;
  public ArrayList code = new ArrayList();
  public Stack loopStack = new Stack();  
  ArrayList gotos;

}
