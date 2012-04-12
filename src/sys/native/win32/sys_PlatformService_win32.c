//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   29 Mar 12  Elizabeth McKenney  Creation
//

#include "sedona.h"
#include "sedonaPlatform.h"

#include <windows.h>


// Str PlatformService.doPlatformId()
Cell sys_PlatformService_doPlatformId(SedonaVM* vm, Cell* params)
{         
  Cell result;
  result.aval = PLATFORM_ID;
  return result;
}


// Str PlatformService.getPlatVersion()
Cell sys_PlatformService_getPlatVersion(SedonaVM* vm, Cell* params)
{         
  Cell result;
  result.aval = PLAT_BUILD_VERSION;  
  return result;
}


// long PlatformService.getNativeMemAvailable()
int64_t sys_PlatformService_getNativeMemAvailable(SedonaVM* vm, Cell* params)
{
  int64_t totalmem;
  MEMORYSTATUSEX memstatus;

  memstatus.dwLength = sizeof(MEMORYSTATUSEX);    // set dwLength before calling GlobalMemory fn
  GlobalMemoryStatusEx( &memstatus );

  // Use MB resolution but report bytes (to reduce event freq)
  totalmem = memstatus.ullAvailPhys >> 20;
  totalmem <<= 20;

  // If mem exceeds max (positive) long value, just return max
  if (totalmem<0) 
    return 0x7fffffffffffffff;
  return totalmem;
}


