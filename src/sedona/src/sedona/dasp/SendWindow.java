//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Mar 08  Brian Frank  Creation
//

package sedona.dasp;

/**
 * SendWindow manages the outgoing datagram queue.
 */
final class SendWindow
{                   

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////  
  
  /**
   * Constructor
   */
  SendWindow(DaspSession session)
  {
    this.session  = session;
    this.socket   = session.socket;
    this.seqNum   = socket.rand.nextInt();             
    this.ackTimes = new int[20];                
    for (int i=0; i<ackTimes.length; ++i)
      ackTimes[i] = -1;                     
  }
  
////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////  

  /**
   * Get the number of unacked datagrams in the window.
   */
  final synchronized int size()
  {
    return size;
  }

  /**
   * Clear the isAlive flag which will wake up a blocked enqueuer.
   */
  final synchronized void kill()
  {
    isAlive = false;
    notifyAll();
  }

  /**
   * Allocate the next sequence number.
   */
  private final synchronized int nextSeqNum()
  {
    return seqNum++ & 0xffff;
  }
  
  /**
   * Get the current sequence number for test/debug.
   */
  final int curSeqNum() { return seqNum & 0xffff; }
  
  /**
   * Add a datagram to the end of the queue - block if full.
   */
  void send(byte[] payload)
    throws InterruptedException
  {                     
    Packet p = new Packet();
    p.enqueuedTime = DaspSession.ticks();
    p.seqNum  = nextSeqNum();
    p.payload = payload;         

    // will block until send window has room
    enqueue(p);                             
    
    // fire away
    p.sentTime = DaspSession.ticks();
    session.send(toMsg(p));
  }
  
  /**
   * Convert a packet item in our linked list to a message, we
   * always do this on-the-fly to get the latest ack headers.
   */
  private DaspMsg toMsg(Packet packet)
  { 
    // construct datagram message                  
    DaspMsg msg = new DaspMsg();
    msg.msgType   = DaspConst.DATAGRAM;
    msg.sessionId = session.remoteId;
    msg.seqNum    = packet.seqNum;
    msg.payload   = packet.payload;  

    // let receive window add ack headers if room
    session.receiveWindow.setAckHeaders(msg);  
        
    return msg;
  }

  /**
   * Add a datagram to the end of the queue - block if full.
   */
  private synchronized void enqueue(Packet p)
    throws InterruptedException
  {                     
    while(isAlive && full()) 
    {
      try { blocked = Thread.currentThread().getName(); } catch(Exception e) {}
      wait();
    }
    blocked = null;
    
    if (p.next != null) throw new IllegalStateException();
    if (tail == null) { head = tail = p; }
    else { tail.next = p; tail = p; }
    size++;
    notifyAll();
  }                     
  
  /**
   * Return if our sending window is filled up.
   */
  private boolean full()
  {
    return size >= sendSize;
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
    StringBuffer s =new StringBuffer();
    s.append("SendWindow size=").append(size).append(" sendSize=").append(sendSize);
    Packet p = head;
    while (p != null)
    {            
      s.append(" ").append(p.seqNum);
      if (p.acked) s.append("a");
      p = p.next;
    }
    if (blocked != null) s.append(" blocked=").append(blocked);
    return s.toString();
  }

  /**
   * Check if we've got some acks which will let us 
   * slide our sending window. 
   */
  synchronized void checkAckHeaders(DaspMsg msg)
  {
    int ackNum = msg.ack;
    if (ackNum < 0 || head == null) return;              
    
    // create the new "unacked window" - anything outside
    // of start and end inclusive is now considered ack'ed
    int unackStart = (ackNum + 1) & 0xffff;
    int unackEnd   = (unackStart + 10000) & 0xffff;    

    // check each packet for ack, and slide head as appriopiate
    Packet p = head;                         
    long now = DaspSession.ticks();
    while (p != null)
    {                    
      if (!p.acked && isAcked(unackStart, unackEnd, msg, p.seqNum))
      {
        p.acked = true;
        ackTimes[ackTimesPos] = (int)(now - p.enqueuedTime);
        ackTimesPos = (ackTimesPos + 1) % ackTimes.length;
      }               
      if (p == head && p.acked)
      {               
        head = p.next; 
        size--;
      }         
      p = p.next;       
    } 
    if (head == null) tail = null;               
    notifyAll();
  }               
  
