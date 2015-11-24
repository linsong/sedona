//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 May 07  Brian Frank  Creation
//

#include "sedona.h"

// int Test.doMain(Obj testToRun)
Cell sys_Test_doMain(SedonaVM* vm, Cell* params)
{
  const char* testName = params[0].aval;
  uint8_t* cb          = (uint8_t*)vm->codeBaseAddr;
  uint16_t testsBix    = *(uint16_t*)(cb+18);
  uint16_t* tests;
  int numTests;
  uint16_t method;
  int i, currAsserts = 0;
  int index = strlen(testName);
  const char* qname;
  Cell ret;
#ifdef SCODE_DEBUG
  if ((strcmp(testName, "") == 0))
    printf("-- Running all svm tests...\n");
  else
    printf("-- Running svm test: %s\n", testName);
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
    qname  = qnameSlot(vm, tests[i*2]);
    currAsserts = vm->assertSuccesses;
    if ( (index > 0) && !strstr(qname, testName) ) continue;
#ifdef SCODE_DEBUG
    printf("-- svm test %s", qname);
#endif
    method = tests[i*2+1];
    vm->call(vm, method, NULL, 0);
#ifdef SCODE_DEBUG
    printf(" [%i verifies]\n", vm->assertSuccesses - currAsserts);
#endif
  }

  // return number of failures
  ret.ival = vm->assertFailures;
  return ret;
}
