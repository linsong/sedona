//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 Oct 06  Brian Frank  Creation
//

package sedona.sox;

import java.io.*;
import sedona.dasp.*;

/**
 * Msg models a binary packet of data.
 */
public class Msg
  extends sedona.Buf
{

////////////////////////////////////////////////////////////////
// Factories
////////////////////////////////////////////////////////////////

  static Msg prepareRequest(int cmd)
  {
    return prepareRequest(cmd, 0xff);
  }

  static Msg prepareRequest(int cmd, int replyNum)
  {
    Msg msg = new Msg();
    msg.u1(cmd);
    msg.u1(replyNum);
    return msg;
  }

  static Msg makeUpdateReq(int compId, int what)
  {
    Msg req = prepareRequest('c');
    req.u2(compId);
    req.u1(what);
    return req;
  }

  /**
   * @deprecated Pre Sox 1.1 messaging only
   */
  static Msg makeSubscribeReq(int compId, int what)
  {
    Msg req = prepareRequest('s');
    req.u2(compId);
    req.u1(what);
    return req;
  }

  /**
   * @deprecated Pre Sox 1.1 messaging only
   */
  static Msg makeUnsubscribeReq(int compId, int whatMask)
  {
    Msg req = prepareRequest('u');
    req.u2(compId);
    req.u1(whatMask);
    return req;
  }

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  public Msg(byte[] bytes) { super(bytes); }

  public Msg() { super(DaspConst.ABS_MAX_VAL); }

////////////////////////////////////////////////////////////////
// Sox Headers
////////////////////////////////////////////////////////////////

  boolean isError() { return bytes[0] == '!'; }

  int command() { return bytes[0]; }

  public int replyNum() { return bytes[1]; }

  public void setReplyNum(int num)
  {
    if (num > 0xff) throw new IllegalStateException("replyNum=" + num + " 0x" + Integer.toHexString(num));
    bytes[1] = (byte)(num & 0xff);
  }

////////////////////////////////////////////////////////////////
// Messaging
////////////////////////////////////////////////////////////////

  void checkResponse(int expectedCmd)
    throws IOException
  {
    int actualCmd = u1();
    int replyNum = u1();

    if (actualCmd == '!')
    {
      String cause = str();
      throw new SoxException("Request failed: " + cause);
    }

    if (actualCmd != expectedCmd)
    {
      String actualStr = (char)actualCmd + "(" + actualCmd + ")";
      String expectedStr = (char)expectedCmd + "(" + expectedCmd + ")";
      throw new SoxException(actualStr + " != " + expectedStr);
    }
  }

  public String toString()
  {
    return "" + (char)command() + " replyNum=" + replyNum() + " " + super.toString();
  }

}
