//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Mar 07  Brian Frank  Creation
//

#include "sedona.h"

//////////////////////////////////////////////////////////////////////////
// String Formating
//////////////////////////////////////////////////////////////////////////

static char strbuf[32];

static int formatIntPart(int64_t val, int radix, int n)
{
  uint64_t working;
  char temp[32];
  int wn = 0;
  int i;

  // negative
  if (val < 0 && radix != 16)
  {
    val = -val;
    strbuf[n++] = '-';
  }

  // write chars backwards
  working = (uint64_t)val;
  do
  {
    temp[wn++] = "0123456789abcdef"[working%radix];        // umoddi3
    working /= radix;                                      // udivdi3
  }
  while(working != 0);

  // reverse chars
  for (i=0; i<wn; ++i)
    strbuf[n++] = temp[wn-i-1];

  return n;
}

static Cell formatIntToCell(int64_t val, int radix)
{
  Cell result;
  int n = 0;

  n = formatIntPart(val, radix, n);
  strbuf[n++] = '\0';

  result.aval = strbuf;
  return result;
}

// Str Sys.intStr(int)
Cell sys_Sys_intStr(SedonaVM* vm, Cell* params)
{
  int32_t i = params[0].ival;
  return formatIntToCell(i, 10);   // keep sign ext if any
}

// Str Sys.hexStr(int)
Cell sys_Sys_hexStr(SedonaVM* vm, Cell* params)
{
  int32_t i = params[0].ival;

  // Mask off any sign ext since passing as 64-bit
#ifdef _WIN32
  return formatIntToCell(i & 0xffffffffi64, 16);
#else
  return formatIntToCell(i & 0xffffffffLL, 16);
#endif
}


// Str Sys.longStr(long)
Cell sys_Sys_longStr(SedonaVM* vm, Cell* params)
{
  int64_t i = *(int64_t*)params;
  return formatIntToCell(i, 10);   // keep sign ext if any
}



// Str Sys.longHexStr(long)
Cell sys_Sys_longHexStr(SedonaVM* vm, Cell* params)
{
  int64_t i = *(int64_t*)params;
  return formatIntToCell(i, 16);
}


// We don't do scientific notation, just fixed point for printing.  SMALLEST consts specify
// how many digits after the decimal point get printed.

// Must match in precision!  (Could calculate reciprocal at runtime instead, but code
// is more efficient if we make them both constants.)
#define SMALLEST_PRINTABLE_DOUBLE (1.0e-6)
#define SMALLEST_DOUBLE_RECIP     (1.0e6)

// Str Sys.doubleStr(double)
Cell sys_Sys_doubleStr(SedonaVM* vm, Cell* params)
{
  double val = *(double*)params;
  double abs;
  int n = 0;
  //int32_t i, d, j;
  int64_t i;
  int32_t d, j;
  Cell result;
  result.aval = strbuf;      // strbuf location doesn't change, just its contents

  // Handle trivial case first
  if (val==0.0)
  {
    strbuf[n++] = '0';
    strbuf[n++] = '.';
    // Use correct # of 0's for our precision
    for (d=SMALLEST_DOUBLE_RECIP; d>1; d/=10) strbuf[n++] = '0';
    strbuf[n++] = '\0';
    return result;
  }

  // get absolute value
  abs = val < 0 ? -val : val;

  // if not printable then print hex bit sequence
  if (abs != 0.0 && abs < SMALLEST_PRINTABLE_DOUBLE)
  {
    strbuf[n++] = '0';
    strbuf[n++] = 'x';
    n = formatIntPart(params[0].ival, 16, n);
    n = formatIntPart(params[1].ival, 16, n);   // can we still get away with this?
  }
  else
  {
    // negative
    if (val < 0) strbuf[n++] = '-';

    abs += (SMALLEST_PRINTABLE_DOUBLE*0.5);        // round to Nth decimal place
    i = (int64_t) abs;                             // integer part
    d = (int32_t) ((abs-i)*SMALLEST_DOUBLE_RECIP); // fractional part

    // Write the integer part and decimal point
    n = formatIntPart(i, 10, n);
    strbuf[n++] = '.';

    // Write leading zeroes, if any, for decimal part
    for (j=(int)(SMALLEST_DOUBLE_RECIP/10); j>0; j/=10)
      if (d<j) strbuf[n++] = '0';

    // If d==0 then we're done; otherwise write rest of fraction
    if (d>0)
      n = formatIntPart(d, 10, n);
  }

  strbuf[n++] = '\0';
  return result;
}


// Str Sys.floatStr(float)
Cell sys_Sys_floatStr(SedonaVM* vm, Cell* params)
{
  double dval = (double)params[0].fval;
  Cell cells[2];

  // Take two steps to get to double* in order to avoid compiler warning
  // against type-punning.
  void* vcells = (void*)cells;
  double* dcells = (double*)vcells;
  *dcells = dval;

  return sys_Sys_doubleStr(vm, cells);
}



