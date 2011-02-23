//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.*;

import sedonac.Compiler;
import sedonac.*;

/**
 * Search for unreachable (i.e. "dead") code.  Dead code is reported as a
 * warning.  If the method does not exit (due to detection of an infinite
 * loop), that is reported as an error.
 * 
 * @author Matthew Giannini
 * @creation Nov 5, 2009
 *
 */
public class DeadCodeAnalysis extends CompilerStep
{
  public DeadCodeAnalysis(Compiler c, ControlFlowGraph cfg)
  {
    super(c);
    this.cfg = cfg;
  }
  
  public void run()
  {
    // Begin by assuming all blocks are dead
    HashSet deadBlocks = cfg.asSet();
    HashSet liveBlocks = new HashSet();
    
    Iterator iter = cfg.depthFirstIterator();
    while (iter.hasNext())
    {
      Object o = iter.next();
      deadBlocks.remove(o);
      liveBlocks.add(o);
    }
    
    if (deadBlocks.isEmpty()) return;
    
    BasicBlock[] dead = (BasicBlock[])deadBlocks.toArray(new BasicBlock[deadBlocks.size()]);
    Arrays.sort(dead, bc);
    BasicBlock[] alive = (BasicBlock[])liveBlocks.toArray(new BasicBlock[liveBlocks.size()]);
    Arrays.sort(alive, bc);
    
    reportDeadRanges(dead, alive);
  }
  
  /**
   * This method iterates the dead blocks and reports ranges of contiguous
   * dead blocks instead of each one individually.  It works under the
   * premise that a set of dead blocks is contiguous if there is no alive
   * block in between them.
   * 
   * @param dead set of dead blocks that are ordered by location
   * @param alive set of alive blocks that are ordered by location
   */
  private void reportDeadRanges(BasicBlock[] dead, BasicBlock[] alive)
  {
    int start = 0;
    int end = 0;
    do
    {
      OUTER: for (end = start + 1; end < dead.length; ++end)
      {
        for (int a = 0; a < alive.length; ++a)
        {
          if (between(dead[start], alive[a], dead[end]))
            break OUTER;
        }
      }
      reportRange(dead[start], dead[end-1]);
      start = end;
    } while (start<dead.length);
  }
  
  private void reportRange(BasicBlock start, BasicBlock end)
  {
    // If method doesn't exit, we'll report this as an error
    if (start == cfg.exit())
      err("Method '" + cfg.method.qname() + "' never exits", cfg.method.loc); 
    else
    {
      Location startLoc = start.getFirstStmt().loc;
      Location endLoc = end.getLastStmt().loc;
      
      StringBuffer sb = new StringBuffer().append(startLoc);
      if (startLoc.compareTo(endLoc) != 0)
        sb.append(" - ").append(endLoc.line).append(':').append(endLoc.col);
      sb.append(": Dead Code");
      warn(sb.toString());
    }
  }
  
  /**
   * @return true if {@code (start < test < end)}. The comparison is based
   * on location.  If end is the exit block, we always return true so
   * that it is handled as an individual block in range reporting.
   */
  private boolean between(BasicBlock start, BasicBlock test, BasicBlock end)
  {
    // Force a method exit block to be handled separately.
    if (end == cfg.exit()) return true;

    if (bc.compare(start, test) < 0)
    {
      if (bc.compare(test, end) < 0)
        return true;
    }
    return false;
  }

////////////////////////////////////////////////////////////////
// BlockComparator
////////////////////////////////////////////////////////////////

  private class BlockComparator implements Comparator
  {
    /**
     * Compare two BasicBlocks based on the location of the first statement.
     */
    public int compare(Object o1, Object o2)
    {
      BasicBlock b1 = (BasicBlock)o1;
      BasicBlock b2 = (BasicBlock)o2;
      if (b1 == cfg.exit()) return 1;
      else if (b2 == cfg.exit()) return -1;
      else return b1.getFirstStmt().loc.compareTo(b2.getFirstStmt().loc);
    }
  }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  protected final ControlFlowGraph cfg;
  protected final BlockComparator bc = new BlockComparator();

}
