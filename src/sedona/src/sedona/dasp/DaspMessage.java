//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Mar 08  Brian Frank  Creation
//

package sedona.dasp;

import java.io.*;
import java.net.*;
import java.util.*;
import sedona.util.TextUtil;

/**
 * DaspMessage encapsulates a DASP packet with associated headers.
 */
public class DaspMessage
  implements DaspConst
{

////////////////////////////////////////////////////////////////
// Public Access
////////////////////////////////////////////////////////////////

  /**
   * Get the session this message was received from.
   */
  public DaspSession session() { return session; }

  /**
   * Get the payload of the message.
   */
  public byte[] payload() { return payload; }

  /**
   * String representation.
   */
  public String toString()
  {
    return "DaspMessage @ " + session + " (" + payload.length + " bytes)";
  }

////////////////////////////////////////////////////////////////
// Header Field Access
////////////////////////////////////////////////////////////////

  int version()
  {
    if (version == -1) throw new RuntimeException("Missing version header field");
    return version;
  }

  int remoteId()
  {
    if (remoteId == -1) throw new RuntimeException("Missing remoteId header field");
    return remoteId;
  }

  String digestAlgorithm()
  {
    if (digestAlgorithm == null) return "SHA-1";
    return digestAlgorithm;
  }

  byte[] nonce()
  {
    if (nonce == null) throw new RuntimeException("Missing nonce header field");
    return nonce;
  }

  String username()
  {
    if (username == null) throw new RuntimeException("Missing username header field");
    return username;
  }

  byte[] digest()
  {
    if (digest == null) throw new RuntimeException("Missing digest header field");
    return digest;
  }

  int idealMax()
  {
    if (idealMax == -1) return IDEAL_MAX_DEF;
    return idealMax;
  }

  int absMax()
  {
    if (absMax == -1) return ABS_MAX_DEF;
    return absMax;
  }

  int receiveMax()
  {
    if (receiveMax == -1) return RECEIVE_MAX_DEF;
    return receiveMax;
  }

  long receiveTimeout()
  {
    if (receiveTimeout == -1) return RECEIVE_TIMEOUT_DEF;
    return receiveTimeout;
  }

////////////////////////////////////////////////////////////////
// Encode
////////////////////////////////////////////////////////////////

  int encode(byte[] buf)
  {
    int pos = 0;
    pos = u2(pos, buf, sessionId);
    pos = u2(pos, buf, seqNum);
    pos++;  // ctrl byte

    boolean hasIdealMax       = idealMax       != -1 && idealMax       != IDEAL_MAX_DEF;
    boolean hasAbsMax         = absMax         != -1 && absMax         != ABS_MAX_DEF;
    boolean hasReceiveMax     = receiveMax     != -1 && receiveMax     != RECEIVE_MAX_DEF;
    boolean hasReceiveTimeout = receiveTimeout != -1 && receiveTimeout != RECEIVE_TIMEOUT_DEF;

    int num = 0;
    if (version != -1)           { num++; buf[pos++] = (byte)VERSION;          pos = u2(pos, buf, version); }
    if (remoteId != -1)          { num++; buf[pos++] = (byte)REMOTE_ID;        pos = u2(pos, buf, remoteId); }
    if (digestAlgorithm != null) { num++; buf[pos++] = (byte)DIGEST_ALGORITHM; pos = str(pos, buf, digestAlgorithm); }
    if (nonce != null)           { num++; buf[pos++] = (byte)NONCE;            pos = bytes(pos, buf, nonce); }
    if (username != null)        { num++; buf[pos++] = (byte)USERNAME;         pos = str(pos, buf, username); }
    if (digest != null)          { num++; buf[pos++] = (byte)DIGEST;           pos = bytes(pos, buf, digest); }
    if (hasIdealMax)             { num++; buf[pos++] = (byte)IDEAL_MAX;        pos = u2(pos, buf, idealMax); }
    if (hasAbsMax)               { num++; buf[pos++] = (byte)ABS_MAX;          pos = u2(pos, buf, absMax); }
    if (ack != -1)               { num++; buf[pos++] = (byte)ACK;              pos = u2(pos, buf, ack); }
    if (ackMore != null)         { num++; buf[pos++] = (byte)ACK_MORE;         pos = bytes(pos, buf, ackMore); }
    if (hasReceiveMax)           { num++; buf[pos++] = (byte)RECEIVE_MAX;      pos = u2(pos, buf, receiveMax); }
    if (hasReceiveTimeout)       { num++; buf[pos++] = (byte)RECEIVE_TIMEOUT;  pos = u2(pos, buf, (int)(receiveTimeout/1000L)); }
    if (errorCode != -1)         { num++; buf[pos++] = (byte)ERROR_CODE;       pos = u2(pos, buf, errorCode); }

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
// Decode
////////////////////////////////////////////////////////////////

  DaspMessage decode(byte[] buf, int len)
    throws Exception
  {
    // 5 byte header
    this.sessionId = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
    this.seqNum    = ((buf[2] & 0xff) << 8) | (buf[3] & 0xff);
    this.msgType   = (buf[4] & 0xff) >> 4;
    int numFields  = buf[4] & 0xf;

    // header fields
    int pos = 5;
    for (int i=0; i<numFields; ++i)
    {
      int id = buf[pos++];

      // parse header value
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
      }

      // set header field
      switch (id)
      {
        case VERSION:          version         = u2;    break;
        case REMOTE_ID:        remoteId        = u2;    break;
        case DIGEST_ALGORITHM: digestAlgorithm = str;   break;
        case NONCE:            nonce           = bytes; break;
        case USERNAME:         username        = str;   break;
        case DIGEST:           digest          = bytes; break;
        case IDEAL_MAX:        idealMax        = u2;    break;
        case ABS_MAX:          absMax          = u2;    break;
        case ACK:              ack             = u2;    break;
        case ACK_MORE:         ackMore         = bytes; break;
        case RECEIVE_MAX:      receiveMax      = u2;    break;
        case RECEIVE_TIMEOUT:  receiveTimeout  = u2 * 1000L; break;
        case ERROR_CODE:       errorCode       = u2;    break;
      }
    }

    // payload is whatever is left (we need to
    // make a copy  since  input buffer is reused)
    len = len - pos;
    this.payload = len == 0 ? noBytes : new byte[len];
    System.arraycopy(buf, pos, this.payload, 0, len);

    return this;
  }

////////////////////////////////////////////////////////////////
// Debug
////////////////////////////////////////////////////////////////

  public void dump() { dump(new PrintWriter(System.out)); }

  public void dump(PrintWriter out)
  {
    out.println("sessionId=" + sessionId + " msgType=" + msgType + " seqNum=" + seqNum);

    if (version != -1)           out.println("  version         = " + version);
    if (remoteId != -1)          out.println("  remoteId        = " + remoteId);
    if (digestAlgorithm != null) out.println("  digestAlgorithm = " + digestAlgorithm);
    if (nonce != null)           out.println("  nonce           = " + toString(nonce));
    if (username != null)        out.println("  username        = " + username);
    if (digest != null)          out.println("  digest          = " + toString(digest));
    if (idealMax != -1)          out.println("  idealMax        = " + idealMax);
    if (absMax != -1)            out.println("  absMax          = " + absMax);
    if (ack != -1)               out.println("  ack             = " + ack);
    if (ackMore != null)         out.println("  ackMore         = " + toString(ackMore));
    if (receiveMax != -1)        out.println("  receiveMax      = " + receiveMax);
    if (receiveTimeout != -1)    out.println("  receiveTimeout  = " + receiveTimeout);
    if (errorCode != -1)         out.println("  errorCode       = " + errorCode);

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
// Headers
////////////////////////////////////////////////////////////////

  private static final byte[] noBytes = new byte[0];

  // 5 byte header
  int sessionId = -1;
  int msgType = -1;
  int seqNum  = -1;

  // header fields
  int version = -1;
  int remoteId = -1;
  String digestAlgorithm;
  byte[] nonce;
  String username;
  byte[] digest;
  int idealMax = -1;
  int absMax = -1;
  int ack = -1;
  byte[] ackMore;
  int receiveMax = -1;
  long receiveTimeout = -1;   // ms (encoded as sec)
  int errorCode = -1;

  // payload bytes
  byte[] payload = noBytes;

  // next in Queue linked list
  DaspMessage next;

  // DaspSession
  DaspSession session;
}