  /**
   * Return if the specified seqNum should be considered
   * acked.  The unackStart and unackEnd define the inclusive
   * range of what the remote endpoint has not acked (which
   * means anything outside of that range is immediately
   * considered acked).  We also check our ackMore mask for
   * unordered acks.
   */
  boolean isAcked(int unackStart, int unackEnd, DaspMsg msg, int seqNum)
  {                     
    // if outside the unacked range, then definitely acked
    if (unackStart <= unackEnd)
    { 
      if (seqNum < unackStart || seqNum > unackEnd)
        return true;
    }        
    else
    {
      if (seqNum < unackStart && seqNum > unackEnd)
        return true;
    }   
    
    // if we have an ackMore header, then check for unordered acks
    byte[] ackMore = msg.ackMore;
    if (ackMore != null)
    {                                          
      int diff = (seqNum - msg.ack) & 0xffff;             
      int index = ackMore.length - (diff>>3) - 1;
      if (0 <= index && index < ackMore.length)
      {
        int ackMoreByte = ackMore[index] & 0xff;
        int ackMoreMask = 1 << (diff & 0x7);
        if ((ackMoreMask & ackMoreByte) != 0) return true;
      }     
    } 
    
    return false;
  }

  /**
   * Anything that hasn't been acked which was sent a while 
   * ago needs to get resent.  Return the longest duration we've
   * had an unacked packet enqueued.
   */
  synchronized long sendRetries()
  {                      
    long now = DaspSession.ticks();
    long oldest = 0;
    Packet p = head;
    while (p != null)
    {
      if (!p.acked)
      {
        // MG - do not implement maxSend timeouts right now. The default
        // behavior of the specification is too aggressive.
//        if (p.sendAttempts >= maxSend)
//        {
//          // packet was never ack'd after maxSend attempts, so close the session.
//          session.close(DaspConst.TIMEOUT, "maxSend limit exceeded for seqNum="+Integer.toHexString(p.seqNum));
//          return oldest;
//        }
        
        if (now - p.sentTime >= sendRetry)
        {
          p.sentTime = now;        
          ++p.sendAttempts;
          ++session.numRetries;
          ++session.iface.numRetries;
          session.send(toMsg(p));  
        }
        
        oldest = Math.max(oldest, now-p.enqueuedTime);
      }
      
      p = p.next;
    }    
    return oldest;
  }
      
////////////////////////////////////////////////////////////////
// Packet
////////////////////////////////////////////////////////////////

  static class Packet
  {                  
    int seqNum;
    byte[] payload;
    boolean acked;  
    long enqueuedTime;
    long sentTime;
    int sendAttempts = 1; // only incremented during retry
    Packet next;
  }

////////////////////////////////////////////////////////////////
// Test
////////////////////////////////////////////////////////////////
                  
