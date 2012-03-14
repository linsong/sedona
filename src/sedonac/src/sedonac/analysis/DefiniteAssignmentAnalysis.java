//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import sedonac.Compiler;
import sedonac.CompilerStep;
import sedonac.ast.Expr;
import sedonac.ast.Stmt;

/**
 * Checks for definite assignment of all local variables declared in
 * a method.
 *
 * @author Matthew Giannini
 * @creation Nov 13, 2009
 *
 */
public class DefiniteAssignmentAnalysis extends CompilerStep
{
  
  public DefiniteAssignmentAnalysis(Compiler compiler, ControlFlowGraph cfg)
  {
    super(compiler);
    this.cfg = cfg;
  }

  /**
   * Iterates over the CFG in reverse-postorder to determine what variables
   * are definitely assigned in, at, and after each block.  It is necessary
   * to iterate the CFG multiple times because of goto statements. We stop
   * when no more changes are detected. 
   */
  public void run()
  {
    int iterations = 0;
    changed = true;
    while (changed)
    {
      ++iterations;
      changed = false;
      Iterator iter = cfg.reversePostorderIterator();
      while (iter.hasNext())
      {
        BasicBlock b = (BasicBlock)iter.next();
        if ((this.curBlockSets = getBlockSets(b)) == null)
        {
          changed |= true;
          map.put(b, curBlockSets = new DefinedSets());
        }
        computeDefinedIn(b);
        analyzeBlock(b);
        computeDefinedOut();
      }
    }
    reportErrors();
  }
  
  /**
   * Report errors sorted by location
   */
  private void reportErrors()
  {
    if (errs.size() == 0) return;
    Expr.Local[] e = (Expr.Local[])errs.values().toArray(new Expr.Local[errs.size()]);
    Arrays.sort(e, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return ((Expr.Local)o1).loc.compareTo(((Expr.Local)o2).loc);
      }
    });
    for (int i=0; i<e.length; ++i)
      err("Local variable '" + e[i].name + "' may not have been initalized", e[i].loc);
  }
  
  private DefinedSets getBlockSets(BasicBlock b)
  {
    return (DefinedSets)map.get(b);
  }
  
  /**
   * The set of variables that are definitely assigned when {@code b} is 
   * entered is the intersection of all the {@code defOut} sets for all
   * of {@code b's} predecessors.
   */
  private void computeDefinedIn(BasicBlock b)
  {
    // no defIn for entry block
    if (b == cfg.entry()) return;
    
    final int predSize = b.backEdges.size();
    DefinedSets predSets = null;
    HashSet solution = null;
    for (int i=0; i<predSize; ++i)
    {
      BasicBlock predBlock = (BasicBlock)b.backEdges.get(i);
      
      // Skip if block jumps back to itself
      if (predBlock == b) continue;
      
      // Skip if we haven't seen this predecessor yet
      if ((predSets = getBlockSets(predBlock)) == null) continue;
      
      if (solution == null)
        solution = new HashSet(predSets.defOut);
      else
        intersect(solution, predSets.defOut);
      
      // short-circuit if intersection becomes empty set
      if (solution.isEmpty()) break;
    }
    
    // Now, determine if defIn changed.
    if (solution.size() != curBlockSets.defIn.size())
    {
      changed |= true;
      curBlockSets.defIn = solution;
    }
    else
    {
      Iterator iter = curBlockSets.defIn.iterator();
      while (iter.hasNext())
      {
        if (!solution.contains(iter.next()))
        {
          changed |= true;
          curBlockSets.defIn = solution;
          break;
        }
      }
    }
  }
  
  /**
   * defOut = union(defIn, defAt)
   * <p>
   * We do not need to update the iteration {@code changed} flag here
   * because {@code defOut} will only change if {@code defIn} or
   * {@code defAt} changed.
   */
  private void computeDefinedOut()
  {
    curBlockSets.defOut = new HashSet(curBlockSets.defIn);
    if (!curBlockSets.defAt.isEmpty())
      curBlockSets.defOut.addAll(curBlockSets.defAt);
  }

