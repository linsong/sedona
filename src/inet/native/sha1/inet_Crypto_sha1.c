//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Mar 08  Brian Frank  Creation
//

#include "sedona.h"
#include "inet_sha1.h"

//
// This implementation of Crypt.sha1 uses the code straight
// from RFC 3174:
//
//    http://tools.ietf.org/html/rfc3174
//
// static void sha1(byte[] input, int inputOff, int len, byte[] output, int outputOff)
//
Cell inet_Crypto_sha1(SedonaVM* vm, Cell* params)
{
  uint8_t* in     = params[0].aval;
  int32_t  inOff  = params[1].ival;
  int32_t  len    = params[2].ival;
  uint8_t* out    = params[3].aval;
  int32_t  outOff = params[4].ival;
  SHA1Context cx;

  in  = in + inOff;
  out = out + outOff;

  SHA1Reset(&cx);
  SHA1Input(&cx, in, len);
  SHA1Result(&cx, out);

  return nullCell;
}






