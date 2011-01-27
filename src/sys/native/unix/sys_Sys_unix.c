//
// Copyright (c) 2008Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
// 	5 Dec 08  Matthew Giannini Creation
//

#include <sys/time.h>
#include <sys/times.h>
#include <pthread.h>
#include "sedona.h"

// static Str sys_Sys_platformType()
Cell sys_Sys_platformType(SedonaVM* vm, Cell* params)
{
  Cell result;

#ifdef PLATFORM_TYPE
  result.aval = PLATFORM_TYPE;
#else
  result.aval = "sys::PlatformService";
#endif

  return result;
}

// static void Sys.sleep(Time t)
Cell sys_Sys_sleep(SedonaVM* vm, Cell* params)
{
  int64_t ns = *(int64_t*)params;
  struct timespec ts;

  if (ns <= 0) return nullCell;

  if (ns < 1000LL * 1000LL * 1000LL)
  {
    ts.tv_sec = 0;
    ts.tv_nsec = ns;
  }
  else
  {
    ts.tv_sec = ns / (1000LL * 1000LL * 1000LL);
    ts.tv_nsec = ns - (ts.tv_sec * 1000LL * 1000LL * 1000LL);
  }

  nanosleep(&ts, NULL);
  return nullCell;
}

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
uint64_t last_cycles    = 0;
uint64_t cycles_total   = -1;
uint64_t cycles_per_sec = 0;

// static long Sys.ticks()
int64_t sys_Sys_ticks(SedonaVM* vm, Cell* params)
{
  int64_t ticks;
  uint64_t cycles = 0;
  uint64_t cycles_delta = 0;
  struct tms tmsbuf;

  pthread_mutex_lock(&mutex);
  
  // first time only
  if (cycles_per_sec == 0)
  {
    cycles_total = 0;
    cycles_per_sec = sysconf(_SC_CLK_TCK);
  }

  cycles = times(&tmsbuf);
  if (sizeof(clock_t) == 4)
    cycles &= 0x0FFFFFFFF;

  if (cycles >= last_cycles)
    cycles_delta = cycles - last_cycles;
  else
  {
    // by how much did we rollover?
    if (sizeof(clock_t) == 4)
      cycles_delta = 0x100000000LL - last_cycles;
    else
      cycles_delta = UINT64_MAX - last_cycles + 1;
    
    // how far above 0 now? 
    cycles_delta += cycles;
  }

  cycles_total += cycles_delta;
  ticks = (((int64_t)(1000L*1000L*1000L*cycles_total)) / cycles_per_sec);
  last_cycles = cycles;

  pthread_mutex_unlock(&mutex);
  
  return ticks;
}


