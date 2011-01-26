//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import sedonac.ast.Block;
import sedonac.ast.Expr;
import sedonac.ast.MethodDef;
import sedonac.ast.Stmt;

/**
 * Builds a control flow graph (CFG) for a method definition.
 *
 * @author Matthew Giannini
 * @creation Nov 3, 2009
 *
 */
public final class ControlFlowGraph
{
  private ControlFlowGraph(MethodDef method)
  {
    this.method = method;
    blocks = new ArrayList(); 
    labels = new HashMap();
    exits = new ArrayList();
    loopStack = new Stack();
    
    entryBlock = newBlock();
  }
  
  public static ControlFlowGraph make(MethodDef method)
  {
    ControlFlowGraph cfg = new ControlFlowGraph(method);
    cfg.buildGraph();
    return cfg;
  }

//////////////////////////////////////////////////////////////////////////
// BuildGraph
//////////////////////////////////////////////////////////////////////////
  
  private void buildGraph()
  {
    if (method.code == null) { exitBlock = entryBlock; return; }
    
    this.exitBlock = block(entryBlock, method.code);
    while (!exits.isEmpty())
    {
      BasicBlock b = (BasicBlock)exits.remove(0);
      addJump(b, exitBlock);
    }
  }
  
  private BasicBlock block(BasicBlock block, Block codeBlock)
  {
    if (codeBlock == null) throw new IllegalStateException("empty codeBlock: " + method.qname); 
    BasicBlock cur = block;
    Stmt[] stmts = codeBlock.stmts();
    for (int i=0; i<stmts.length; ++i)
    {
      if (stmts[i].label != null)
        cur = label(cur, stmts[i].label);
      cur = stmt(cur, stmts[i]);
    }
    return cur;
  }
  
