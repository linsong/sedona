//
// Copyright (c) 2010 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   09 Jul 10  Matthew Giannini 
//
package sedona.dasp;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;

import sedona.dasp.DaspConst;
import sedona.util.TextUtil;

/**
 * Models a DASP message.
 * <p>
 * See the DASP specification for details on message format and explanations
 * of the various header fields and message types.
 * 
 * @author Matthew Giannini
 * @creation Jul 9, 2010
 *
 */
public class DaspMsg implements DaspConst
{
////////////////////////////////////////////////////////////////
// Static Factories
////////////////////////////////////////////////////////////////

  /**
   * Decodes the datagram packet into a DaspMessage.
   * 
   * @return the decoded message.
   * @throws UnsupportedEncodingException
   *           if a Str header in the message is not properly encoded.
   * @see #decode(byte[], int)
   */
  public static DaspMsg decode(DatagramPacket p)
    throws UnsupportedEncodingException
  {
    return decode(p.getData(), p.getLength());
  }
  
  /**
   * Decodes the given buffer into a DaspMessage.
   * 
   * @param buf
   *          the byte[] containing the message to decode.
   * @param length
   *          the length of the dasp message. It must be less than or equal to
   *          the length of the {@code buf}.
   * 
   * @return the decoded message.
   * @throws UnsupportedEncodingException
   *           if a Str header in the message is not properly encoded.
   */
  public static DaspMsg decode(byte[] buf, final int length) 
    throws UnsupportedEncodingException
  {
    
    return new DaspMsg(buf, length);
  }
  
////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  /**
   * Create an empty DASP message.  This constructor should be used when
   * you want to explicitly build up a DASP message from scratch.
   */
  public DaspMsg()
  {  
  }
  
  /**
   * Construct a DaspMessage by decoding the given buffer.
   * 
   * @param buf the byte[] containing the message to decode.
   * @param length the length of the dasp message. It must be less than or equal
   * to the length of the {@code buf}.
   * 
   * @throws UnsupportedEncodingException
   *           if a Str header in the message is not properly encoded.
   */
  protected DaspMsg(byte[] buf, final int length) throws UnsupportedEncodingException
  {
    if (length > buf.length) throw new IllegalArgumentException("length parameter is larger than buf");
    doDecode(buf, length);
  }
  
////////////////////////////////////////////////////////////////
// Decode
////////////////////////////////////////////////////////////////

  protected void doDecode(byte[] buf, final int length) throws UnsupportedEncodingException
  {
    // header
    sessionId = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
    seqNum    = ((buf[2] & 0xff) << 8) | (buf[3] & 0xff);
    msgType   = (buf[4] & 0xff) >> 4;
    final int numFields = (buf[4] & 0xf);
    
    int pos = 5;
    for (int i=0; i<numFields; ++i)
    {
      int id = buf[pos++];
      
      int u2 = 0;
      String str = null;
      byte[] bytes = null;
      switch (id & 0x3)
      {
        case 0:
          break;
        case 1:
          u2 = ((buf[pos++] & 0xff) << 8) | (buf[pos++] & 0xff);
          break;
        case 2:
          int s = pos;
          while (buf[pos++] != 0);
          str = new String(buf, s, pos-s-1, "UTF-8");
          break;
        case 3:
          int blen = buf[pos++] & 0xff;
          bytes = blen == 0 ? noBytes : new byte[blen];
          System.arraycopy(buf, pos, bytes, 0, blen);
          pos += blen;
          break;
        default:
          throw new IllegalStateException("Unknown header type: " + (id & 0x3));
      }
      
      switch (id)
      {
        case VERSION:           version         = u2;     break;
        case REMOTE_ID:         remoteId        = u2;     break;
        case DIGEST_ALGORITHM:  digestAlgorithm = str;    break;
        case NONCE:             nonce           = bytes;  break;
        case USERNAME:          username        = str;    break;
        case DIGEST:            digest          = bytes;  break;
        case IDEAL_MAX:         idealMax        = u2;     break;
        case ABS_MAX:           absMax          = u2;     break;
        case ACK:               ack             = u2;     break;
        case ACK_MORE:          ackMore         = bytes;  break;
        case RECEIVE_MAX:       receiveMax      = u2;     break;
        case RECEIVE_TIMEOUT:   receiveTimeout  = u2 * 1000L; break;
        case ERROR_CODE:        errorCode       = u2;     break;
        case PLATFORM_ID:       platformId      = str;    break;
        default: throw new IllegalStateException("Unknown field id: " + id);
      }
    }
    
    // payload is whatever is left
    int len = length - pos;
    payload = (len == 0) ? noBytes : new byte[len];
    System.arraycopy(buf, pos, payload, 0, len);
  }
  
////////////////////////////////////////////////////////////////
// Encode
////////////////////////////////////////////////////////////////
  
