//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 May 07  Brian Frank  Creation
//

#include "sedona.h"

// Obj Type.malloc()
Cell sys_Type_malloc(SedonaVM* vm, Cell* params)
{
  uint8_t* self = params[0].aval;
  size_t size   = getTypeSizeof(self);
  int init      = getTypeInit(self);
  void* mem;
  Cell args[1];
  Cell ret;

  // allocate memory for instance
  mem = (void*)malloc(size);
  if (mem == NULL) return nullCell;
  memset(mem, 0, size);

  // call instance initializer method
  args[0].aval = mem;
  vm->call(vm, init, args, 1);

  // return instance pointer
  ret.aval = mem;
  return ret;
}

