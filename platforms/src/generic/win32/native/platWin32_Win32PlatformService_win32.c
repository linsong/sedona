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

extern int64_t yieldNs;
extern unsigned int pmSize;
extern uint8_t pmImage[];

// void Win32YieldPlatformService.doYield()
Cell platWin32_Win32YieldPlatformService_doYield(SedonaVM* vm, Cell* params)
{
  yieldNs = *(int64_t*)params;
  return nullCell;
}

// int PlatformManifestServer_getPmSize()
Cell platWin32_PlatformManifestServer_getPmSize(SedonaVM* vm, Cell* params)
{
  Cell result;
  result.ival = pmSize;
  return result;
}

// byte[] PlatformManifestServer_getPmBytes()
Cell platWin32_PlatformManifestServer_getPmBytes(SedonaVM* vm, Cell* params)
{
  Cell result;
  result.aval = pmImage;
  return result;
}