  /**
   * Encode this DASP message into the given buffer. The default encoding
   * does not do any message validation.  If a header is set and has a non-default
   * value, then it will be encoded regardless of the msgType.
   * 
   * @param buf
   *          the buffer to encode the message into.
   * 
   * @return the actual length of the encoded DASP message.
   */
  public int encode(byte[] buf)
  {
    int pos = 0;
    pos = u2(pos, buf, sessionId);
    pos = u2(pos, buf, seqNum);
    pos++;  // skip msgType|numFields byte. Will backpatch.

    boolean hasIdealMax       = idealMax       > -1 && idealMax       != IDEAL_MAX_DEF;
    boolean hasAbsMax         = absMax         > -1 && absMax         != ABS_MAX_DEF;
    boolean hasReceiveMax     = receiveMax     > -1 && receiveMax     != RECEIVE_MAX_DEF;
    boolean hasReceiveTimeout = receiveTimeout > -1 && receiveTimeout != RECEIVE_TIMEOUT_DEF;

    int num = 0;
    if (version > -1)            { num++; buf[pos++] = (byte)VERSION;          pos = u2(pos, buf, version); }
    if (remoteId > -1)           { num++; buf[pos++] = (byte)REMOTE_ID;        pos = u2(pos, buf, remoteId); }
    if (digestAlgorithm != null) { num++; buf[pos++] = (byte)DIGEST_ALGORITHM; pos = str(pos, buf, digestAlgorithm); }
    if (nonce != null)           { num++; buf[pos++] = (byte)NONCE;            pos = bytes(pos, buf, nonce); }
    if (username != null)        { num++; buf[pos++] = (byte)USERNAME;         pos = str(pos, buf, username); }
    if (digest != null)          { num++; buf[pos++] = (byte)DIGEST;           pos = bytes(pos, buf, digest); }
    if (hasIdealMax)             { num++; buf[pos++] = (byte)IDEAL_MAX;        pos = u2(pos, buf, idealMax); }
    if (hasAbsMax)               { num++; buf[pos++] = (byte)ABS_MAX;          pos = u2(pos, buf, absMax); }
    if (ack > -1)                { num++; buf[pos++] = (byte)ACK;              pos = u2(pos, buf, ack); }
    if (ackMore != null)         { num++; buf[pos++] = (byte)ACK_MORE;         pos = bytes(pos, buf, ackMore); }
    if (hasReceiveMax)           { num++; buf[pos++] = (byte)RECEIVE_MAX;      pos = u2(pos, buf, receiveMax); }
    if (hasReceiveTimeout)       { num++; buf[pos++] = (byte)RECEIVE_TIMEOUT;  pos = u2(pos, buf, (int)(receiveTimeout/1000L)); }
    if (errorCode > -1)          { num++; buf[pos++] = (byte)ERROR_CODE;       pos = u2(pos, buf, errorCode); }
    if (platformId != null)      { num++; buf[pos++] = (byte)PLATFORM_ID;      pos = str(pos, buf, platformId); }

    // backpatch msgType and numFields
    buf[4] = (byte)((msgType << 4) | num);

    if (payload != null)
    {
      System.arraycopy(payload, 0, buf, pos, payload.length);
      pos += payload.length;
    }

    return pos;
  }
  
