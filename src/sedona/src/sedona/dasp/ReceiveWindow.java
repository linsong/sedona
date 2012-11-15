//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Jan 07  Brian Frank  Creation
//

package sedona.dasp;

/**
 * ReceiveWindow manages the incoming message sequences and acknowledgements.
 */    
final class ReceiveWindow  
{                        

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  ReceiveWindow(DaspSession session)
  {
    this.session = session;       
  }
    
////////////////////////////////////////////////////////////////
// Sliding Window
////////////////////////////////////////////////////////////////

  /**
   * Return if we have unacked messages.
   */  
  final synchronized boolean unacked()
  {                  
    return !acked;
  }
  
  /**
   * If we have room within idealMax then 
   * add the ack and ackMore headers. 
   */
  final synchronized void setAckHeaders(DaspMsg msg)
  {   
    // if we have room within idealMax always add ack 
    // header where 8 = 5 fixed header + 3 ack header
    int payloadLen = msg.payload.length;
    if (payloadLen+8 <= session.idealMax)
    {
      msg.ack = seqNum;
      acked = true;
      
      // if we have ackMores and we have room for another 3 
      // bytes (total 11 bytes), then add the ackMore header
      if (seqMore > 0x1 && payloadLen+11 <= session.idealMax)
      {
        msg.ackMore = toAckMore(seqMore, session.idealMax - payloadLen - 10);
      }
    }
  }      
  
  /**
   * Given a 32-bit seqMore bitmask, return the ackMore header.
   * The max argument limits the result to 1, 2, or 3 bytes.
   */
  static byte[] toAckMore(int seqMore, int max)
  {
    if (seqMore <= 0xff     || max <= 1) 
      return new byte[] 
      { 
        (byte)(seqMore >>> 0) 
      };
      
    if (seqMore <= 0xffff   || max <= 2) 
      return new byte[] 
      { 
        (byte)(seqMore >>> 8), 
        (byte)(seqMore >>> 0) 
      };
      
    if (seqMore <= 0xffffff || max <= 3) 
      return new byte[] 
      { 
        (byte)(seqMore >>> 16), 
        (byte)(seqMore >>> 8), 
        (byte)(seqMore >>> 0) 
      };
      
    return new byte[] 
    { 
      (byte)(seqMore >>> 24), 
      (byte)(seqMore >>> 16), 
      (byte)(seqMore >>> 8), 
      (byte)(seqMore >>> 0) 
    };
  } 
  
  /**
   * Initialize the receive window with the handshake seqNum.
   */
  final synchronized void init(int num)
  {
    if (seqNum >= 0) throw new IllegalStateException();
    seqNum  = (num - 1) & 0xffff;   
    seqMore = 1;
    acked   = false;
  }
  
  /**
   * Receive the given sequence number.  Return true if the
   * message should be processed.  Return false if outside of
   * the receiving window or we have already processed it.
   */
  final synchronized boolean receive(int num)
  {                     
    // sanity check  
    if (num < 0 || num > 0xffff)   
      throw new IllegalStateException(""+num);
            
    // if seq num is still -1 this is our first incoming 
    // message, so let's use it to init our seqNum
    if (seqNum < 0) throw new IllegalStateException();
    
    // check if out of range within our current receiving window
    int start = seqNum;         // exclusive start of accept window
    int end   = start + max;    // inclusive end of accept window
    if (end <= 0xffff)          
    {
      if (num <= start || num > end) 
      {
        acked = false;  // force next outgoing msg to include ackNum
        return false;   
      }
    }                                                     
    else
    {                          
      end -= 0xffff;    
      if (num <= start && num > end) 
      {
        acked = false;  // force next outgoing msg to include ackNum
        return false;
      }
    }             
    
    // if next message in window, then slide the window 
    if (num == seqNum+1 || (num == 0 && seqNum == 0xffff))
    {                                           
      do
      {
        seqNum++;                                      
        seqMore = (seqMore >> 1) & 0x7fffffff;
      } 
      while ((seqMore & 0x2) != 0); 
      seqNum &= 0xffff;
      seqMore |= 0x1;                        
    }                       
    
    // otherwise we've received a message out of order
    else
    {                   
      int bit = 1 << ((num - start) & 0xffff);
      if ((seqMore & bit) != 0) return false;
      seqMore |= bit;
    }

    // clean acked flag so that we know to ack these received
    acked = false;
              
    // successful received    
    return true;
  }                               

////////////////////////////////////////////////////////////////
// Test
////////////////////////////////////////////////////////////////

