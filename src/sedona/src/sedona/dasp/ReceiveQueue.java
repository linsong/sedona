//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Jan 07  Brian Frank  Creation
//

package sedona.dasp;

/**
 * ReceiveQueue manages the incoming DaspSessionMessage queue.
 */
final class ReceiveQueue
{                       

////////////////////////////////////////////////////////////////
// Constructor.
////////////////////////////////////////////////////////////////  
  
  /**
   * Constructor. 
   */
  ReceiveQueue(int max)
  {
    this.max = max;
  }
  
////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////  

  /**
   * Get the if size is zero.
   */
  public final synchronized boolean isEmpty()
  {
    return size == 0;
  }

  /**
   * Get the number of frames currently in the queue.
   */
  public final synchronized int size()
  {
    return size;
  }

  /**
   * Get the peak size this queue has ever reached.
   */
  public final int peak()
  {
    return peak;
  }

  /**
   * Return current queue backlog which will be tolerated. 
   */
  public final int max()
  {
    return max;
  }

  /**
   * Clear the isAlive flag which will wake up blocked enqueuers.
   */
  public final synchronized void kill()
  {
    isAlive = false;
    notifyAll();
  }
  
  /**
   * Read off the oldest message from the queue.  If
   * no messages exist on the queue, then wait for
   * up to timeout milliseconds before returning
   *
   * @param timeout number of milliseconds to wait
   *    before timing out or -1 to wait forever.
   * @return oldest Queue element, or null
   *    if the queue is empty and the timeout
   *    expired.
   */
  public synchronized DaspMessage dequeue(long timeout)   
    throws InterruptedException
  {
    while (isAlive && size == 0 && timeout != 0)
    {
      if (timeout == -1) wait();
      else { wait(timeout); break; }
    }
    
    DaspMessage m = head;
    if (m == null) return null;
    head = m.next;
    if (head == null) tail = null;
    m.next = null;
    size--;
    notifyAll();
    return m;
  }

  /**
   * Add a message to the end of the Queue.
   * Throw FullException if queue is full.
   */
  public synchronized void enqueue(DaspMessage m)
    throws FullException
  {                     
    if (isAlive && size >= max()) 
      throw new FullException();
    
    if (m.next != null) throw new IllegalStateException();
    if (tail == null) { head = tail = m; }
    else { tail.next = m; tail = m; }
    size++;
    if (size > peak) peak = size;
    notifyAll();
  }
  
  /**
   * Remove all the enqueued entries.
   */
  public synchronized void clear()
  {
    size = 0;
    head = null;
    tail = null;
    notifyAll();
  }
  
  /**
   * To string.
   */
  public String toString()
  {
    return "MsgQueue size=" + size + " peak=" + peak + " max=" + max();
  }
  
////////////////////////////////////////////////////////////////
// FullException
////////////////////////////////////////////////////////////////

  static class FullException extends Exception
  {
  }

////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////

  private DaspMessage head;
  private DaspMessage tail;
  private int size;
  private int peak;
  private boolean isAlive = true;
  private int max;   
  
}