  private int u2(int pos, byte[] buf, int val)
  {
    buf[pos++] = (byte)(val >>> 8);
    buf[pos++] = (byte)val;
    return pos;
  }

  private int bytes(int pos, byte[] buf, byte[] val)
  {
    buf[pos++] = (byte)val.length;
    System.arraycopy(val, 0, buf, pos, val.length);
    return pos+val.length;
  }

  private int str(int pos, byte[] buf, String val)
  {
    for (int i=0; i<val.length(); ++i)
    {
      int c = val.charAt(i);
      if (c > 0x7f) throw new RuntimeException("String must be ASCII " + val);
      buf[pos++] = (byte)c;
    }
    buf[pos++] = 0;
    return pos;
  }
  
////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////
  
  /**
   * Get the session id for this message.
   */
  public final int sessionId()
  {
    return sessionId;
  }
  
  /**
   * Set the session id for this dasp message.
   * 
   * @param sessionId
   *          the new session id.
   */
  public final void setSessionId(final int sessionId)
  {
    this.sessionId = sessionId;
  }
  
  /**
   * Get the sequence number.
   */
  public final int seqNum()
  {
    return this.seqNum;
  }
  
  /**
   * Set the sequence number.
   * 
   * @param seqNum
   *          the sequence number.
   */
  public final void setSeqNum(final int seqNum)
  {
    this.seqNum = seqNum;
  }
  
  /**
   * Get the message type.
   */
  public final int msgType()
  {
    return msgType;
  }
  
  /**
   * Set the message type.
   * 
   * @param msgType the message type.
   */
  public final void setMsgType(final int msgType)
  {
    this.msgType = msgType;
  }
  
  /**
   * Get the payload bytes.
   * 
   * @return the payload. If the message has no payload the array will be
   *         zero-length.
   */
  public final byte[] payload()
  {
    return payload;
  }
  
  /**
   * Set the payload.
   * 
   * @param payload
   *          the payload for this message, or null to clear the payload.
   */
  public final void setPayload(byte[] payload)
  {
    if (payload == null || payload.length == 0)
      this.payload = noBytes;
    else
    {
      this.payload = new byte[payload.length];
      System.arraycopy(payload, 0, this.payload, 0, payload.length);
    }
  }
  
  /**
   * Get the DASP version for this message.
   * 
   * @return the DASP version, or -1 if this header is not set.
   */
  public final int version()
  {
    return Math.max(-1, version);
  }
  
  /**
   * Set the DASP protocol version.
   * 
   * @param version
   *          the protocol version. Use a negative value to clear this header.
   */
  public final void setVersion(final int version)
  {
    this.version = Math.max(-1, version);
  }

  /**
   * Get the remoteId header.
   * 
   * @return the remoteId header, or -1 if the header is not set.
   */
  public final int remoteId()
  {
    return Math.max(-1, remoteId);
  }
  
  /**
   * Set the remoteId header.
   * 
   * @param remoteId
   *          the remote id. Use a negative value to clear this header.
   */
  public final void setRemoteId(final int remoteId)
  {
    this.remoteId = Math.max(-1, remoteId);
  }

  /**
   * Get the digest algorithm.
   */
  public final String digestAlgorithm()
  {
    if (digestAlgorithm == null) return "SHA-1";
    return digestAlgorithm;
  }
  
  /**
   * Set the digest algorithm.
   * 
   * @param digestAlgorithm
   *          the digest algorithm.
   */
  public final void setDigestAlgorithm(String digestAlgorithm)
  {
    this.digestAlgorithm = "SHA-1".equals(digestAlgorithm) ? null : digestAlgorithm;
  }