  public static void main(String[] args)        
    throws Exception
  {                          
    long t1 = System.currentTimeMillis();
    DaspSocket sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);                   
    java.util.Hashtable options = new java.util.Hashtable();
    options.put("dasp.test", new DaspTestHooks()
      {
        public boolean send(int msgType, int num, byte[] msg) { return false; }
      }
    );
    DaspSession s = new DaspSession(sock.interfaces[0], 0, null, 0, true, options);
    SendWindow x = new SendWindow(s);
    x.test();
    long t2 = System.currentTimeMillis();
    System.out.println("  SendWindow Success: " + verifies + " verifies [" + (t2-t1) + "ms]");
    sock.close();
  }                            
  
  private void test() 
    throws Exception
  {                          
    seqNum = 0;     
    sendSize = 4;         
    byte[] b = new byte[0];
    
    verify("");  
    send(b); verify("0");  
    send(b); verify("0 1");  
    send(b); verify("0 1 2");  
    send(b); verify("0 1 2 3 full");  
    
    ack(0);  verify("1 2 3");  
    send(b); verify("1 2 3 4 full");  
    ack(1);  verify("2 3 4");  
    ack(2);  verify("3 4");  
    ack(2);  verify("3 4");  
    ack(0);  verify("3 4");  
    ack(65000);  verify("3 4");  
    ack(3);  verify("4");  
    ack(4);  verify("");   
    
    send(b); verify("5");  
    send(b); verify("5 6");  
    send(b); verify("5 6 7");  
    send(b); verify("5 6 7 8 full");  
    ack(7);  verify("8");  
    send(b); verify("8 9");  
    ack(9);  verify("");  
    
    seqNum = 65534; send(b); send(b); send(b); send(b); 
    verify("65534 65535 0 1 full");  
    ack(65534); verify("65535 0 1");  
    ack(65533); verify("65535 0 1");  
    ack(65534); verify("65535 0 1");  
    ack(0); ack(1); verify("");  

    seqNum = 65534; send(b); send(b); send(b); send(b); 
    verify("65534 65535 0 1 full");  
    ack(65535); verify("0 1");  
    ack(1); verify("");  

    seqNum = 65534; send(b); send(b); send(b); send(b); 
    verify("65534 65535 0 1 full");  
    ack(0); verify("1");  
    ack(1); verify("");  

    seqNum = 65534; send(b); send(b); send(b); send(b); 
    verify("65534 65535 0 1 full");  
    ack(1); verify("");  
    
    seqNum = 65535; send(b); send(b); send(b); send(b);
    verify("65535 0 1 2 full");  
    ack(65535); verify("0 1 2");  
    ack(2); verify("");  
    
    seqNum = 65535; send(b); send(b); send(b); send(b);
    verify("65535 0 1 2 full");  
    ack(1); verify("2");  
    ack(2); verify("");  
    
    seqNum = 10; send(b); send(b); send(b); send(b); 
    verify("10 11 12 13 full");
    ack(9, 11); verify("10 11a 12 13 full");
    ack(9, 11, 13); verify("10 11a 12 13a full");
    ack(10); verify("12 13a");
    ack(12); verify("");
    
    seqNum = 65534; send(b); send(b); send(b); send(b); 
    verify("65534 65535 0 1 full");  
    ack(65533, 65535); verify("65534 65535a 0 1 full");  
    ack(65534); verify("0 1");     
    ack(1); verify("");     
    
    seqNum = 65533; send(b); send(b); send(b); send(b); 
    verify("65533 65534 65535 0 full");  
    ack(65532, 0); verify("65533 65534 65535 0a full");  
    ack(65532, 65535, 0); verify("65533 65534 65535a 0a full");  
    ack(65534); verify("");      
    
    seqNum = 65535; send(b); send(b); send(b); 
    verify("65535 0 1");  
    ack(65533, 0); verify("65535 0a 1");  
    send(b); verify("65535 0a 1 2 full");  
    ack(65535); verify("1 2");  
    ack(2); verify("");  
    
    seqNum = 100;     
    sendSize = 32;     
    for (int i=0; i<18; ++i) send(b);     
    verify("100 101 102 103 104 105 106 107 108 109 110 111 112 113 114 115 116 117");
    ack(99, 107, 117);  
    verify("100 101 102 103 104 105 106 107a 108 109 110 111 112 113 114 115 116 117a");
    ack(99, 101, 102, 111, 117);  
    verify("100 101a 102a 103 104 105 106 107a 108 109 110 111a 112 113 114 115 116 117a");
    ack(104);  
    verify("105 106 107a 108 109 110 111a 112 113 114 115 116 117a");
    ack(106);  
    verify("108 109 110 111a 112 113 114 115 116 117a");
  }        
  
  private void ack(int a) { ack(new int[] { a }); }
  private void ack(int a, int b) { ack(new int[] { a, b }); }
  private void ack(int a, int b, int c) { ack(new int[] { a, b, c }); }
  private void ack(int a, int b, int c, int d) { ack(new int[] { a, b, c, d }); }
  private void ack(int a, int b, int c, int d, int e) { ack(new int[] { a, b, c, d, e }); }
  
  private void ack(int[] nums) 
  {                  
    DaspMsg msg = new DaspMsg();    
    msg.ack = nums[0];
    if (nums.length > 1)
    {
      int mask = 0x01;
      for (int i=0; i<nums.length; ++i)
      {                    
        int off = nums[i] - nums[0];
        mask |= 1 << off;
      }                 
      msg.ackMore = ReceiveWindow.toAckMore(mask, 4);
    }
    checkAckHeaders(msg);
  }
  
  private void verify(String expected)
  {                     
    StringBuffer s = new StringBuffer();
    Packet p = head;    
    int num = 0;        
    while (p != null)
    {          
      if (s.length() > 0) s.append(' ');  
      s.append(p.seqNum);
      if (p.acked) s.append('a');
      p = p.next;
      num++;
    }                            
    if (full()) s.append(" full");
    String actual = s.toString().trim();                   
    
    verify(num == size);
    
    if (expected.equals(""))
    { 
      verify(head == null);
      verify(tail == null);
    }
    
    if (expected.equals(actual))                               
      verifies++;
    else
      throw new RuntimeException(expected + " != " + actual);
  }            
  
  private void verify(boolean x)
  {
    if (x)                               
      verifies++;
    else
      throw new RuntimeException();
  }

  private static int verifies;      
  
////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////
  
  private final DaspSession session;
  private final DaspSocket socket;
  private Packet head;
  private Packet tail;
  private String blocked;
  private boolean isAlive = true;
  private int size;                // current number of messages
  private int seqNum;              // next outgoing sequence number
  long sendRetry = 1000;           // ms
  int  maxSend = 3;                // max number of times to send a datagram packet
  int sendSize = 8;                // current max num for sending window
  int[] ackTimes;                  // circular list of ack times in ms
  private int ackTimesPos;         // next index to write in ackTimes
  
}
