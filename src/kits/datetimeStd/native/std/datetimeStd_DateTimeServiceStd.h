#ifndef _DATETIMESTD_DATETIMESERVICESTD_H_
#define _DATETIMESTD_DATETIMESERVICESTD_H_


//  Difference in seconds between ANSI C Epoch of midnight Jan 1 1970 and
//  the Sedona epoch of midnight Jan 1 2000.  There were 7 leap years
//  in this timeframe - 72,76,80,84,88,92,96

#define SEDONA_EPOCH_OFFSET_SECS ((int64_t)(((365L * 30L) + 7L) * 24L * 60L * 60L))

#endif