  /**
   * Get the nonce bytes.
   * 
   * @return the nonce bytes, or null if this header is not set.
   */
  public final byte[] nonce()
  {
    return nonce;
  }
  
  /**
   * Set the nonce for this message.
   * 
   * @param nonce
   *          the nonce for this message, or null to clear this header.
   */
  public final void setNonce(byte[] nonce)
  {
    if (nonce == null) 
      this.nonce = null;
    else
    {
      this.nonce = new byte[nonce.length];
      System.arraycopy(nonce, 0, this.nonce, 0, nonce.length);
    }
  }

  /**
   * Get the username header field.
   * 
   * @return the username, or null if this header is not set.
   */
  public final String username()
  {
    return username;
  }
  
  /**
   * Set the username header.
   * 
   * @param username
   *          the username, or null to clear this field.
   */
  public final void setUsername(String username)
  {
    this.username = username;
  }

  /**
   * Get the digest header field.
   * 
   * @return the digest header field, or null if the header is not set.
   */
  public final byte[] digest()
  {
    return digest;
  }
  
  /**
   * Set the digest header field.
   * 
   * @param digest the digest bytes, or null to clear this header.
   */
  public final void setDigest(byte[] digest)
  {
    if (digest == null)
      this.digest = null;
    else
    {
      this.digest = new byte[digest.length];
      System.arraycopy(digest, 0, this.digest, 0, digest.length);
    }
  }

  /**
   * Get the ideal max header field.
   * 
   * @return the ideal max header.
   */
  public final int idealMax()
  {
    return idealMax < 0 ? IDEAL_MAX_DEF : idealMax;
  }
  
  /**
   * Set the ideal max header field.
   * 
   * @param idealMax
   *          the ideal max header. Use a negative value to indicate the default
   *          value.
   */
  public final void setIdealMax(int idealMax)
  {
    this.idealMax = Math.max(-1, idealMax);
  }

  /**
   * Get the abs max header field.
   * 
   * @return the abs max header.
   */
  public final int absMax()
  {
    return absMax < 0 ? ABS_MAX_DEF : absMax;
  }
  
  /**
   * Set the abs max header field.
   * 
   * @param absMax
   *          the abs max header. Use a negative value to indicate the default
   *          value.
   */
  public final void setAbsMax(int absMax)
  {
    this.absMax = Math.max(-1, absMax);
  }

  /**
   * Get the receive max header field.
   * 
   * @return the receive max header.
   */
  public final int receiveMax()
  {
    return receiveMax < 0 ? RECEIVE_MAX_DEF : receiveMax;
  }
  
  /**
   * Set the receive max header field.
   * 
   * @param receiveMax
   *          the receive max header. Use a negative value to indicate the
   *          default value.
   */
  public final void setReceiveMax(int receiveMax)
  {
    this.receiveMax = Math.max(-1, receiveMax);
  }

  /**
   * Get the receive timeout header field (in ms).
   * 
   * @return the receive timeout header (in ms).
   */
  public final long receiveTimeout()
  {
    return receiveTimeout < 0 ? RECEIVE_TIMEOUT_DEF : receiveTimeout;
  }
  
  /**
   * Set the receive timeout header field. The parameter should be in
   * milliseconds, but when the message is encoded it will be converted to
   * seconds (per the DASP specification).
   * 
   * @param receiveTimeout
   *          the receive timeout in ms. Use a negative value to indicate the
   *          default value.
   */
  public final void setReceiveTimeout(long receiveTimeout)
  {
    this.receiveTimeout = Math.max(-1L, receiveTimeout);
  }
  
  /**
   * Get the ack header field.
   * 
   * @return the ack header field, or -1 if the header is not set.
   */
  public final int ack()
  {
    return Math.max(-1, ack);
  }
  
  /**
   * Set the ack header field.
   * 
   * @param ack
   *          the ack header. Use a negative value to clear this header.
   */
  public final void setAck(final int ack)
  {
    this.ack = Math.max(-1, ack);
  }
  
