//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.*;

/**
 * Iterates the basic blocks of a CFG in depth-first order
 *
 * @author Matthew Giannini
 * @creation Nov 5, 2009
 *
 */
class DepthFirstIterator
  implements Iterator
{
  
  public DepthFirstIterator(ControlFlowGraph cfg)
  {
    this(cfg.entry());
  }
  
  public DepthFirstIterator(BasicBlock block)
  {
    this.visited = new HashSet();
    this.stack = new Stack();
    stack.push(block);
  }

//////////////////////////////////////////////////////////////////////////
// Iterator
//////////////////////////////////////////////////////////////////////////

  public boolean hasNext()
  {
    return !stack.isEmpty();
  }

  public Object next()
  {
    BasicBlock result = (BasicBlock)stack.pop();
    visited.add(result);
    int size = result.jumpEdges.size();
    for (int i=0; i<size; ++i)
    {
      Object o = result.jumpEdges.get(i);
      if (!visited.contains(o))
        stack.push(o);
    }
    return result;
  }

  public void remove()
  { 
    throw new UnsupportedOperationException();
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  protected HashSet visited;
  protected Stack stack;
}