  private BasicBlock label(BasicBlock curBlock, final String labelName)
  {
    // Note: a labeled statement should always start a new basic block
    BasicBlock labelBlock = (BasicBlock)labels.get(labelName);
    if (labelBlock == null)
    {
      // Haven't seen a goto for this label yet. If the block has statements
      // we need to start a new block.  Otherwise, we just use the given
      // block as the start of the labeled block.
      labelBlock = curBlock;
      if (curBlock.hasStmts())
      {
        labelBlock = newBlock();
        addJump(curBlock, labelBlock);
      }
      labels.put(labelName, labelBlock);
    }
    else
    {
      if (labelBlock.hasStmts()) throw new IllegalStateException("label block should be empty");
      addJump(curBlock, labelBlock);
    }
    return labelBlock;
  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  private BasicBlock stmt(BasicBlock curBlock, Stmt stmt)
  {
    switch (stmt.id)
    {
      case Stmt.EXPR_STMT:
      case Stmt.LOCAL_DEF:
      case Stmt.ASSERT:
        // These statements do not cause a jump
        curBlock.addStmt(stmt);
        return curBlock;
        
      case Stmt.IF:       return ifStmt       (curBlock, (Stmt.If)stmt);
      case Stmt.FOR:      return forStmt      (curBlock, (Stmt.For)stmt);
      case Stmt.FOREACH:  return foreachStmt  (curBlock, (Stmt.Foreach)stmt);
      case Stmt.WHILE:    return whileStmt    (curBlock, (Stmt.While)stmt);
      case Stmt.DO_WHILE: return doWhileStmt  (curBlock, (Stmt.DoWhile)stmt);
      case Stmt.SWITCH:   return switchStmt   (curBlock, (Stmt.Switch)stmt);
      case Stmt.RETURN:   return returnStmt   (curBlock, (Stmt.Return)stmt);
      case Stmt.BREAK:    return breakStmt    (curBlock, stmt);
      case Stmt.CONTINUE: return continueStmt (curBlock, stmt);
      case Stmt.GOTO:     return gotoStmt     (curBlock, (Stmt.Goto)stmt);
      default:
        throw new IllegalStateException("Unhandled statement id: " + stmt.id);
    }
  }
  
  private BasicBlock returnStmt(BasicBlock curBlock, Stmt.Return ret)
  {
    // If this is the compiler generated return statement for 
    // a void method, we don't want to actually add it as a statement
    if (ret.expr == null && ret.loc.compareTo(method.loc) == 0)
    {
      if (curBlock.hasStmts())
      {
        exits.add(curBlock);
        return newBlock();
      }
      else
        return curBlock;
    }
    else
    {
      // This is an explicit "return" statement.
      curBlock.addStmt(ret);      
      exits.add(curBlock);
      return newBlock();
    }
  }
  
  private BasicBlock breakStmt(BasicBlock curBlock, Stmt stmt)
  {
    curBlock.addStmt(stmt);
    addJump(curBlock, ((LoopItem)loopStack.peek()).breakTarget);
    return newBlock();
  }
  
  private BasicBlock continueStmt(BasicBlock curBlock, Stmt stmt)
  {
    curBlock.addStmt(stmt);
    addJump(curBlock, ((LoopItem)loopStack.peek()).contTarget);
    return newBlock();
  }
  
  private BasicBlock gotoStmt(BasicBlock curBlock, Stmt.Goto stmt)
  {
    curBlock.addStmt(stmt);
    
    // If we haven't seen a statement with the destination label
    // yet, go ahead and put one into the label map.
    BasicBlock target = (BasicBlock)labels.get(stmt.destLabel);
    if (target == null)
    {
      target = newBlock();
      labels.put(stmt.destLabel, target);
    }
    addJump(curBlock, target);
    return newBlock();
  }
  
  private BasicBlock ifStmt(BasicBlock curBlock, Stmt.If stmt)
  {
    curBlock.addStmt(stmt);
    
    BasicBlock afterIf    = newBlock();
    BasicBlock startTrue  = newBlock();
    BasicBlock endTrue    = null;
    BasicBlock startFalse = null;
    BasicBlock endFalse   = null;
    
    // Note: We always process the trueBlock and falseBlock, even in the
    // presence of 'true' and 'false' literals because there might be
    // goto target labels in those blocks.
    
    // Handle true
    // Only jump to the true block if condition is not the 'false' literal
    if (Expr.FALSE_LITERAL != stmt.cond.id)
      addJump(curBlock, startTrue);
    endTrue = block(startTrue, stmt.trueBlock);
    addJump(endTrue, afterIf);
    
    // Handle false
    if (stmt.falseBlock == null)
    {
      // The only exit path should be through the trueBlock in this case
      //   if (true) {<trueBlock>}
      //   <afterIf>
      if (Expr.TRUE_LITERAL != stmt.cond.id)
        addJump(curBlock, afterIf);
    }
    else
    {
      startFalse = newBlock();
      // The only exit should be through the trueBlock in this case
      //   if (true) {<trueBlock>}
      //   else {<falseBlock>}
      //   <afterIf>
      if (Expr.TRUE_LITERAL != stmt.cond.id)
        addJump(curBlock, startFalse);
      endFalse = block(startFalse, stmt.falseBlock);
      addJump(endFalse, afterIf);
    }
    
    return afterIf;
  }
  
  private BasicBlock whileStmt(BasicBlock curBlock, Stmt.While whileStmt)
  {
    BasicBlock startWhile = null;
    BasicBlock afterWhile = newBlock();
    BasicBlock loopBody   = newBlock();
    BasicBlock lastBody   = null;

    startWhile = curBlock.hasStmts() ? newBlock() : curBlock;
    startWhile.addStmt(whileStmt);
    if (curBlock != startWhile)
      addJump(curBlock, startWhile);
    
    if (whileStmt.cond.id != Expr.FALSE_LITERAL)
      addJump(startWhile, loopBody);

    LoopItem loopItem = new LoopItem(afterWhile, startWhile);
    loopStack.push(loopItem);
    lastBody = block(loopBody, whileStmt.block);
    if (loopStack.pop() != loopItem) throw new IllegalStateException();
    addJump(lastBody, startWhile);  
    
    if (whileStmt.cond.id != Expr.TRUE_LITERAL)
      addJump(startWhile, afterWhile);
    return afterWhile;
  }
  
  private BasicBlock doWhileStmt(BasicBlock curBlock, Stmt.DoWhile doStmt)
  {
    BasicBlock startDo  = null;
    BasicBlock cond     = null;
    BasicBlock afterDo  = newBlock();
    BasicBlock lastBody = null;
    
    // Re-write do_while so that location is reported as location of
    // the condition
    Stmt.DoWhile rewriteDo = new Stmt.DoWhile(doStmt.cond.loc);
    rewriteDo.block = doStmt.block;
    rewriteDo.cond  = doStmt.cond;
    rewriteDo.label = doStmt.label;
    doStmt = rewriteDo;
    
    cond = startDo = curBlock.hasStmts() ? newBlock() : curBlock;
    if (curBlock != startDo)
      addJump(curBlock, startDo);
    
    if (!doStmt.block.isEmpty())
    {
      cond = newBlock();
      LoopItem loopItem = new LoopItem(afterDo, cond);
      loopStack.push(loopItem);
      lastBody = block(startDo, doStmt.block);
      if (loopStack.pop() != loopItem) throw new IllegalStateException();
      addJump(lastBody, cond);
    }

    cond.addStmt(doStmt);
    if (Expr.TRUE_LITERAL != doStmt.cond.id)
      addJump(cond, afterDo);
    if (Expr.FALSE_LITERAL != doStmt.cond.id)
      addJump(cond, startDo);
    
    return afterDo;
  }
  
  private BasicBlock switchStmt(BasicBlock curBlock, Stmt.Switch s)
  {
    BasicBlock afterSwitch = newBlock();
    BasicBlock startCase = null;
    BasicBlock endPrev = null;
    curBlock.addStmt(s);
    
    // CodeAsm will detect continue in a switch statement and report it,
    // so we just use afterSwitch as the continue location
    LoopItem loopItem = new LoopItem(afterSwitch, afterSwitch);
    loopStack.push(loopItem);
    for (int i=0; i<s.cases.length; ++i)
    {
      if (s.cases[i].block == null) continue;
      
      startCase = newBlock();
      addJump(curBlock, startCase);
      if (endPrev != null)
        addJump(endPrev, startCase);
      endPrev = block(startCase, s.cases[i].block);
    }
    if (s.defaultBlock == null)
    {
      addJump(curBlock, afterSwitch);
      if (endPrev != null)
        addJump(endPrev, afterSwitch);
    }
    else
    {
      startCase = newBlock();
      addJump(curBlock, startCase);
      if (endPrev != null)
        addJump(endPrev, startCase);
      endPrev = block(startCase, s.defaultBlock);
      addJump(endPrev, afterSwitch);
    }
    if (loopStack.pop() != loopItem) throw new IllegalStateException();
    return afterSwitch;
  }
  
  private BasicBlock forStmt(BasicBlock curBlock, Stmt.For forStmt)
  {
    BasicBlock cond     = null;
    BasicBlock update   = null;
    BasicBlock startFor = null;
    BasicBlock lastFor  = null;
    BasicBlock afterFor = newBlock();
    
    if (forStmt.init != null)
      curBlock.addStmt(forStmt.init);
    
    cond = curBlock.hasStmts() ? newBlock() : curBlock;
    if (curBlock != cond)
      addJump(curBlock, cond);
    
    cond.addStmt(forStmt);
//    if (forStmt.cond != null)
//      cond.addStmt(new Stmt.ExprStmt(forStmt.cond.loc, forStmt.cond));
//    else
//      cond.addStmt(new Stmt.ExprStmt(forStmt.loc, new Expr.Literal(forStmt.loc, Expr.TRUE_LITERAL, null, Boolean.TRUE)));
//    
    update = cond;
    if (forStmt.update != null)
    {
      update = newBlock();
      update.addStmt(new Stmt.ExprStmt(forStmt.update.loc, forStmt.update));
      addJump(update, cond);
    }
    
    if (!forStmt.block.isEmpty())
    {
      startFor = newBlock();
      LoopItem loopItem = new LoopItem(afterFor, update);
      loopStack.push(loopItem);
      lastFor = block(startFor, forStmt.block);
      addJump(lastFor, update);
      if (loopStack.pop() != loopItem) throw new IllegalStateException();
      
      if (forStmt.cond == null || Expr.FALSE_LITERAL != forStmt.cond.id)
        addJump(cond, startFor);
    }
    else
      addJump(cond, update);

    if (forStmt.cond != null && Expr.TRUE_LITERAL != forStmt.cond.id)
      addJump(cond, afterFor);

    return afterFor;
  }
  
  private BasicBlock foreachStmt(BasicBlock curBlock, Stmt.Foreach forEach)
  {
    BasicBlock startFor   = curBlock.hasStmts() ? newBlock() : curBlock;
    BasicBlock startBody  = newBlock();
    BasicBlock lastBody   = null; 
    BasicBlock afterFor   = newBlock();
    
    startFor.addStmt(forEach);
    if (curBlock != startFor)
      addJump(curBlock, startFor);
    addJump(startFor, startBody);
    
    LoopItem loopItem = new LoopItem(afterFor, startFor);
    loopStack.push(loopItem);
    lastBody = block(startBody, forEach.block);
    if (loopStack.pop() != loopItem) throw new IllegalStateException();
    
    addJump(lastBody, startFor);
    addJump(startFor, afterFor);
    return afterFor;
  }
  
//////////////////////////////////////////////////////////////////////////
// ControlFlowGraph
//////////////////////////////////////////////////////////////////////////

  public BasicBlock entry() { return entryBlock; }
  public BasicBlock exit() { return exitBlock; }
  
  /**
   * @return the number of basic blocks in the graph.
   */
  public int size() { return blocks.size(); }
  
  /**
   * @return an array of all the basic blocks in the CFG.
   */
  public BasicBlock[] asArray()
  {
    return (BasicBlock[])blocks.toArray(new BasicBlock[blocks.size()]);
  }
  
  /**
   * @return a HashSet containing all the basic blocks in the CFG.
   */
  public HashSet asSet()
  {
    return new HashSet(blocks);
  }
  
  /**
   * Creates a new BasicBlock, adds it to the set of blocks in the graph,
   * and returns it.
   * 
   * @return an empty BasicBlock that is now part of the CFG.
   */
  private BasicBlock newBlock()
  {
    BasicBlock b = new BasicBlock();
    blocks.add(b);
    return b;
  }
  
  private void addJump(BasicBlock from, BasicBlock to)
  {
    if (!from.hasStmts())
    { 
      // If "from" contains no statements, we can optimize the graph
      // by re-mapping all its jump sources to jump to "to" directly,
      // and then remove "from" from the graph.
      while (!from.backEdges.isEmpty())
      {
        BasicBlock toRemap = (BasicBlock)from.backEdges.get(0);
        toRemap.removeJump(from);
        toRemap.addJump(to);
      }
      if (labels.entrySet().contains(from)) throw new IllegalStateException(from.toString());
      blocks.remove(from);
    }
    else
    {
      from.addJump(to);
      if (!blocks.contains(from)) blocks.add(from);
    }
    
    if (!blocks.contains(to)) blocks.add(to);
  }

//////////////////////////////////////////////////////////////////////////
// LoopItem
//////////////////////////////////////////////////////////////////////////
  
  private static class LoopItem
  {
    public LoopItem(BasicBlock breakTarget, BasicBlock contTarget)
    {
      this.breakTarget  = breakTarget;
      this.contTarget   = contTarget;
    }
    public final BasicBlock breakTarget;
    public final BasicBlock contTarget;
  }

//////////////////////////////////////////////////////////////////////////
// Iterators
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Get a depth-first iterator for the CFG.
   */
  public Iterator depthFirstIterator()
  {
    return new DepthFirstIterator(this);
  }
  
  /**
   * Get a reverse-post-order iterator for the CFG.
   */
  public Iterator reversePostorderIterator()
  {
    return new ReversePostorderIterator(this);
  }
  
//////////////////////////////////////////////////////////////////////////
//Fields
//////////////////////////////////////////////////////////////////////////
  
  public final MethodDef method;
  private ArrayList blocks;
  private BasicBlock entryBlock;
  private BasicBlock exitBlock;
  
  private HashMap labels;
  private ArrayList exits;
  private Stack loopStack;

}
