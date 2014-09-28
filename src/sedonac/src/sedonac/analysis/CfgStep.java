//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
package sedonac.analysis;

import sedonac.Compiler;
import sedonac.CompilerStep;
import sedonac.ast.Expr;
import sedonac.ast.Stmt;

import java.util.HashSet;
import java.util.Set;

/**
 * @user Matthew Giannini
 * @creation 2/23/12 4:52 PM
 */
public abstract class CfgStep extends CompilerStep
{
//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////
  public CfgStep(Compiler c, ControlFlowGraph cfg)
  {
    super(c);
    this.cfg = cfg;
  }

//////////////////////////////////////////////////////////////////////////
// Block
//////////////////////////////////////////////////////////////////////////

  protected void analyzeBlock(BasicBlock block)
  {
    Stmt[] statements = block.stmts();
    for (int i=0; i<statements.length; ++i)
    {
      analyzeStatement(statements[i]);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  protected void analyzeStatement(Stmt s)
  {
    switch (s.id)
    {
      case Stmt.LOCAL_DEF:  localDefStmt((Stmt.LocalDef)s);   break;
      case Stmt.EXPR_STMT:  doExpr(((Stmt.ExprStmt)s).expr);  break;
      case Stmt.ASSERT:     doExpr(((Stmt.Assert)s).cond);    break;
      case Stmt.IF:         doExpr(((Stmt.If)s).cond);        break;
      case Stmt.SWITCH:     doExpr(((Stmt.Switch)s).cond);    break;
      case Stmt.DO_WHILE:   doExpr(((Stmt.DoWhile)s).cond);   break;
      case Stmt.WHILE:      doExpr(((Stmt.While)s).cond);     break;
      case Stmt.RETURN:     doExpr(((Stmt.Return)s).expr);    break;
      case Stmt.FOR:        forStmt((Stmt.For)s);             break;
      case Stmt.FOREACH:    forEachStmt((Stmt.Foreach)s);     break;
      case Stmt.GOTO:
      case Stmt.BREAK:
      case Stmt.CONTINUE:
        break;
      default: throw new IllegalStateException("Unknown Statement: " + s.loc);
    }
  }
  
  protected void localDefStmt(Stmt.LocalDef localDef)
  {
    doExpr(localDef.init);
  }
  
  protected void forEachStmt(Stmt.Foreach forEach)
  {
    doExpr(forEach.array);
    doExpr(forEach.length);
  }
  
  /**
   * "for" statements are an unusual construct because they define 3
   * parts (init, cond, update) that happen at different times relative to
   * the loop body.
   * <ul>
   * <li>The CFG explicitly writes any {@code init} statement directly into
   * a separate basic block.  So we don't need to process that statement.
   * <li>The CFG explicitly writes the {@code update} statement directly into
   * the basic block that ends the loop.  So we don't need to handle that
   * statement either.
   * <li>We only need to handle the {@code cond} statement of the for loop
   * </ul>
   */
  private void forStmt(Stmt.For forStmt)
  {
    doExpr(forStmt.cond);
  }

//////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////

  protected void doExpr(Expr expr)
  {
    if (expr == null || expr.isLiteral())
      return;
    
    switch (expr.id)
    {
      case Expr.LOCAL:    localExpr((Expr.Local)expr); break;
      
      case Expr.PROP_SET: 
      case Expr.CALL:     callExpr((Expr.Call)expr); break;

      case Expr.ASSIGN:   assignExpr((Expr.Binary)expr); break;
      
      case Expr.INDEX:    indexExpr((Expr.Index)expr); break;
      case Expr.INTERPOLATION: interpolationExpr((Expr.Interpolation)expr); break;

      // Special boolean operators
      case Expr.COND_AND:
      case Expr.COND_OR:  condExpr((Expr.Cond)expr); break;
      case Expr.TERNARY:  ternaryExpr((Expr.Ternary)expr); break;
      
      // Unary
      case Expr.NEGATE:
      case Expr.COND_NOT:
      case Expr.BIT_NOT:
      case Expr.PRE_DECR:
      case Expr.PRE_INCR:
      case Expr.POST_DECR:
      case Expr.POST_INCR:  unaryExpr((Expr.Unary)expr); break;
      case Expr.CAST:       castExpr((Expr.Cast)expr); break;
      
      // Binary
      case Expr.ELVIS:
      case Expr.ADD:
      case Expr.DIV:
      case Expr.MOD:
      case Expr.MUL:
      case Expr.SUB:
      
      case Expr.BIT_AND:
      case Expr.BIT_OR:
      case Expr.BIT_XOR:
      case Expr.LSHIFT:
      case Expr.RSHIFT:
        
      case Expr.EQ:
      case Expr.NOT_EQ:
      case Expr.LT:
      case Expr.LT_EQ:
      case Expr.GT:
      case Expr.GT_EQ:
      
      case Expr.ASSIGN_ADD:
      case Expr.ASSIGN_BIT_AND:
      case Expr.ASSIGN_BIT_OR:
      case Expr.ASSIGN_BIT_XOR:
      case Expr.ASSIGN_DIV:
      case Expr.ASSIGN_LSHIFT:
      case Expr.ASSIGN_MOD:
      case Expr.ASSIGN_MUL:
      case Expr.ASSIGN_RSHIFT:
      case Expr.ASSIGN_SUB:
      case Expr.PROP_ASSIGN:
        binaryExpr((Expr.Binary)expr);
        break;
       
      // for completeness, even though only sys kit can use them
      case Expr.NEW:    doExpr(((Expr.New)expr).arrayLength); break;
      case Expr.DELETE: doExpr(((Expr.Delete)expr).target); break;

      // Ignore these
      case Expr.PARAM:
      case Expr.THIS:
      case Expr.SUPER:
      case Expr.FIELD:
      case Expr.INIT_ARRAY:
      case Expr.INIT_VIRT:
      case Expr.INIT_COMP:
      case Expr.STATIC_TYPE:
        break;
      
      default:
        throw new IllegalStateException("Unhandled expression: " + cfg.method.name + " " + expr.id + " " + expr + "\n" + expr.loc);
    }
  }
  
  protected void localExpr(Expr.Local expr)
  {
  }
  
  protected void callExpr(Expr.Call call)
  {
    for (int i=0; i<call.args.length; ++i)
      doExpr(call.args[i]);
    doExpr(call.target);
  }

  /**
   * Unique callback for assignment.
   * <p>
   *   May require special handling for the following kinds of <code>lhs</code>
   *   <li>Expr.LOCAL</li>
   *   <li>Expr.INDEX</li>
   *   <li>Expr.FIELD</li>
   *   <li>Expr.PARAM</li>
   * </p>
   */
  protected abstract void assignExpr(Expr.Binary assign);

  protected void indexExpr(Expr.Index index)
  {
    doExpr(index.index);
    doExpr(index.target);
  }
  
  protected void interpolationExpr(Expr.Interpolation interpolation)
  {
    for (int i=0; i<interpolation.parts.size(); ++i)
      doExpr((Expr)interpolation.parts.get(i));
  }

  /**
   * Does not attempt to do any short-circuit evaluation. Every operand
   * will be visited for the condition.
   */
  protected void condExpr(Expr.Cond cond)
  {
    for (int i=0; i<cond.operands.size(); ++i)
      doExpr((Expr)cond.operands.get(i));
  }

  /**
   * Always visits both the true and false expressions.
   */
  protected void ternaryExpr(Expr.Ternary tern)
  {
    doExpr(tern.cond);
    doExpr(tern.trueExpr);
    doExpr(tern.falseExpr);
  }
  
  protected void unaryExpr(Expr.Unary unary)
  {
    doExpr(unary.operand);
  }
  
  protected void castExpr(Expr.Cast cast)
  {
    doExpr(cast.target);
  }
  
  protected void binaryExpr(Expr.Binary binary)
  {
    doExpr(binary.lhs);
    doExpr(binary.rhs);
  }

//////////////////////////////////////////////////////////////////////////
// Utility Classes
//////////////////////////////////////////////////////////////////////////

  public static class DefinedSets
  {
    public DefinedSets()
    {
    }

    public DefinedSets(DefinedSets copy)
    {
      defIn.addAll(copy.defIn);
      defAt.addAll(copy.defAt);
      defOut.addAll(copy.defOut);
    }

    public Set defIn = new HashSet();
    public Set defAt = new HashSet();
    public Set defOut = new HashSet();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  protected final ControlFlowGraph cfg;
}
