//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 Mar 08  Brian Frank  Creation
//

package sedona.dasp;

/**
 * DaspConst
 */
public interface DaspConst
{                                              

////////////////////////////////////////////////////////////////
// Java Implementation Defaults
////////////////////////////////////////////////////////////////

  public static final int  VERSION_VAL         = 0x0100; // 1.0
  public static final int  IDEAL_MAX_VAL       = 512;    // bytes
  public static final int  ABS_MAX_VAL         = 1024;   // bytes
  public static final int  RECEIVE_MAX_VAL     = 31;     // msgs (must be 31 or less to fit in 32-bit int)
  public static final long RECEIVE_TIMEOUT_VAL = 30000L; // ms
  public static final long CONNECT_TIMEOUT_VAL = 10000L; // ms
  public static final int  MAX_SESSIONS_VAL    = 10000;  // sessions

  static final int SESSION_QUEUE_MAX = 2000;
  static final int SOCKET_QUEUE_MAX  = 10000;                     
  static final long SEND_RETRY_VAL = 1000;  // ms

////////////////////////////////////////////////////////////////
// Specification Defaults
////////////////////////////////////////////////////////////////

  public static final int  IDEAL_MAX_DEF       = 512;    // bytes
  public static final int  ABS_MAX_DEF         = 512;    // bytes
  public static final int  RECEIVE_MAX_DEF     = 31;     // msgs
  public static final int  MAX_SEND            = 3;      // max times to resend unacked msg
  public static final long RECEIVE_TIMEOUT_DEF = 30000L; // ms

////////////////////////////////////////////////////////////////
// Message Types
////////////////////////////////////////////////////////////////

  public static final int DISCOVER     = 0;
  public static final int HELLO        = 1;
  public static final int CHALLENGE    = 2;
  public static final int AUTHENTICATE = 3;
  public static final int WELCOME      = 4;
  public static final int KEEPALIVE    = 5;
  public static final int DATAGRAM     = 6;
  public static final int CLOSE        = 7;

////////////////////////////////////////////////////////////////
// Header Values
////////////////////////////////////////////////////////////////

  public static final int NIL  = 0;
  public static final int U2   = 1;
  public static final int STR  = 2;
  public static final int BYTE = 3;

////////////////////////////////////////////////////////////////
// Header Ids
////////////////////////////////////////////////////////////////

  public static final int VERSION              = 0x05;  // (1,1)
  public static final int REMOTE_ID            = 0x09;  // (2,1)
  public static final int DIGEST_ALGORITHM     = 0x0e;  // (3,2)
  public static final int NONCE                = 0x13;  // (4,3)
  public static final int USERNAME             = 0x16;  // (5,2)
  public static final int DIGEST               = 0x1b;  // (6,3)
  public static final int IDEAL_MAX            = 0x1d;  // (7,1)
  public static final int ABS_MAX              = 0x21;  // (8,1)
  public static final int ACK                  = 0x25;  // (9,1)
  public static final int ACK_MORE             = 0x2b;  // (a,3)
  public static final int RECEIVE_MAX          = 0x2d;  // (b,1)
  public static final int RECEIVE_TIMEOUT      = 0x31;  // (c,1)
  public static final int ERROR_CODE           = 0x35;  // (d,1)
  public static final int PLATFORM_ID          = 0x3a;  // (e,2)

////////////////////////////////////////////////////////////////
// Error Codes
///////////////////////////////////////////////////////////////

  public static final int INCOMPATIBLE_VERSION = 0xe1;
  public static final int BUSY                 = 0xe2;
  public static final int DIGEST_NOT_SUPPORTED = 0xe3;
  public static final int NOT_AUTHENTICATED    = 0xe4;
  public static final int TIMEOUT              = 0xe5;
  
////////////////////////////////////////////////////////////////
// Device Discovery
///////////////////////////////////////////////////////////////

  public static final String IPv4_MULTICAST_ADDR = "239.255.18.76";  // RFC 2365
  public static final String IPv6_MULTICAST_ADDR = "FF02::1";  //"FF02::137";   // use Niagara's

}
