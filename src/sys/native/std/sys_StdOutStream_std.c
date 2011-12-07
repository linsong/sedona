//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Apr 07  Brian Frank  Creation
//

#include "sedona.h"

// bool StdOutStream.doWrite(int)
Cell sys_StdOutStream_doWrite(SedonaVM* vm, Cell* params)
{
  int32_t b = params[0].ival;

  putchar(b);
  if (b == (int32_t)'\n')
    fflush(stdout);

  return trueCell;
}

// bool StdOutStream.doWriteBytes(byte[], int, int)
Cell sys_StdOutStream_doWriteBytes(SedonaVM* vm, Cell* params)
{
  uint8_t* buf = (uint8_t*)params[0].aval;
  int32_t  off = params[1].ival;
  int32_t  len = params[2].ival;
  
  buf = buf + off;

  fwrite(buf, 1, len, stdout);

  return trueCell;
}

// void StdOutStream.doFlush()
Cell sys_StdOutStream_doFlush(SedonaVM* vm, Cell* params)
{
  fflush(stdout);
  return nullCell;
}



