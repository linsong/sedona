//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   8 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * CheckErrors is used to walk the entire AST down to leaf expressions
 * and perform as much of the error checking as possible in one single
 * step for batch error reporting.  A non-complete list of some of the 
 * things we do in this step:
 *  - flag checking for types and slots
 *  - type checking
 *  - coercion of null as bool, float
 */
public class CheckErrors
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public CheckErrors(Compiler compiler)
  {
    super(compiler);  
    this.isSys = compiler.ast.name.equals("sys");
  }

  public void run()
  {
    log.debug("  CheckErrors");
    walkAst(WALK_TO_EXPRS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  public void enterType(TypeDef t)
  {
    super.enterType(t);
    this.hasUnsizedInlineArray = false;
    checkTypeFlags(t);
    checkBase(t);
    checkAbstractMethods(t);
  }

  private void checkTypeFlags(TypeDef t)
  {
    Location loc = t.loc;

    if (t.isAbstract() && t.isFinal())
      err("Invalid combination of 'abstract' and 'final' modifiers", loc);
  }

  private void checkBase(TypeDef t)
  {
    if (t.base == null) return;

    if (t.base.isFinal())
      err("Cannot extend final class '" + t.base + "'", t.loc);
    
    if (t.base.isInternal() && !t.base.kit().name().equals(t.kit().name()))
      err("Cannot extend internal class '" + t.base + "'. Not in same kit.", t.loc);

    checkDepend(t.base, t.loc);
  }

  private void checkAbstractMethods(TypeDef t)
  {
    if (t.isAbstract()) return;

    // if not abstract make sure all methods are concrete
    Slot[] slots = t.slots();
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (!slot.isAbstract()) continue;
      if (slot.parent() == t)
      {
        err("Class '" + t.name + "' must be abstract since it contains abstract methods", t.loc);
        return;
      }
      else
      {
        err("Concrete class '" + t.name + "' must override '" + slot.qname() + "'", t.loc);
      }
    }
  }

  private void checkDepend(Type usedType, Location loc)
  {
    if (!TypeUtil.isTestOnly(curType) && TypeUtil.isTestOnly(usedType))
      err("Runtime class '" + curType.qname() + "' cannot depend on test class '" + usedType.qname() + "'", loc);
  }

//////////////////////////////////////////////////////////////////////////
// Field
//////////////////////////////////////////////////////////////////////////

  public void enterField(FieldDef f)
  {
    super.enterField(f);
    checkProtectionFlags(f);
    checkFieldFlags(f);
    checkFieldInit(f);
    checkFieldDefine(f);
    checkFieldProp(f);
    checkFieldArray(f);
    checkFieldCtor(f);
    checkFieldDepend(f);   
  }

  private void checkFieldFlags(FieldDef f)
  {
    Type type = f.type;
    Location loc = f.loc;

    if (f.isAbstract())     err("Cannot use 'abstract' modifier on field", loc);
    else if (f.isVirtual()) err("Cannot use 'virtual' modifier on field", loc);
    if (f.isNative())       err("Cannot use 'native' modifier on field", loc);
    if (f.isOverride())     err("Cannot use 'override' modifier on field", loc);
    if (f.isAction())       err("Cannot use 'action' modifier on field", loc);

    if (f.isDefine())
    {
      if (f.isInline())     err("Cannot use 'inline' modifier on define", loc);
    }

    if (f.isProperty())
    {
      if (f.isStatic())     err("Cannot use 'static' modifier on property", loc);
      if (f.isConst())      err("Cannot use 'const' modifier on property", loc);
      if (!f.isPublic())    err("Properties must be public", loc);
    }              
    
    if (f.isConst() && !f.isDefine())
    {
      if (!curType.kit().name().equals("sys"))
        err("Only sys types may use 'const' modifier", loc);
    }
    
    if (f.isInline())
    {
      if (type.isAbstract())
        err("Cannot inline fields whose type is abstract", loc);
    }

    if (f.isInline() && !curType.kit.name.equals("sys"))
    {
      if (type.isPrimitive()) err("Cannot use 'inline' modifier on '" + type + "' field", loc);
      else if (type.isConst()) err("Cannot use 'inline' modifier with const type '" + type + "'", loc);
    }
  }

  private void checkFieldInit(FieldDef f)
  {
    if (f.init == null) return;
    Type type = f.type;

    if (f.init.id == Expr.INIT_ARRAY)
    {
      if (!f.isInline()) err("Cannot use '{...}' initializer on non-inline field", f.loc);
      else if (!type.isArray()) err("Cannot use '{...}' initializer on non-array type field", f.loc);
      else if (!type.arrayOf().isRef()) err("Cannot use '{...}' initializer on '" + type + "' field", f.loc);
    }
    
    if (f.init.id == Expr.ARRAY_LITERAL)
    {                                    
      Object[] array = ((Expr.Literal)f.init).asArray();
      if (array.length == 0)
        err("Cannot create empty array literal", f.init.loc);
      if (!type.isArray()) err("Cannot use array literal with non-array type", f.init.loc);     
      Type of = type.arrayOf();
      for (int i=0; i<array.length; ++i)
      {                
        Object v = array[i];
        if (!isArrayLiteralValueOk(of, v))
        { 
          err("Invalid literal value " + TypeUtil.toCodeStringSafe(v) + " for " + type + " array literal", f.init.loc);
          break;
        }
      }
    }
    
    if (f.type.isBuf() && f.isInline())
    {
      // Note: checkCall() will report this error.
      if (f.ctorArgs == null || f.ctorArgs.length == 0) return;
      
      int max = f.ctorArgs[0].toIntLiteral().intValue();
      // Note: STR_LITERAL check makes sure there is room for the null terminator
      if ((f.init.id == Expr.BUF_LITERAL && f.init.toLiteral().asBuf().size > max) ||
          (f.init.id == Expr.STR_LITERAL && f.init.toLiteral().asString().length() >= max))
      {
        err("Buf is too small to hold initial value: " + f.toString(), f.init.loc); 
      }
    }
  }                          
  
  private boolean isArrayLiteralValueOk(Type of, Object val)
  {                       
    if (val == null) return false;                       
    Class cls = val.getClass();
    if (of.isByte())    return cls == Integer.class;
    if (of.isShort())   return cls == Integer.class;
    if (of.isInteger()) return cls == Integer.class;
    if (of.isLong())    return cls == Long.class;
    if (of.isFloat())   return cls == Float.class;
    if (of.isDouble())  return cls == Double.class;
    if (of.isStr())     return cls == String.class;
    return false;
  }

  private void checkFieldDefine(FieldDef f)
  {
    if (!f.isDefine()) return;
    String qname = f.qname;

    if (f.init == null)
    {
      if (!f.type.isLog())
        err("Define '" + qname + "' missing value definition", f.loc);
    }
    else 
    {
      if (!f.init.isLiteral() && f.init.id != Expr.ARRAY_LITERAL)
        err("Define '" + qname + "' must be defined as literal", f.loc);
    }        
    
    if (!isValidDefineType(f.type))
      err("Unsupported type '" + f.type + "' for define field", f.loc);
    
    if (f.init != null && !f.type.isArray())
    { 
      // Check for init type compatibility (coercing null to correct type as needed)
      // NOTE: array type checking is done in checkFieldInit
      if (!f.type.equals(f.init.type) && !f.init.isNullLiteral(f.type))
        err("Define field '" + f.name + "' has type '" + f.type + 
                 "', but is initialized with expression of type '" + f.init.type + "'", f.loc); 
    }

  }                       
  
  private boolean isValidDefineType(Type t)
  {
    if (t.isArray())
    {    
      Type of = t.arrayOf();  
      if (of.isByte())   return true;
      if (of.isShort())  return true;
      if (of.isInt())    return true;
      if (of.isLong())   return true;
      if (of.isFloat())  return true;
      if (of.isDouble()) return true;
      if (of.isStr())    return true;
    }
    else
    {
      if (t.isBool())    return true;
      if (t.isInt())     return true;
      if (t.isLong())    return true;
      if (t.isFloat())   return true;
      if (t.isDouble())  return true;
      if (t.isStr())     return true;
      if (t.isLog())     return true;
    }             
    return false;
  }

  private void checkFieldProp(FieldDef f)
  {
    if (!f.isProperty())
    {
      if (f.facets().getb("config"))
        err("Field '" + f.name + "' is marked @config, but is not a property", f.loc);
      else if (f.facets().getb("asStr"))
        err("Field '" + f.name + "' is marked @asStr, but is not a property", f.loc);
      return;
    }
    
    String qname = f.qname;
    if (!f.type.isBool()   &&
        !f.type.isByte()   &&
        !f.type.isShort()  &&
        !f.type.isInt()    &&
        !f.type.isLong()   &&
        !f.type.isFloat()  &&
        !f.type.isDouble() &&
        !f.type.isBuf())
      err("Unsupported type '" + f.type + "' for property '" + qname + "'", f.loc);
  }

  private void checkFieldArray(FieldDef f)
  {
    Type type = f.type;
    if (!type.isArray()) return;

    if (f.isInline() && type.arrayLength() == null)
    {
      // make special exception for sys const arrays
      if (curType.kit.name.equals("sys") && f.isConst())
      {
        // ok
      }
      else if (hasUnsizedInlineArray)
      {
        err("The class '" + curType.name + "' can only have have one unsized array", f.loc);
      }
      else
      {
        hasUnsizedInlineArray = true;
        if (f.ctorLengthParam <= 0)
        {
          err("Unsized array '" + f.name + "' must have length assigned in constructor", f.loc);
        }
      }
    }
    else
    {
      if (f.ctorLengthParam > 0)
        err("Cannot specify length of non-inline field '" + f.name + "'", f.loc);
    }
  }

  private void checkFieldCtor(FieldDef f)
  {
    if (f.ctorArgs == null) return;

    // can't use a constructor on a non-inline field
    if (!f.isInline())
      err("Cannot use constructor on non-inline field '" + f.name + "'", f.loc);

    // NOTE: because the ctor calls are automatically
    // inlined into my ctor by Normalize, the normal
    // checkArgs() methods will do normal arg checking

    if (f.ctorArgs != null && f.ctorArgs.length > 0)
    {
      Field unsizedField = TypeUtil.getUnsizedArrayField(f.type);
      if (unsizedField != null)
      {
        Expr ctorArg = f.ctorArgs[unsizedField.ctorLengthParam()-1];
        if (!ctorArg.isLiteral() && !ctorArg.isDefine())
        {
          err("Constructor argument must be literal or define", ctorArg.loc);
        }             
        
        // if this is a Buf (or asStr) property, then
        // map the length to the "max" facet
        else if (f.isProperty())
        {                  
          Integer max = ctorArg.toIntLiteral();
          if (max != null)
          {          
            f.setFacet("max", max.intValue());
          }
        }
      }
    }
  }

  private void checkFieldDepend(FieldDef f)
  {
    checkDepend(f.type, f.loc);
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    checkProtectionFlags(m);
    checkMethodFlags(m);
    checkMethodMax(m);
    checkMethodValidVars(m);
    checkMethodAction(m);
    checkMethodCtor(m);
    checkMethodDepend(m);
  }

  private void checkMethodFlags(MethodDef m)
  {
    Location loc = m.loc;

    if (m.isInline())   err("Cannot use 'inline' modifier on method", loc);
    if (m.isConst())    err("Cannot use 'const' modifier on method", loc);
    if (m.isProperty()) err("Cannot use 'property' modifier on method", loc);

    if (m.isNative())
    {
      if (m.isAbstract()) err("Invalid combination of 'native' and 'abstract' modifiers", loc);
      else if (m.isVirtual()) err("Invalid combination of 'native' and 'virtual' modifiers", loc);
      if (m.isOverride()) err("Invalid combination of 'native' and 'override' modifiers", loc);
    }

    if (m.isAction())
    {
      if (m.isStatic()) err("Cannot use 'static' modifier on action", loc);
      if (!m.isPublic()) err("Actions must be public", loc);
    }

    if (m.isStatic())
    {
      if (m.isAbstract()) err("Invalid combination of 'static' and 'abstract' modifiers", loc);
      else if (m.isVirtual()) err("Invalid combination of 'static' and 'virtual' modifiers", loc);
      if (m.isOverride()) err("Invalid combination of 'static' and 'override' modifiers", loc);
    }

    if (m.isAbstract())
    {
      if (m.isOverride()) err("Invalid combination of 'abstract' and 'override' modifiers", loc);
    }

    if (m.isVirtual())
    {
      if (!m.parent().isaVirtual()) err("Virtual methods can only be used on sys::Virtual subclasses", loc);
    }
  }

  private void checkMethodMax(MethodDef m)
  {
    // check max param
    if (m.params.length > SCode.vmMaxParams)
      err("Too many parameters", m.loc);

    // check max local
    if (m.maxLocals > SCode.vmMaxLocals)
      err("Too many locals", m.loc);
  }

  private void checkMethodValidVars(MethodDef m)
  {
    // cannot use byte, short for return
    checkValidVar("Return", m.ret, m.loc);

    // cannot use byte, short for parameters
    for (int i=0; i<m.params.length; ++i)
      checkValidVar("Parameter", m.params[i].type, m.params[i].loc);
  }

  private void checkValidVar(String kind, Type type, Location loc)
  {
    if (type.isInteger() && !type.isInt())
      err(kind + " type must be int, not " + type, loc);
  }

  private void checkMethodAction(MethodDef m)
  {
    if (!m.isAction()) return;
    String qname = m.qname;

    if (!m.ret.isVoid())
      err("Action '" + qname + "' must be have void return type", m.loc);

    if (m.params.length > 0)
    {
      Type argType = m.params[0].type;
      if (!argType.isBool()   &&
          !argType.isInt()    &&
          !argType.isLong()   &&
          !argType.isFloat()  &&
          !argType.isDouble() &&
          !argType.isBuf())
        err("Unsupported argument type '" + argType + "' for action '" + qname + "'", m.loc);

      if (m.params.length > 1)
        err("Action '" + qname + "' can't have more than one parameter", m.loc);
    }
  }

  private void checkMethodCtor(MethodDef m)
  {
    if (!m.isInstanceInit()) return;

    if (!m.synthetic && !curType.isFinal())
      err("Class '" + curType.name + "' must be final since it declares constructor", curType.loc);

    if (curType.isaComponent() && m.params.length > 0)
      err("Component class '" + curType.name + "' must have a no argument constructor", curType.loc);
  }

  private void checkMethodDepend(MethodDef m)
  {
    checkDepend(m.ret, m.loc);
    for (int i=0; i<m.params.length; ++i)
      checkDepend(m.params[i].type, m.params[i].loc);
  }

//////////////////////////////////////////////////////////////////////////
// Protection
//////////////////////////////////////////////////////////////////////////

  private void checkProtectionFlags(SlotDef s)
  {
    Location loc = s.loc;
    if (s.isPublic())
    {
      if (s.isProtected()) err("Invalid combination of 'public' and 'protected' modifiers", loc);
      if (s.isPrivate())   err("Invalid combination of 'public' and 'private' modifiers", loc);
      if (s.isInternal())  err("Invalid combination of 'public' and 'internal' modifiers", loc);
    }
    else if (s.isProtected())
    {
      if (s.isPrivate())   err("Invalid combination of 'protected' and 'private' modifiers", loc);
      if (s.isInternal())  err("Invalid combination of 'protected' and 'internal' modifiers", loc);
    }
    else if (s.isPrivate())
    {
      if (s.isInternal())  err("Invalid combination of 'private' and 'internal' modifiers", loc);
      if (s.isAbstract())  err("Invalid combination of 'private' and 'abstract' modifiers", loc);
      else if (s.isVirtual()) err("Invalid combination of 'private' and 'virtual' modifiers", loc);
    }

  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  public void enterStmt(Stmt stmt)
  {
    switch (stmt.id)
    {
      case Stmt.EXPR_STMT: checkExprStmt((Stmt.ExprStmt)stmt); break;
      case Stmt.LOCAL_DEF: checkLocalDef((Stmt.LocalDef)stmt); break;
      case Stmt.IF:        checkIf((Stmt.If)stmt); break;
      case Stmt.FOR:       checkFor((Stmt.For)stmt); break;
      case Stmt.FOREACH:   checkForeach((Stmt.Foreach)stmt); break;
      case Stmt.WHILE:     checkWhile((Stmt.While)stmt); break;
      case Stmt.DO_WHILE:  checkDoWhile((Stmt.DoWhile)stmt); break;
      case Stmt.BREAK:     checkBreak((Stmt.Break)stmt); break;
      case Stmt.CONTINUE:  checkContinue((Stmt.Continue)stmt); break;
      case Stmt.RETURN:    checkReturn((Stmt.Return)stmt); break;
      case Stmt.ASSERT:    checkAssert((Stmt.Assert)stmt); break;
      case Stmt.SWITCH:    checkSwitch((Stmt.Switch)stmt); break;
    }
  }

  private void checkExprStmt(Stmt.ExprStmt stmt)
  {
    if (!stmt.expr.isStmt())
      err("Not a statement", stmt.expr.loc);
  }

  private void checkLocalDef(Stmt.LocalDef stmt)
  {
    checkValidVar("Local variable", stmt.type, stmt.loc);

    checkDepend(stmt.type, stmt.loc);

    if (stmt.init != null)
      checkAssignable(stmt.type, stmt.init, stmt.loc);
  }

  private void checkIf(Stmt.If stmt)
  {
    if (!stmt.cond.type.isBool())
      err("If condition must be bool, not '" + stmt.cond.type + "'", stmt.cond.loc);
  }

  private void checkFor(Stmt.For stmt)
  {
    if (stmt.cond != null && !stmt.cond.type.isBool())
      err("For condition must be bool, not '" + stmt.cond.type + "'", stmt.cond.loc);
  }

  private void checkForeach(Stmt.Foreach stmt)
  {
    Type localType = stmt.local.type;
    Type arrayType = stmt.array.type;

    if (!arrayType.isArray())
    {
      err("Foreach requires array type, not '" + arrayType + "'", stmt.array.loc);
      return;
    }

    if (stmt.length == null)
    {
      if (arrayType.arrayLength() == null)
        err("Foreach without length requires bounded array type", stmt.array.loc);
    }
    else
    {
      if (!stmt.length.type.isInteger())
        err("Foreach length must be int, not type'" + stmt.length.type + "'", stmt.length.loc);
    }

    if (!localType.is(arrayType.arrayOf()))
      err("Invalid type '" + localType + "' to iterator '" + arrayType + "'", stmt.local.loc);
  }

  private void checkWhile(Stmt.While stmt)
  {
    if (!stmt.cond.type.isBool())
      err("While condition must be bool, not '" + stmt.cond.type + "'", stmt.cond.loc);
  }

  private void checkDoWhile(Stmt.DoWhile stmt)
  {
    if (!stmt.cond.type.isBool())
      err("Do/while condition must be bool, not '" + stmt.cond.type + "'", stmt.cond.loc);
  }

  private void checkBreak(Stmt.Break stmt)
  {                    
    // use of break inside a loop checked in CodeAsm
  }

  private void checkContinue(Stmt.Continue stmt)
  {
    // use of continue inside a loop checked in CodeAsm
  }

  private void checkReturn(Stmt.Return stmt)
  {
    Type ret = curMethod.ret;
    if (stmt.expr == null)
    {
      if (!ret.isVoid())
        err("Must return a value from non-void method", stmt.loc);
    }
    else
    {
      if (stmt.expr.isNullLiteral(ret))
      {                               
        // ok
      }
      else
      {
        if (!stmt.expr.type.is(ret))
          err("Cannot return '" + stmt.expr.type + "' as '" + ret + "'", stmt.expr.loc);
      }
    }
  }

  private void checkAssert(Stmt.Assert stmt)
  {
    if (!stmt.cond.type.isBool())
      err("Assert condition must be bool, not '" + stmt.cond.type + "'", stmt.cond.loc);
  }

  private void checkSwitch(Stmt.Switch stmt)
  {
    HashMap dups = new HashMap();

    if (!stmt.cond.type.isInteger())
      err("Switch condition must be int, not '" + stmt.cond.type + "'", stmt.cond.loc);

    for (int i=0; i<stmt.cases.length; ++i)
    {
      Stmt.Case c = stmt.cases[i];
      Integer literal = c.label.toIntLiteral();
      if (literal == null)
      {
        err("Case label not an int literal", c.label.loc);
      }
      else
      {
        if (dups.get(literal) != null)
          err("Duplicate case label", c.label.loc);
        dups.put(literal, literal);
      }
    }              
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public Expr expr(Expr expr)
  {
    switch (expr.id)
    {
      case Expr.TYPE_LITERAL:   checkTypeLiteral((Expr.Literal)expr); break;
      case Expr.NEGATE:         checkNumeric((Expr.Unary)expr); break;
      case Expr.COND_NOT:       checkBool((Expr.Unary)expr);    break;
      case Expr.BIT_NOT:        checkIntegerOrLong((Expr.Unary)expr);     break;
      case Expr.PRE_INCR:
      case Expr.PRE_DECR:
      case Expr.POST_INCR:
      case Expr.POST_DECR:      checkIncr((Expr.Unary)expr); break;
      case Expr.COND_OR:
      case Expr.COND_AND:       checkBools((Expr.Cond)expr); break;
      case Expr.EQ:
      case Expr.NOT_EQ:         checkEquality((Expr.Binary)expr); break;
      case Expr.GT:
      case Expr.GT_EQ:
      case Expr.LT:
      case Expr.LT_EQ:          checkCompare((Expr.Binary)expr); break;
      case Expr.BIT_OR:
      case Expr.BIT_XOR:
      case Expr.BIT_AND:        checkBoolOrBitwise((Expr.Binary)expr); break;
      case Expr.LSHIFT:
      case Expr.RSHIFT:         checkShift((Expr.Binary)expr); break;
      case Expr.MUL:
      case Expr.DIV:
      case Expr.MOD:
      case Expr.ADD:
      case Expr.SUB:            checkMath((Expr.Binary)expr); break;

      case Expr.PROP_ASSIGN:    
      case Expr.ASSIGN:         checkAssign((Expr.Binary)expr); 
                                checkPropAssign((Expr.Binary)expr); 
                                break;
      case Expr.ASSIGN_ADD:
      case Expr.ASSIGN_SUB:
      case Expr.ASSIGN_MUL:
      case Expr.ASSIGN_DIV:
      case Expr.ASSIGN_MOD:     checkMath((Expr.Binary)expr); 
                                checkPropAssign((Expr.Binary)expr); 
                                checkAssignable(((Expr.Binary)expr).lhs); 
                                break;
      case Expr.ASSIGN_BIT_OR:
      case Expr.ASSIGN_BIT_XOR:
      case Expr.ASSIGN_BIT_AND: checkBoolOrBitwise((Expr.Binary)expr); 
                                checkPropAssign((Expr.Binary)expr); 
                                checkAssignable(((Expr.Binary)expr).lhs); 
                                break;
      case Expr.ASSIGN_LSHIFT: 
      case Expr.ASSIGN_RSHIFT:  checkShift((Expr.Binary)expr); 
                                checkPropAssign((Expr.Binary)expr); 
                                checkAssignable(((Expr.Binary)expr).lhs); 
                                break;
      case Expr.ELVIS:          checkElvis((Expr.Binary)expr); break;
      case Expr.TERNARY:        checkTernary((Expr.Ternary)expr); break;
      case Expr.THIS:           checkThis((Expr.This)expr); break;
      case Expr.SUPER:          checkSuper((Expr.Super)expr); break;
      case Expr.FIELD:          checkField((Expr.Field)expr); break;
      case Expr.INDEX:          checkIndex((Expr.Index)expr); break;
      case Expr.CAST:           checkCast((Expr.Cast)expr); break;
      case Expr.CALL:           checkCall((Expr.Call)expr); break;
      case Expr.INTERPOLATION:  checkInterpolation((Expr.Interpolation)expr); break;
      case Expr.NEW:            checkHeap(expr); break;
      case Expr.DELETE:         checkHeap(expr); break;
    }
    return expr;
  }

  private void checkTypeLiteral(Expr.Literal expr)
  {
    Type type =expr.asType();
    if (!type.isReflective())
      err("Cannot use type literal for non-reflective type '" + expr.asType() + "'", expr.loc);
  }

  private void checkBool(Expr.Unary expr)
  {
    if (!expr.type.isBool())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.type + "'", expr.loc);
  }

  private void checkBools(Expr.Cond expr)
  {
    for (int i=0; i<expr.operands.size(); ++i)
    {
      Expr operand = (Expr)expr.operands.get(i);
      if (!operand.type.isBool())
        err("Cannot apply '" + expr.op + "' operator to '" + expr.type + "'", expr.loc);
    }
  }

  private void checkIntegerOrLong(Expr.Unary expr)
  {
    if (!expr.operand.type.isInteger() & !expr.operand.type.isLong())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.operand.type + "'", expr.loc);
  }

  private void checkNumeric(Expr.Unary expr)
  {
    if (!expr.operand.type.isNumeric())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.operand.type + "'", expr.loc);
  }

  private void checkIncr(Expr.Unary expr)
  {
    checkNumeric(expr);
    checkAssignable(expr.operand);

    // Discourage use of pre/postfix ops with properties
    if (expr.operand instanceof Expr.Field)
    {
      Expr.Field f = (Expr.Field)expr.operand;
      if (f.field.isProperty())
        warn("Should use ':=' assignment operator for properties", expr.loc);
    }
  }

  private void checkEquality(Expr.Binary expr)
  {                    
    Type lhs = expr.lhs.type;
    Type rhs = expr.rhs.type;      
    
    if (expr.lhs.isNullLiteral(rhs)) return;
    
    if (expr.rhs.isNullLiteral(lhs)) return;
    
    if (!lhs.is(rhs) && !rhs.is(lhs))
      err("Type mismatch: " + lhs + " " + expr.op + " " + rhs, expr.loc);
  }

  private void checkCompare(Expr.Binary expr)
  {
    checkMatch(expr);
    checkNumeric(expr);
  }

  private void checkBoolOrBitwise(Expr.Binary expr)
  {
    checkMatch(expr);
    if (!expr.lhs.type.isBool() || !expr.rhs.type.isBool())
      checkIntegerOrLong(expr);
  }

  private void checkShift(Expr.Binary expr)
  {                                     
    if (!expr.lhs.type.isInteger() && !expr.lhs.type.isLong())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.lhs.type + "' and '" + expr.rhs.type + "'", expr.loc);
    if (!expr.rhs.type.isInteger())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.lhs.type + "' and '" + expr.rhs.type + "'", expr.loc);
  }

  private void checkMath(Expr.Binary expr)
  {
    if (expr.lhs.type.isArray())
    {
      checkPointerArithmetic(expr);
    }
    else
    {
      checkMatch(expr);
      checkNumeric(expr);
    }
  }

  private void checkPointerArithmetic(Expr.Binary expr)
  {
    /*
    if ((expr.id != Expr.ADD && expr.id != Expr.SUB) || !expr.rhs.type.isInteger())
    */
    err("Cannot apply '" + expr.op + "' operator to '" + expr.lhs.type + "' and '" + expr.rhs.type + "'", expr.loc);
  } 

  private void checkAssign(Expr.Binary expr)
  {
    checkAssignable(expr.lhs);
    checkAssignable(expr.lhs.type, expr.rhs, expr.loc);  
  }

  private void checkElvis(Expr.Binary expr)
  {
    if (!expr.lhs.type.isRef())                                  
      err("Cannot apply '?:' operator to '" + expr.lhs.type + "'", expr.loc);
    checkAssignable(expr.lhs.type, expr.rhs, expr.rhs.loc);
  }


  // Checks that prop assignment := is used appropriately
  private void checkPropAssign(Expr.Binary expr)
  {
    if (expr.lhs.id==Expr.FIELD)
    {
      Expr.Field f = (Expr.Field)expr.lhs;

      // If not using PROP_ASSIGN for a property (except for initializer), compile warning
      if (f.field.isProperty())
      {
        if ( (expr.op.toBinaryExprId()!=Expr.PROP_ASSIGN) && !curMethod.isInstanceInit() )
          warn("Should use ':=' assignment operator for properties", expr.loc);
        return;
      }
      // If not property, drop through to error below
    }

    // If using PROP_ASSIGN for a non-property, compile error
    if (expr.op.toBinaryExprId()==Expr.PROP_ASSIGN)
      err("Cannot apply ':=' operator to non-property", expr.loc);
  }


  private void checkTernary(Expr.Ternary expr)
  {
    if (!expr.cond.type.isBool())
      err("Ternary cond must be bool, not '" + expr.cond.type + "'", expr.loc);
    if (expr.trueExpr.type != expr.falseExpr.type)
      err("Cannot apply ternary operator to '" + expr.trueExpr.type + "' and '" + expr.falseExpr.type + "'", expr.loc);
  }

  private void checkNumeric(Expr.Binary expr)
  {
    if (!expr.lhs.type.isNumeric())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.lhs.type + "'", expr.loc);
    if (!expr.rhs.type.isNumeric())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.rhs.type + "'", expr.loc);
  }

  private void checkIntegerOrLong(Expr.Binary expr)
  {         
    if (!expr.lhs.type.isInteger() && !expr.lhs.type.isLong())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.lhs.type + "'", expr.loc);
    if (!expr.rhs.type.isInteger() && !expr.rhs.type.isLong())
      err("Cannot apply '" + expr.op + "' operator to '" + expr.rhs.type + "'", expr.loc);
  }

  private void checkAssignable(Expr expr)
  {
    if (!expr.isAssignable())
    {                    
      Expr x = expr;
      if (x.id == Expr.INDEX) x = ((Expr.Index)x).target;
      if (x.id == Expr.FIELD)
      {
        Field f = ((Expr.Field)x).field;
        String kind = "Field";
        if (f.isInline()) kind = "Inline field";
        else if (f.isDefine()) kind = "Define field";
        else if (f.isConst()) kind = "Const field";
        err(kind + " '" + f.qname() + "' is not assignable", expr.loc);
      }
      else
      {
        err("Not assignable", expr.loc);
      }
    }         
    
    // check that nothing in chain is safe navigation
    Expr x = expr;
    while (x instanceof Expr.Name)
    {                   
      Expr.Name n = (Expr.Name)x;
      if (n.safeNav) err("Cannot use ?. on left hand side of assignment", n.loc);
      x = n.target;
    }
  }

  private void checkAssignable(Type lhs, Expr rhs, Location loc)
  {
    // assigning null to lhs
    if (rhs.isNullLiteral(lhs)) return;

    if (!rhs.type.is(lhs))  
      err("'" + rhs.type + "' is not assignable to '" + lhs + "'", loc);
  }

  private void checkMatch(Expr.Binary expr)
  {
    Type lhs = expr.lhs.type;
    Type rhs = expr.rhs.type;

    if (!rhs.is(lhs))
      err("Type mismatch: " + lhs + " " + expr.op + " " + rhs, expr.loc);
  }

  private void checkThis(Expr.This expr)
  {
    if (inStatic)
      err("Cannot access 'this' in static context", expr.loc);
  }

  private void checkSuper(Expr.Super expr)
  {
    if (inStatic)
      err("Cannot access 'super' in static context", expr.loc);
  }

  private void checkField(Expr.Field expr)
  {
    checkSlotAccess(expr.field, expr.loc, "field");
    checkSlotTarget(expr.field, expr.loc, "access", "field", expr.target);
  }

  private void checkIndex(Expr.Index expr)
  {
    if (!expr.index.type.isInteger())
      err("Index must be int, not '" + expr.index.type + "'", expr.loc);
  }

  private void checkCast(Expr.Cast cast)
  { 
    Type target = cast.target.type;                             
    Type to = cast.type;

    // number to number casts are always ok             
    if (target.isNumeric() && to.isNumeric()) return;
    
    // let to/from byte[] slide in Sys only
    if (compiler.ast.name.equals("sys") && 
        (target.isArray() && target.arrayOf().isByte()) ||
        (to.isArray() && to.arrayOf().isByte()))
      return;
        
    if (!to.is(target))
      err("Expr type " + target + " cannot be cast as " + to, cast.loc);
  }

  private void checkCall(Expr.Call call)
  {         
    checkIllegalCalls(call);    
    checkSlotAccess(call.method, call.loc, "method");
    checkSlotTarget(call.method, call.loc, "call", "method", call.target);
    checkArgs(call);
  }

  private void checkIllegalCalls(Expr.Call call)
  {                                            
    String qname = call.method.qname();
    
    if (qname.equals("sys::Sys.malloc")) 
      err("Must use new operator instead of Sys.malloc", call.loc);
      
    if (qname.equals("sys::Sys.free")) 
      err("Must use delete operator instead of Sys.free", call.loc);
  }

  private void checkInterpolation(Expr.Interpolation si)
  {                                                 
    Location loc = si.loc;
    if (!si.callOk)
      err("Cannot use str interpolation here", loc);      
    
    si.printMethods = new Method[si.parts.size()];
    for (int i=1; i<si.parts.size(); ++i)
    {
      Expr part = (Expr)si.parts.get(i);
      Method printMethod = TypeUtil.toPrintMethod(ns, part.type);
      if (printMethod == null)
        err("Cannot use str interpolation with '" + part.type + "'", part.loc);
      else
        si.printMethods[i] = printMethod;
    }
  }         

  private void checkHeap(Expr expr)
  {
    // only allow heap management in sys for right now
    if (!isSys) err("Heap operators only allowed in sys kit", expr.loc);
  }        
  
  private void checkSlotAccess(Slot slot, Location loc, String kind)
  {
    String qname = slot.qname();
    Type parent = slot.parent();

    if (slot.isPrivate())
    {
      if (!curType.equals(parent))
        err("Private " + kind + " '" + qname + "' not accessible", loc);
    }
    else if (slot.isProtected())
    {
      if (!curType.is(parent))               
        err("Protected " + kind + " '" + qname + "' not accessible", loc);
    }
    else if (slot.isInternal())
    {
      String kit1 = curType.kit.name;
      String kit2 = parent.kit().name();
      if (!kit1.equals(kit2))
        err("Internal " + kind + " '" + qname + "' not accessible", loc);
    }

    checkDepend(slot.parent(), loc);
  }

  private void checkSlotTarget(Slot slot, Location loc, String verb, String kind, Expr target)
  {
    String qname = slot.qname();
    if (slot.isStatic())
    {
      if (target == null) return;
      if (target.id != Expr.STATIC_TYPE)
        err("Cannot " + verb + " static " + kind + " '" + qname + "' on instance", loc);
    }
    else
    {
      if (target == null || target.id == Expr.STATIC_TYPE)
        err("Cannot " + verb + " instance " + kind + " '" + qname + "' in static context", loc);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Check Args
//////////////////////////////////////////////////////////////////////////

  private void checkArgs(Expr.Call call)
  {
    String name   = call.name;
    Expr[] args   = call.args;
    Type[] params = call.method.paramTypes();
    boolean isErr = false;

    // use friendly name for constructor
    if (call.method.isInstanceInit())
      name = call.method.parent().name();

    // if params don't match args - always an error
    if (params.length != args.length)
    {
      isErr = true;
    }

    // check each arg against each parameter
    else
    {
      for (int i=0; i<args.length && !isErr; ++i)
      {
        Expr arg = args[i];
        Type param = params[i];

        if (arg.isNullLiteral(param)) continue;
        
        isErr = !arg.type.is(param);        
      }
    }

    if (!isErr) return;

    StringBuffer s = new StringBuffer();
    s.append("Invalid args for ").append(name).append("(");
    for (int i=0; i<params.length; ++i)
    {
      if (i > 0) s.append(",");
      s.append(params[i]);
    }
    s.append("), not (");
    for (int i=0; i<args.length; ++i)
    {
      if (i > 0) s.append(",");
      s.append(args[i].type);
    }
    s.append(")");
    err(s.toString(), call.loc);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  boolean hasUnsizedInlineArray;
  boolean isSys;
}
