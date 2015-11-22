//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 May 07  Brian Frank  Creation
//

#include "sedona.h"

// int Test.doMain()
Cell sys_Test_doMain(SedonaVM* vm, Cell* params)
{
  uint8_t* cb       = (uint8_t*)vm->codeBaseAddr;
  uint16_t testsBix = *(uint16_t*)(cb+18);
  uint16_t* tests;
  int numTests;
  uint16_t method;
  int i;
  Cell ret;
#ifdef SCODE_DEBUG
  const char* qname;
#endif

  // lookup tests table
  if (testsBix == 0) return negOneCell;
  tests = (uint16_t*)block2addr(cb, testsBix);

  // iterate through the tests
  numTests = tests[0];
  if (numTests <= 0) return negOneCell;
  ++tests;
  for (i=0; i<numTests; ++i)
  {
#ifdef SCODE_DEBUG
    qname  = qnameSlot(vm, tests[i*2]);
    printf("-- Test %s\n", qname);
#endif
    method = tests[i*2+1];
    vm->call(vm, method, NULL, 0);
  }

  // return number of failures
  ret.ival = vm->assertFailures;
  return ret;
}

