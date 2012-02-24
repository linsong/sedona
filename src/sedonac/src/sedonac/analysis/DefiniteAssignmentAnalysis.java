//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import sedonac.Compiler;
import sedonac.ast.Expr;

/**
 * Checks for definite assignment of all local variables declared in
 * a method.
 *
 * @author Matthew Giannini
 * @creation Nov 13, 2009
 *
 */
public class DefiniteAssignmentAnalysis extends AssignmentAnalysis
{
  public DefiniteAssignmentAnalysis(Compiler compiler, ControlFlowGraph cfg)
  {
    super(compiler, cfg);
  }
  
  protected void postAnalyze()
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

//////////////////////////////////////////////////////////////////////////
// Definite Assignment
//////////////////////////////////////////////////////////////////////////

  protected void localExpr(Expr.Local local)
  {
    checkDefinitelyAssigned(local);
    super.localExpr(local);
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
  
  protected void condExpr(Expr.Cond cond)
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
  
  protected void ternaryExpr(Expr.Ternary tern)
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
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private HashMap errs = new HashMap();

}
