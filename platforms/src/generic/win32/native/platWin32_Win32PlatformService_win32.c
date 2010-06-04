//
// Copyright 2009 Tridium, Inc. All Rights Reserved.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12/3/2009 Matthew Giannini - creation
//

#include "sedona.h"
#include "sedonaPlatform.h"
#include <windows.h>

// Str Win32PlatformService.doPlatformId()
Cell platWin32_Win32PlatformService_doPlatformId(SedonaVM* vm, Cell* params)
{         
  Cell result;
  result.aval = PLATFORM_ID;
  return result;
}                      

extern int64_t yieldNs;

// void Win32YieldPlatformService.doYield()
Cell platWin32_Win32YieldPlatformService_doYield(SedonaVM* vm, Cell* params)
{         
  yieldNs = *(int64_t*)params;   
  return nullCell;
}                      
