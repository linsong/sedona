//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   8 May 07  Brian Frank  Creation
//

#include "sedona.h"
#include <time.h>
#include <pthread.h>
#include <sys/syspage.h>
#include <sys/neutrino.h>
#include <inttypes.h>

// static Str Sys.platformType()
Cell sys_Sys_platformType(SedonaVM* vm, Cell* params)
{
  Cell result;
  result.aval = "platJaceQnxPpc::JaceQnxPpcPlatformService";
  return result;
}

// static void Sys.sleep(Time t)
Cell sys_Sys_sleep(SedonaVM* vm, Cell* params)
{
  int64_t ns = *(int64_t*)params;
  struct timespec ts;
  if (ns < 1000 * 1000 * 1000)
  {
    ts.tv_sec = 0;
    ts.tv_nsec = ns;
  }
  else
  {
    ts.tv_sec = ns  / (1000 * 1000 * 1000);
    ts.tv_nsec = ns - (ts.tv_sec * 1000 * 1000 * 1000);
  }

  nanosleep( &ts, NULL);

  return nullCell;
}


uint64_t last_cycles = 0;
uint64_t rollover_count = 0;
uint64_t cycles_per_usec = 0;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

int64_t sys_Sys_ticks(SedonaVM* vm, Cell* params)
{
  uint64_t cycles;
  int64_t nanos;

  pthread_mutex_lock(&mutex);

  if (cycles_per_usec == 0)
    cycles_per_usec = (SYSPAGE_ENTRY(qtime)->cycles_per_sec) / (1000 * 1000);

  cycles = ClockCycles();

  if (cycles < last_cycles)
    rollover_count++;

  //  To avoid overflow and rounding problems, convert to usec, then
  //  multiply by 1000 to get nanos.
  nanos = (cycles / cycles_per_usec) * 1000 +
          rollover_count * (UINT64_MAX / cycles_per_usec) * 1000;

  last_cycles = cycles;

  pthread_mutex_unlock(&mutex);

  return nanos;
}
