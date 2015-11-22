//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Nov 08  Brian Frank  Creation
//

#include "sedona.h"

// static Str fromBytes(byte[] buf, int len)
Cell sys_Str_fromBytes(SedonaVM* vm, Cell* params)
{
  uint8_t* buf  = params[0].aval;
  uint32_t off  = params[1].ival;
  Cell ret;

  ret.aval = buf + off;
  
  return ret;
}

