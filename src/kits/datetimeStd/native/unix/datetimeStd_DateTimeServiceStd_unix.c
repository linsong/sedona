//
// Copyright (c) 2019 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//

#include "sedona.h"
#include "datetimeStd_DateTimeServiceStd.h"

#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

// void doSetClock
Cell datetimeStd_DateTimeServiceStd_doSetClock(SedonaVM* vm, Cell* params)
{
  int64_t nanos = *(int64_t*)(params+0); // param 0+1
  nanos += SEDONA_EPOCH_OFFSET_SECS * 1000 * 1000 * 1000;

  struct timespec ts;
  ts.tv_sec = nanos / (1000*1000*1000);
  ts.tv_nsec = nanos % (1000*1000*1000);

  int ret;
  ret = clock_settime(CLOCK_REALTIME, &ts);
  if (ret) {
	  perror("failed to set time");
	  return nullCell;
  }
  system("hwclock -w");
  return nullCell;
}