  /**
   * Get the ackMore header field.
   * 
   * @return the ackMore header, or null if this header is not set.
   */
  public final byte[] ackMore()
  {
    return ackMore;
  }
  
  /**
   * Set the ackMore header field.
   * 
   * @param ackMore
   *          the ackMore header, or null to clear this header.
   */
  public final void setAckMore(byte[] ackMore)
  {
    if (ackMore == null)
      this.ackMore = null;
    else
    {
      this.ackMore = new byte[ackMore.length];
      System.arraycopy(ackMore, 0, this.ackMore, 0, ackMore.length);
    }
  }
  
  /**
   * Get the error code header field.
   * 
   * @return the error code, or -1 if the header is not set.
   */
  public final int errorCode()
  {
    return Math.max(-1, errorCode);
  }
  
  /**
   * Set the error code header field.
   * 
   * @param errorCode
   *          the error code. Use a negative value to clear this header.
   */
  public final void setErrorCode(final int errorCode)
  {
    this.errorCode = Math.max(-1, errorCode);
  }
  
    /**
   * Get the platformId header field.
   * 
   * @return the platformId, or null if this header is not set.
   */
  public final String platformId()
  {
    return platformId;
  }
  
  /**
   * Set the platformId header.
   * 
   * @param platformId
   *          the platformId, or null to clear this field.
   */
  public final void setPlatformId(String platformId)
  {
    this.platformId = platformId;
  }


////////////////////////////////////////////////////////////////
//Debug
////////////////////////////////////////////////////////////////

 public void dump() { dump(new PrintWriter(System.out)); }

 public void dump(PrintWriter out)
 {
   out.println("sessionId=" + sessionId + " msgType=" + msgType + " seqNum=" + seqNum);

   if (version > -1)            out.println("  version         = " + version);
   if (remoteId > -1)           out.println("  remoteId        = " + remoteId);
   if (digestAlgorithm != null) out.println("  digestAlgorithm = " + digestAlgorithm);
   if (nonce != null)           out.println("  nonce           = " + toString(nonce));
   if (username != null)        out.println("  username        = " + username);
   if (digest != null)          out.println("  digest          = " + toString(digest));
   if (idealMax > -1)           out.println("  idealMax        = " + idealMax);
   if (absMax > -1)             out.println("  absMax          = " + absMax);
   if (ack > -1)                out.println("  ack             = " + ack);
   if (ackMore != null)         out.println("  ackMore         = " + toString(ackMore));
   if (receiveMax > -1)         out.println("  receiveMax      = " + receiveMax);
   if (receiveTimeout > -1)     out.println("  receiveTimeout  = " + receiveTimeout);
   if (errorCode > -1)          out.println("  errorCode       = " + errorCode);
   if (platformId != null)      out.println("  platformId      = " + platformId);

   out.flush();
 }

 static String toString(byte[] buf) { return toString(buf, buf.length); }
 static String toString(byte[] buf, int len)
 {
   StringBuffer s = new StringBuffer("0x[");
   for (int i=0; i<len; ++i)
     s.append(TextUtil.byteToHexString(buf[i] & 0xFF));
   s.append(']');
   return s.toString();
 }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  protected static final byte[] noBytes = new byte[0];

  protected int sessionId = -1;
  protected int msgType = -1;
  protected int seqNum  = -1;
  
  // headers
  protected int     version = -1;
  protected int     remoteId = -1;
  protected String  digestAlgorithm;
  protected byte[]  nonce;
  protected String  username;
  protected byte[]  digest;
  protected int     idealMax = -1;
  protected int     absMax = -1;
  protected int     ack = -1;
  protected byte[]  ackMore;
  protected int     receiveMax = -1;
  protected long    receiveTimeout = -1;
  protected int     errorCode = -1;
  protected String  platformId;
  
  // payload bytes
  protected byte[] payload = noBytes;
}
