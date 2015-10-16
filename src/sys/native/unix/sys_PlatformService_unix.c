//
// Copyright 2012 Tridium, Inc. All Rights Reserved.
// Licensed under the Academic Free License version 3.0
//
// History:
//    2012 Apr 10   Elizabeth McKenney   Adapted from win32 
//

#include "sedona.h"
#include "sedonaPlatform.h"

#ifdef __DARWIN__
  #include <sys/sysctl.h>
  #include <mach/host_info.h>
  #include <mach/mach_host.h>
#else
  #include <sys/sysinfo.h>
#endif


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
#ifdef __DARWIN__
  int mib[6];
  mib[0] = CTL_HW;
  mib[1] = HW_PAGESIZE;

  int pagesize;
  size_t length;
  length = sizeof (pagesize);
  if (sysctl (mib, 2, &pagesize, &length, NULL, 0) < 0)
    return 0; // failed to get pagesize

  mach_msg_type_number_t count = HOST_VM_INFO_COUNT;
  vm_statistics_data_t vmstat;
  if (host_statistics (mach_host_self (), HOST_VM_INFO, (host_info_t) &vmstat, &count) != KERN_SUCCESS)
    return 0; // failed to get VM statistics

  return vmstat.free_count * pagesize;
#else
  struct sysinfo info;
  sysinfo(&info);
  return info.freeram * (long)info.mem_unit;     // this may be Linux-specific
#endif
}


