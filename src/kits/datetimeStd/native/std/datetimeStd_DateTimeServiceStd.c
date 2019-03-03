//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Feb 09  Dan Giorgis  Creation
//

#include "sedona.h"

#include "datetimeStd_DateTimeServiceStd.h"
#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef _WIN32
#if !defined(__MINGW32__)
extern long _timezone;
extern int _daylight;
#endif
#define DAYLIGHT _daylight
#define TIMEZONE _timezone
#else
extern long timezone;
extern int daylight;
#define DAYLIGHT daylight
#define TIMEZONE timezone
#endif

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

  result.ival = -(int)TIMEZONE;
  if (DAYLIGHT)
      result.ival += 3600;  //  if daylight savings active, advance one hour

//printf("timezone %lld result.ival %d\n", _timezone, result.ival);

  return result;
  
}  
