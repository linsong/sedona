//
// Copyright 2007 Tridium, Inc. All Rights Reserved.
//
// History:
//   5 Dec 07  Dan Giorgis  Creation
//

#include "sedona.h"
#include "sedonaPlatform.h"

// Str QnxPlatform.doPlatformId()
Cell platQnxPpc_QnxPlatformService_doPlatformId(SedonaVM* vm, Cell* params)
{         
  Cell result;

  result.aval = PLATFORM_ID;
  return result;
}                      
