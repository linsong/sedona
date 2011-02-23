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

// shared buffer
static char strbuf[32];

// Str Sys.intStr(int)
Cell sys_Sys_intStr(SedonaVM* vm, Cell* params)
{
  int32_t i = params[0].ival;
  Cell result;

  sprintf(strbuf, "%d", i);

  result.aval = strbuf;
  return result;
}

// Str Sys.hexStr(int)
Cell sys_Sys_hexStr(SedonaVM* vm, Cell* params)
{
  int32_t i = params[0].ival;
  Cell result;

  sprintf(strbuf, "%x", i);

  result.aval = strbuf;
  return result;
}

// Str Sys.longStr(int)
Cell sys_Sys_longStr(SedonaVM* vm, Cell* params)
{
  int64_t i = *(int64_t*)params;
  Cell result;

#ifdef _WIN32
  sprintf(strbuf, "%I64d", i);
#else
  sprintf(strbuf, "%lld", i);
#endif

  result.aval = strbuf;
  return result;
}

// Str Sys.longHexStr(int)
Cell sys_Sys_longHexStr(SedonaVM* vm, Cell* params)
{         
  int64_t i = *(int64_t*)params;  
  Cell result;

#ifdef _WIN32
  sprintf(strbuf, "%I64x", i);
#else
  sprintf(strbuf, "%llx", i);
#endif

  result.aval = strbuf;
  return result;
}

// Str Sys.floatStr(float)
Cell sys_Sys_floatStr(SedonaVM* vm, Cell* params)
{
  float f = params[0].fval;
  Cell result;

  sprintf(strbuf, "%f", f);

  result.aval = strbuf;
  return result;
}

// Str Sys.doubleStr(float)
Cell sys_Sys_doubleStr(SedonaVM* vm, Cell* params)
{
  double d = *(double*)params;
  Cell result;      

  sprintf(strbuf, "%lf", d);

  result.aval = strbuf;
  return result;
}

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

// int Sys.rand()
Cell sys_Sys_rand(SedonaVM* vm, Cell* params)
{
  Cell result; 
  result.ival = rand();
  return result;
}


