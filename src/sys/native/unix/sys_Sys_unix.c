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
  result.aval = "sys::Platform";
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
uint64_t rollover_count = 0;
uint64_t last_cycles = 0;
uint64_t cycles_per_sec = 0;

// static long Sys.ticks()
int64_t sys_Sys_ticks(SedonaVM* vm, Cell* params)
{
  int64_t nanos;

  pthread_mutex_lock(&mutex);

  if (0 == cycles_per_sec)
    cycles_per_sec = sysconf(_SC_CLK_TCK);

  struct tms tmsbuf;
  clock_t cycles = times(&tmsbuf);

  if (cycles < last_cycles)
    ++rollover_count;

  nanos = (1000LL * 1000LL * 1000LL * cycles) / cycles_per_sec;
  nanos += rollover_count * (((int64_t)CLOCK_T_MAX * 1000LL * 1000LL * 1000LL) / cycles_per_sec);

  last_cycles = cycles;

  pthread_mutex_unlock(&mutex);

  return nanos;
}