//////////////////////////////////////////////////////////////////////////
// Analyze Block
//////////////////////////////////////////////////////////////////////////

  private void analyzeBlock(BasicBlock b)
  {
    Stmt[] stmts = b.stmts();
    for (int i=0; i<stmts.length; ++i)
    {
      Stmt s = stmts[i];
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
  }
  
  private void localDefStmt(Stmt.LocalDef localDef)
  {
    if (localDef.init != null)
    {
      curBlockSets.defAt.add(localDef);
      doExpr(localDef.init);
    }
  }

  private void forEachStmt(Stmt.Foreach each)
  {
    // By definition, the iterating variable is always defined
    curBlockSets.defAt.add(each.local);
    doExpr(each.array);
    doExpr(each.length);
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
  
  private void doExpr(Expr expr)
  {
    if (expr == null) return;
    switch(expr.id)
    {
      case Expr.LOCAL:          checkDefinitelyAssigned((Expr.Local)expr); break;
      case Expr.PROP_SET:
      case Expr.CALL:           callExpr((Expr.Call)expr); break;
      case Expr.ASSIGN:         assignExpr((Expr.Binary)expr); break;
      case Expr.INDEX:          indexExpr((Expr.Index)expr); break;
      case Expr.INTERPOLATION:  interpolateExpr((Expr.Interpolation)expr); break;
        
      // Special boolean operators
      case Expr.COND_AND: 
      case Expr.COND_OR:        condExpr((Expr.Cond)expr); break;
      case Expr.TERNARY:        ternaryExpr((Expr.Ternary)expr); break;
        
      // Unary
      case Expr.NEGATE:
      case Expr.COND_NOT:
      case Expr.BIT_NOT:
      case Expr.PRE_DECR:
      case Expr.PRE_INCR:
      case Expr.POST_DECR:
      case Expr.POST_INCR:      doExpr(((Expr.Unary)expr).operand); break;
      case Expr.CAST:           doExpr(((Expr.Cast)expr).target); break;
        
      // lhs of Elvis operator is the only expression guaranteed to evaluate
      case Expr.ELVIS:          doExpr(((Expr.Binary)expr).lhs); break;
        
      // Binary Operators not requiring special handling
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
        Expr.Binary binary = (Expr.Binary)expr;
        doExpr(binary.lhs);
        doExpr(binary.rhs);
        break;
        
      // For completeness, even though only sys kit can use them
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
        if (!expr.isLiteral())
          System.out.println("### Unhandled Expr: " + cfg.method.name + " "+ expr.id + " " + expr +  "\n"+ expr.loc);
    }
  }
  
  private void checkDefinitelyAssigned(Expr.Local local)
  {
    if (!curBlockSets.defIn.contains(local.def) && 
        !curBlockSets.defAt.contains(local.def))
    {
      Expr.Local prev = (Expr.Local)errs.get(local.def);
      if (prev == null || local.loc.compareTo(prev.loc) < 0)
        errs.put(local.def, local);
    }
  }
  
  private void assignExpr(Expr.Binary expr)
  {
    switch (expr.lhs.id)
    {
      case Expr.LOCAL:
        // var = <expr>
        Expr.Local local = (Expr.Local)expr.lhs;
        doExpr(expr.rhs);
        curBlockSets.defAt.add(local.def);
        break;
        
      case Expr.INDEX:
        // array[<expr>] = <expr>
        doExpr(expr.lhs);
        doExpr(expr.rhs);
        break;
        
      case Expr.FIELD:
      case Expr.PARAM:
        doExpr(expr.rhs);
        break;
        
      default:
        throw new IllegalStateException("Unhandled assignment: " + expr.lhs.id + " " + expr + "\n" + expr.loc);
    }
  }
  
  private void callExpr(Expr.Call call)
  {
    for (int i=0; i<call.args.length; ++i)
      doExpr(call.args[i]);
    doExpr(call.target);
  }
  
  private void indexExpr(Expr.Index index)
  {
    doExpr(index.index);
    doExpr(index.target);
  }
  
  private void condExpr(Expr.Cond cond)
  {
    for (int i=0; i<cond.operands.size(); ++i)
    {
      Expr cur = (Expr)cond.operands.get(i);
      doExpr(cur);
      if ((cond.id == Expr.COND_AND) && (Expr.TRUE_LITERAL != cur.id))
        break;
      if ((cond.id == Expr.COND_OR) && (Expr.FALSE_LITERAL != cur.id))
        break;
    }
  }
  
  private void ternaryExpr(Expr.Ternary tern)
  {
    doExpr(tern.cond);
    if (Expr.TRUE_LITERAL == tern.cond.id)
      doExpr(tern.trueExpr);
    else if (Expr.FALSE_LITERAL == tern.cond.id)
      doExpr(tern.falseExpr);
    else
    {
      // A variable can only be definitely assigned after a ternary condition
      // if it is definitely assigned when true and when false.
      DefinedSets afterCond = new DefinedSets(curBlockSets);
      doExpr(tern.trueExpr);

      // save state when true, and restore defAt state back to what it 
      // was after the condition.
      DefinedSets afterTrue = new DefinedSets(curBlockSets);
      intersect(curBlockSets.defAt, afterCond.defAt);

      // Now determine state when false
      doExpr(tern.falseExpr);
      
      // curBlockSets.defAt now contains DA state when false.  We intersect
      // it with the state when true
      intersect(curBlockSets.defAt, afterTrue.defAt);
    }
  }
  
  private void interpolateExpr(Expr.Interpolation inter)
  {
    for (int i=0; i<inter.parts.size(); ++i)
    {
      Expr part = (Expr)inter.parts.get(i);
      doExpr(part);
    }
  }
  
//////////////////////////////////////////////////////////////////////////
// Set operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Intersects s1 and s2 and stores the result in s1
   * @return true if s1 changed
   */
  private boolean intersect(HashSet s1, HashSet s2)
  {
    if (s1.isEmpty()) 
      return false;
    else if (s2 == null || s2.isEmpty())
    {
      s1.clear();
      return true;
    }
    else
    {
      boolean s1Changed = false;
      Object[] items = s1.toArray();
      for (int i=0; i<items.length; ++i)
      {
        if (!s2.contains(items[i]))
        {
          s1Changed |= true;
          s1.remove(items[i]);
        }
      }
      return s1Changed;
    }
  }
  
//////////////////////////////////////////////////////////////////////////
// Inner Classes
//////////////////////////////////////////////////////////////////////////

  private static class DefinedSets
  {
    public DefinedSets(){}
    
    public DefinedSets(DefinedSets copy)
    {
      defIn.addAll(copy.defIn);
      defAt.addAll(copy.defAt);
      defOut.addAll(copy.defOut);
    }

// DEBUG
//    public String toString()
//    {
//      StringBuffer sb = new StringBuffer();
//      sb.append("defIn: "); toString(defIn, sb);
//      sb.append("\ndefAt: "); toString(defAt, sb);
//      sb.append("\ndefOut: "); toString(defOut, sb);
//      return sb.toString();
//    }
//    
//    private void toString(Set s, StringBuffer sb)
//    {
//      Iterator iter = s.iterator();
//      sb.append("{");
//      while (iter.hasNext())
//        sb.append(iter.next()).append(", ");
//      sb.append("}");
//    }
    
    public HashSet defIn    = new HashSet();
    public HashSet defAt    = new HashSet();
    public HashSet defOut   = new HashSet();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private ControlFlowGraph cfg;
  private HashMap map = new HashMap();
  private HashMap errs = new HashMap();
  private boolean changed;
  private DefinedSets curBlockSets;
  
}
