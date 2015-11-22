//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Mar 07  Brian Frank  Creation
//

#include "sedona.h"

//////////////////////////////////////////////////////////////////////////
// Memory Management
//////////////////////////////////////////////////////////////////////////

// static byte[] Sys.malloc(int num)
Cell sys_Sys_malloc(SedonaVM* vm, Cell* params)
{
  size_t num = (size_t)params[0].ival;
  void* mem;
  Cell ret;

  mem = (void*)malloc(num);
  if (mem != NULL) memset(mem, 0, num);

  ret.aval = mem;
  return ret;
}

// static void Sys.free(Obj mem)
Cell sys_Sys_free(SedonaVM* vm, Cell* params)
{
  void* mem = params[0].aval;

  if (mem != NULL) free(mem);

  return nullCell;
}

// static void Sys.copy(byte[] src, int srcOff, byte[] dest, int destOff, int num)
Cell sys_Sys_copy(SedonaVM* vm, Cell* params)
{
  uint8_t* src     = params[0].aval;
  int32_t  srcOff  = params[1].ival;
  uint8_t* dest    = params[2].aval;
  int32_t  destOff = params[3].ival;
  int32_t  num     = params[4].ival;
  Cell ret;

  if (num > 0)
    memmove(dest+destOff, src+srcOff, num);

  ret.ival = 0;
  return ret;
}

// static void compareBytes(byte[] a, int aoff, byte[] b, int boff, int len)
Cell sys_Sys_compareBytes(SedonaVM* vm, Cell* params)
{
  uint8_t* a    = params[0].aval;
  int32_t  aoff = params[1].ival;
  uint8_t* b    = params[2].aval;
  int32_t  boff = params[3].ival;
  int32_t  len  = params[4].ival;
  int i;
  
  a = a + aoff;
  b = b + boff;
  for (i=0; i<len; ++i)
  {          
    int ai = a[i];
    int bi = b[i];
    if (ai != bi) return ai < bi ? negOneCell : oneCell;
  }                          
  
  return zeroCell;
}

// static void setBytes(int val, byte[] bytes, int off, int len)
Cell sys_Sys_setBytes(SedonaVM* vm, Cell* params)
{
  uint8_t  val   = (uint8_t)params[0].ival;
  uint8_t* bytes = (uint8_t*)params[1].aval;
  size_t  off    = (size_t)params[2].ival;
  size_t  len    = (size_t)params[3].ival;

  bytes = bytes+off;
  memset(bytes, val, len);
  return nullCell;
}

// static void andBytes(int mask, byte[] bytes, int off, int bytesLen)
Cell sys_Sys_andBytes(SedonaVM* vm, Cell* params)
{
  uint8_t  mask  = (uint8_t)params[0].ival;
  uint8_t* bytes = (uint8_t*)params[1].aval;
  size_t  off    = (size_t)params[2].ival;
  size_t  len    = (size_t)params[3].ival;
  int i;

  bytes = bytes+off;
  
  switch (len)
  {
    case 8:   bytes[7]  &= mask;
    case 7:   bytes[6]  &= mask;
    case 6:   bytes[5]  &= mask;
    case 5:   bytes[4]  &= mask;
    case 4:   bytes[3]  &= mask;
    case 3:   bytes[2]  &= mask;
    case 2:   bytes[1]  &= mask;
    case 1:   bytes[0]  &= mask;
    case 0:   break;
    default:  for (i=0; i<len; ++i) bytes[i] &= mask;
  }

  return nullCell;
}

// static void orBytes(int mask, byte[] bytes, int off, int bytesLen)
Cell sys_Sys_orBytes(SedonaVM* vm, Cell* params)
{
  uint8_t  mask  = (uint8_t)params[0].ival;
  uint8_t* bytes = (uint8_t*)params[1].aval;
  size_t  off    = (size_t)params[2].ival;
  size_t  len    = (size_t)params[3].ival;
  int i;

  bytes = bytes+off;

  switch (len)
  {
    case 8:   bytes[7]  |= mask;
    case 7:   bytes[6]  |= mask;
    case 6:   bytes[5]  |= mask;
    case 5:   bytes[4]  |= mask;
    case 4:   bytes[3]  |= mask;
    case 3:   bytes[2]  |= mask;
    case 2:   bytes[1]  |= mask;
    case 1:   bytes[0]  |= mask;
    case 0:   break;
    default:  for (i=0; i<len; ++i) bytes[i] |= mask;
  }

  return nullCell;
}

// static byte[] scodeAddr()
Cell sys_Sys_scodeAddr(SedonaVM* vm, Cell* params)
{                          
  Cell r;  
  r.aval = (uint8_t*)vm->codeBaseAddr;
  return r;
}

////////////////////////////////////////////////////////////////
// Float/Bit Conversion
////////////////////////////////////////////////////////////////

// int Sys.floatToBits(float)
Cell sys_Sys_floatToBits(SedonaVM* vm, Cell* params)
{
  // assume processor uses IEEE 754 format
  return *params;
}

// long Sys.doubleToBits(double)
int64_t sys_Sys_doubleToBits(SedonaVM* vm, Cell* params)
{ 
  // assume processor uses IEEE 754 format
  return *(int64_t*)params;
}

// float Sys.bitsToFloat(int)
Cell sys_Sys_bitsToFloat(SedonaVM* vm, Cell* params)
{
  // assume processor uses IEEE 754 format
  return *params;
}

// double Sys.bitsToDouble(long)
int64_t sys_Sys_bitsToDouble(SedonaVM* vm, Cell* params)
{                          
  // assume processor uses IEEE 754 format  
  return *(int64_t*)params;
}



