//
// Copyright 2009 Tridium, Inc. All Rights Reserved.
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
