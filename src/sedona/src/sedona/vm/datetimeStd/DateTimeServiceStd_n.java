//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 May 09  Brian Frank  Creation
//                     

package sedona.vm.datetimeStd;

import java.util.*;
import sedona.vm.*;

/**
 * datetimeStd::DateTimeServiceStd
 */
public class DateTimeServiceStd_n
{  
      
  public static long doNow()
  {
    return (System.currentTimeMillis() - epochMillis) * 1000000L;
  }
  
  public static void doSetClock(long nanos)
  {
    System.out.println("WARNING: DateTimeService.doSetClock not implemented");
  }

  public static int doGetUtcOffset()
  {                        
    return TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000;
  }
  
  // Java millis for 1 Jan 2000 UTC
  public static final long epochMillis = 946684800000L;
}