  public static void main(String[] args)        
    throws Exception
  {                          
    long t1 = System.currentTimeMillis();
    DaspSocket sock = DaspSocket.open(-1, null, DaspSocket.SESSION_QUEUING);
    DaspSession s = new DaspSession(sock.interfaces[0], 0, null, 0, true, new java.util.Properties());
    ReceiveWindow x = new ReceiveWindow(s);
    x.test();
    long t2 = System.currentTimeMillis();
    System.out.println("  ReceiveWindow Success: " + verifies + " verifies [" + (t2-t1) + "ms]");
    sock.close();
  }                            
  
  private void test()
  {                          
    verify(-1, -1);   
    seqNum = -1; init(1);    
    
    // in order     
    int num = 0;
    for (int i=1; i<=70000; ++i)
    {                      
      int prev = (i-1) & 0xffff;
      num  = i & 0xffff;     
      int big  = (i+32) & 0xffff;
      
      verify(receive(num),   "num=" + num);
      verify(!receive(prev), "prev=" + prev);
      verify(!receive(big),  "big=" + big);
      verify(num, 1);
    }    
        
    // receive out of order
    for (int i=1; i<=70000; ++i)
    {                          
      num = i & 0xffff;  
      int plusTwo  = (num+2) & 0xffff;
      int plusFour = (num+4) & 0xffff;
         
      seqNum  = num;
      seqMore = 1;
      verify(receive(plusTwo));
      verify(num, 0x5);                
      verify(receive(plusFour));
      verify(num, 0x15);                
      num = (num+1) & 0xffff;
      verify(receive(num));
      verify(plusTwo, 0x5);            
      verify(!receive(plusTwo));
      verify(!receive(plusFour));
      num = (num+2) & 0xffff;
      verify(receive(num));
      verify(plusFour, 0x1);                
      verify(!receive(plusTwo));
      verify(!receive(plusFour));
      verify(!receive(num));
    }                
        
    // receive out of order 31-down
    for (int i=1; i<=70000; ++i)
    {                 
      num = i & 0xffff;
      seqNum  = num;
      seqMore = 1;
      verify(num, 0x1);                
      int mask = 0;
      for (int j=31; j>1; --j)
      {                     
        int x = (num+j) & 0xffff;
        verify(receive(x));
        mask |= 1 << j;
        verify(num, mask|0x1);                
      }      
      verify(receive((num+1)&0xffff));
      verify((num+31)&0xffff, 0x1);
    }                                    
    
  }           
  
    
  private void verify(int seqNum, int seqMore)
  {                                
    if (this.seqNum != seqNum) throw new RuntimeException(this.seqNum + " != " + seqNum);
    if (this.seqMore != seqMore)throw new RuntimeException("0x" + Integer.toHexString(this.seqMore) + " != 0x" + Integer.toHexString(seqMore));
    verifies++; 
  }            

  private void verify(boolean x) { verify(x, ""); }
  private void verify(boolean x, String msg)
  {
    if (!x) throw new RuntimeException(msg);
    verifies++;
  }
  
  private static int verifies;   

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static final int max = DaspConst.RECEIVE_MAX_VAL;
  
  private final DaspSession session;
  private int seqNum  = -1;    // successful received - start of window
  private int seqMore = -1;    // successful received out of order
  private boolean acked;       // have we acked seqNum and seqMore
  
}
