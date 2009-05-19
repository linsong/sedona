//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Apr 07  Brian Frank  Creation
//

#include "sedona.h"


// static bool File.rename(Str from, Str to)
Cell sys_File_rename(SedonaVM* vm, Cell* params)
{                                          
  const char* from = params[0].aval;
  const char* to   = params[1].aval;
  int r;
  
  r = rename(from, to);
  
  return r == 0 ? trueCell : falseCell;
}


