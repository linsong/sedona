//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 May 09  Elizabeth McKenney  Creation
//

#include "sedona.h"
// Uncomment this to implement native checksum feature,
// comment out to disable
//#include "nativetable.h"   // defn of NATIVE_CHECKSUM_STRING

//////////////////////////////////////////////////////////////////////////
// Native checksum accessor
//////////////////////////////////////////////////////////////////////////

// static Str PlatformService.nativeChecksum()
Cell sys_PlatformService_nativeChecksum(SedonaVM* vm, Cell* params)
{
  Cell ret; 
#ifdef NATIVE_CHECKSUM_STRING
  ret.aval = NATIVE_CHECKSUM_STRING;   // defined in nativetable.h
#else 
  ret.aval = NULL;   
#endif 
  return ret;
}

