//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.*;

import sedonac.ast.Stmt;

/**
 * Basic blocks are the nodes of a Control Flow Graph (CFG).  A basic block
 * contains a sequence of statements that can be executed sequentially 
 * without requiring a jump.  The first statement is a jump target, the last
 * statement causes a jump.  
 *
 * @author Matthew Giannini
 * @creation Nov 3, 2009
 *
 */
public final class BasicBlock
{
  private static int gid = 0;
  public BasicBlock()
  {
    stmts = new ArrayList();
    backEdges = new ArrayList();
    jumpEdges = new ArrayList();
    id = gid++;
  }
  
  public void addJump(BasicBlock target)
  {
    this.jumpEdges.add(target);
    target.backEdges.add(this);
  }
  
  public void removeJump(BasicBlock target)
  {
    this.jumpEdges.remove(target);
    target.backEdges.remove(this);
  }
  
  public Stmt[] stmts()
  {
    return (Stmt[])stmts.toArray(new Stmt[stmts.size()]);
  }

  public void addStmt(Stmt stmt)
  {
    stmts.add(stmt);
  }
  
  /**
   * @return true if this basic block contains any statements.
   */
  public boolean hasStmts()
  {
    return !stmts.isEmpty();
  }
  
  public Stmt getFirstStmt() 
  {
    return stmts.isEmpty() ? null : (Stmt)stmts.get(0);
  }
  
  public Stmt getLastStmt()
  {
    return stmts.isEmpty() ? null : (Stmt)stmts.get(stmts.size()-1);
  }
  
//////////////////////////////////////////////////////////////////////////
// Graph Utilities
//////////////////////////////////////////////////////////////////////////

  public boolean hasPathTo(BasicBlock target)
  {
    DepthFirstIterator iter = new DepthFirstIterator(this);
    while (iter.hasNext())
      if (iter.next() == target)
        return true;
    return false;
  }
  
//////////////////////////////////////////////////////////////////////////
// toString
//////////////////////////////////////////////////////////////////////////

  public String toString()
  {
    StringBuffer sb = new StringBuffer("Block [").append(id).append("]\n");
    sb.append("jump: ").append(listEdges(jumpEdges)).append("\n");
    sb.append("back: ").append(listEdges(backEdges)).append("\n");
    if (!hasStmts()) return sb.append("<empty>\n").toString();
    
    for (int i=0; i<stmts.size(); ++i)
    {
      Stmt s = (Stmt)stmts.get(i);
      sb.append(i).append(": ");
      if (s.label != null) sb.append(s.label).append(": ");
      sb.append(toStmtString(s)).append('\n');
    }
    return sb.toString();
  }
  
  private String listEdges(ArrayList edges)
  {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<edges.size(); ++i)
    {
      if (i>0) sb.append(", ");
      sb.append(((BasicBlock)edges.get(i)).id);
    }
    return sb.toString();
  }
  
  public static String toStmtString(Stmt s)
  {
    StringBuffer sb = new StringBuffer();
    switch (s.id)
    {
      case Stmt.ASSERT:
        sb.append("assert(").append(((Stmt.Assert)s).cond).append(')');
        break;
      case Stmt.EXPR_STMT:
        sb.append(((Stmt.ExprStmt)s).expr);
        break;
      case Stmt.LOCAL_DEF:
        Stmt.LocalDef def = (Stmt.LocalDef)s;
        sb.append(def);
        if (def.init != null) sb.append(" = ").append(def.init);
        break;
      case Stmt.IF:
        sb.append("if (").append(((Stmt.If)s).cond).append(')');
        break;
      case Stmt.WHILE:
        sb.append("while (").append(((Stmt.While)s).cond).append(')');
        break;
      case Stmt.DO_WHILE:
        sb.append("do_while (").append(((Stmt.DoWhile)s).cond).append(')');
        break;
      case Stmt.SWITCH:
        sb.append("switch(").append(((Stmt.Switch)s).cond).append(')');
        break;
      case Stmt.FOREACH:
        Stmt.Foreach each = (Stmt.Foreach)s;
        sb.append("foreach(").append(each.local).append(": ").append(each.array);
        if (each.length != null)
          sb.append(", ").append(each.length);
        sb.append(')');
        break;
      case Stmt.GOTO:
        sb.append("goto ").append(((Stmt.Goto)s).destLabel);
        break;
      case Stmt.RETURN:
        Stmt.Return ret = (Stmt.Return)s;
        sb.append("return");
        if (ret.expr != null) sb.append(' ').append(ret.expr);
        break;
      case Stmt.BREAK:
        sb.append("break");
        break;
      case Stmt.CONTINUE:
        sb.append("continue");
        break;
      default:
        sb.append(s);
    }
    return sb.toString();
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  protected final int id;
  
  /**
   * Ordered list of statements in this basic block.
   */
  protected ArrayList stmts;
  
  protected ArrayList backEdges;
  protected ArrayList jumpEdges;
  
}
