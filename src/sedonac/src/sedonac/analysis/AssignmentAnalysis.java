//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
package sedonac.analysis;

import sedonac.Compiler;
import sedonac.ast.Expr;
import sedonac.ast.Stmt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @user Matthew Giannini
 * @creation 2/24/12 8:36 AM
 */
public abstract class AssignmentAnalysis extends CfgStep
{
  public AssignmentAnalysis(Compiler c, ControlFlowGraph cfg)
  {
    super(c, cfg);
  }
  
  public final void run()
  {
    preAnalyze();
    analyzeAssignments();
    postAnalyze();
  }

  protected void preAnalyze()
  {
  }

  protected void postAnalyze()
  {
  }

  protected void analyzeAssignments()
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
          blockMap.put(b, curBlockSets = new DefinedSets());
        }
        computeAssignedIn(b);
        analyzeBlock(b);
        computeAssignedOut();
      }
    }
  }
  
  protected final DefinedSets getBlockSets(BasicBlock b)
  {
    return (DefinedSets)blockMap.get(b);
  }

  /**
   * The set of variables that are assigned when {@code b} is
   * entered is the intersection of all the {@code defOut} sets for all
   * of {@code b's} predecessors.
   */
  protected final void computeAssignedIn(BasicBlock b)
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
  protected final void computeAssignedOut()
  {
    curBlockSets.defOut = new HashSet(curBlockSets.defIn);
    if (!curBlockSets.defAt.isEmpty())
      curBlockSets.defOut.addAll(curBlockSets.defAt);
  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  protected void localDefStmt(Stmt.LocalDef localDef)
  {
    if (localDef.init != null)
      curBlockSets.defAt.add(localDef);
    super.localDefStmt(localDef);
  }

  protected void forEachStmt(Stmt.Foreach forEach)
  {
    // By definition, the iterating variable is always defined
    curBlockSets.defAt.add(forEach.local);
    super.forEachStmt(forEach);
  }

//////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////

  protected void assignExpr(Expr.Binary expr)
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

//////////////////////////////////////////////////////////////////////////
// Set operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Intersects s1 and s2 and stores the result in s1
   * @return true if s1 changed
   */
  protected final boolean intersect(Set s1, Set s2)
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
// Fields
//////////////////////////////////////////////////////////////////////////

  private boolean changed;
  private Map blockMap = new HashMap();
  protected DefinedSets curBlockSets;
}
