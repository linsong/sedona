//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Feb 09  Dan Giorgis  Creation
//

#include "sedona.h"

#include <time.h>
#include <stdio.h>

#ifdef _WIN32
extern long _timezone;
extern int _daylight;
#else
extern long timezone;
extern int daylight;
#endif

//  Difference in seconds between ANSI C Epoch of midnight Jan 1 1970 and
//  the Sedona epoch of midnight Jan 1 2000.  There were 7 leap years
//  in this timeframe - 72,76,80,84,88,92,96

#define SEDONA_EPOCH_OFFSET_SECS ((int64_t)(((365L * 30L) + 7L) * 24L * 60L * 60L))

////////////////////////////////////////////////////////////////
// Native Methods
////////////////////////////////////////////////////////////////

// long doNow()
int64_t datetimeStd_DateTimeServiceStd_doNow(SedonaVM* vm, Cell* params)
{
  time_t now = time(NULL);
  int64_t nanos;
  now -= SEDONA_EPOCH_OFFSET_SECS;
  nanos = ((int64_t) now * 1000 * 1000 *1000);
  return nanos;
}

 //  Setting system time not implemented on Win32
  
// void doSetClock 
Cell datetimeStd_DateTimeServiceStd_doSetClock(SedonaVM* vm, Cell* params)
{
  int64_t nanos = *(int64_t*)(params+0); // param 0+1
    
  return nullCell;
}

Cell datetimeStd_DateTimeServiceStd_doGetUtcOffset(SedonaVM* vm, Cell* params)
{
  Cell result;
   
  //  Per Microsoft CRT docs, timezone global variable is updated whenever 
  //  localtime is called.  QNX follows same convention.
  //  timezone is difference between localtime and GMT, so we must negate to 
  //  get utcOffset as we define it.
  time_t now;
  struct tm *lcltime;

  tzset();   // set up time zone related variables before calling localtime()
  
  now     = time(NULL);
  lcltime = localtime(&now);

  #ifdef _WIN32
    result.ival = -(int)_timezone;
    if (_daylight)
      result.ival += 3600;  //  if daylight savings active, advance one hour
  #else
    result.ival = -(int)timezone;
    if (daylight)
      result.ival += 3600;  //  if daylight savings active, advance one hour
  #endif

//printf("timezone %lld result.ival %d\n", _timezone, result.ival);

  return result;
  
}  
