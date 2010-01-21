//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.*;

/**
 * Iterates a CFG by visiting nodes in reverse-postorder.
 *
 * @author Matthew Giannini
 * @creation Nov 13, 2009
 *
 */
class ReversePostorderIterator
  implements Iterator
{
  public ReversePostorderIterator(ControlFlowGraph cfg)
  {
    this(cfg.entry());
  }
  
  public ReversePostorderIterator(BasicBlock block)
  {
    visited = new HashSet();
    queue   = new LinkedList();
    queue.addLast(block);
  }
  
  public boolean hasNext()
  {
    return !queue.isEmpty();
  }

  public Object next()
  {
    BasicBlock result = (BasicBlock)queue.removeFirst();
    visited.add(result);
    
    int size = result.jumpEdges.size();
    for (int i=0; i<size; ++i)
    {
      Object o = result.jumpEdges.get(i);
      if (!visited.contains(o) && !queue.contains(o))
        queue.addLast(o);
    }
    return result;
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }
  
//////////////////////////////////////////////////////////////////////////
//Fields
//////////////////////////////////////////////////////////////////////////

  protected HashSet visited;
  protected LinkedList queue;
}
