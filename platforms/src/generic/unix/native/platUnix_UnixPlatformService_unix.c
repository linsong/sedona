//
// Copyright 2009 Tridium, Inc. All Rights Reserved.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12/3/2009 Matthew Giannini - creation
//

#include "sedona.h"
#include "sedonaPlatform.h"

// Str UnixPlatformService.doPlatformId()
Cell platUnix_UnixPlatformService_doPlatformId(SedonaVM* vm, Cell* params)
{         
  Cell result;
  result.aval = PLATFORM_ID;
  return result;
}                      
