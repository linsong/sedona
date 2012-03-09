//
// This code licensed to public domain
//
// History:
//   27 Apr 05  Brian Frank  Creation
//

package sedona.util;

import java.text.*;
import java.util.*;

/**
 * Abstime models an absolute point in time modeled as millis since
 * the epoch 1 Jan 1970.  It also provides access to time of day
 * components relative to a specified time zone: year, month, day,
 * hour, min, and seconds.
 */
public class Abstime
  implements Comparable
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Construct for current time and default time zone.
   */
  public static Abstime now()
  {
    return new Abstime(System.currentTimeMillis(), defaultTimeZone);
  }

  /**
   * Construct for specified millis and default time zone.
   */
  public static Abstime make(long millis)
  {
    return new Abstime(millis, defaultTimeZone);
  }

  /**
   * Construct for specified millis and time zone.
   */
  public static Abstime make(long millis, TimeZone timeZone)
  {
    return new Abstime(millis, timeZone);
  }

  /**
   * Parse from string.
   */
  public static Abstime parse(String s)
  {
    Abstime a = new Abstime(0, defaultTimeZone);
    a.decode(s);
    return a;
  }

  /**
   * Private constructor.
   */
  private Abstime(long millis, TimeZone timeZone)
  {
    this.millis   = millis;
    this.timeZone = timeZone;
    this.bits0    = bits1 = 0;
  }

////////////////////////////////////////////////////////////////
// Get Functions
////////////////////////////////////////////////////////////////

  /**
   * @return millis since the epoch relative to UTC.  This
   *    result is independent of this AbsTime's time zone.
   */
  public long getMillis()
  {
    return millis;
  }

  /**
   * The year as a four digit integer (ie 2001).
   */
  public final int getYear()
  {
    if (bits0 == 0) millisToFields();
    return (bits0 >> 16) & 0xFFFF;
  }

  /**
   * The month: 1-12
   */
  public final int getMonth()
  {
    if (bits0 == 0) millisToFields();
    return (bits1 >> 25) & 0x0F;
  }

  /**
   * The day: 1-31.
   */
  public final int getDay()
  {
    if (bits0 == 0) millisToFields();
    return (bits1 >> 20) & 0x1F;
  }

  /**
   * The hour: 0-23.
   */
  public final int getHour()
  {
    if (bits0 == 0) millisToFields();
    return (bits1 >> 15) & 0x1F;
  }

  /**
   * The minute: 0-59.
   */
  public final int getMinute()
  {
    if (bits0 == 0) millisToFields();
    return (bits1 >> 9) & 0x3F;
  }

  /**
   * The seconds: 0-59.
   */
  public final int getSecond()
  {
    if (bits0 == 0) millisToFields();
    return (bits1 >> 3) & 0x3F;
  }

  /**
   * The milliseconds: 0-999.
   */
  public final int getMillisecond()
  {
    if (bits0 == 0) millisToFields();
    return bits0 & 0xFFFF;
  }

  /**
   * The weekday: 0-6
   */
  public final int getWeekday()
  {
    if (bits0 == 0) millisToFields();
    return bits1 & 0x07;
  }

  /**
   * Get the number of milliseconds into the day
   * for this Abstime.  An example is that 1:00 AM
   * would return 3600000.
   */
  public final long getTimeOfDayMillis()
  {
    return getHour()   * 60*60*1000L +
           getMinute() * 60*1000L +
           getSecond() * 1000L +
           getMillisecond();
  }

  /**
   * Return a nice human formatted String.
   */
  public String toString()
  {
    if (millis == 0) return "null";
    return format.format(new Date(millis));
  }
  private DateFormat format = new SimpleDateFormat("HH:mm:ss dd-MMM-yy z");

////////////////////////////////////////////////////////////////
// TimeZone
////////////////////////////////////////////////////////////////

  /**
   * Get timezone used to compute relative fields such
   * as year, month, day, hour, and minutes.  The time zone
   * never has any bearing on getMillis().
   */
  public final TimeZone getTimeZone()
  {
    return timeZone;
  }

  /**
   * Return the offset in millis from GMT taking daylight
   * savings time into account if appropriate.
   */
  public int getTimeZoneOffset()
  {
    if (!inDaylightTime())
      return timeZone.getRawOffset();
    GregorianCalendar cal = new GregorianCalendar(timeZone);
    cal.setTime(new Date(millis));
    return cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
  }

  /**
   * Does this time fall in daylight savings time
   * based on the current TimeZone.
   */
  public boolean inDaylightTime()
  {
    if (bits0 == 0) millisToFields();
    return ((bits1 >> 29) & 0x01) != 0;
  }

  /**
   * Convert this instance to an equivalent instance in the
   * current VM's local time zone.
   */
  public Abstime toLocalTime()
  {
    if (timeZone.equals(defaultTimeZone))
      return this;
    else
      return new Abstime(this.millis, defaultTimeZone);
  }

  /**
   * Convert this instance to an equivalent instance in UTC.
   */
  public Abstime toUtcTime()
  {
    if (timeZone.equals(utcTimeZone))
      return this;
    else
      return new Abstime(this.millis, utcTimeZone);
  }

////////////////////////////////////////////////////////////////
// Comparsion
////////////////////////////////////////////////////////////////

  /**
   * Compare to another Abstime.
   * @return a negative integer, zero, or a
   *    positive integer as this object is less
   *    than, equal to, or greater than the
   *    specified object.
   */
  public int compareTo(Object that)
  {
    Abstime t = (Abstime)that;
    if (this.millis < t.millis) return -1;
    else if (this.millis == t.millis) return 0;
    else return 1;
  }
  
  public boolean equals(Object that)
  {
    if (that instanceof Abstime)
      return ((Abstime)that).millis == this.millis;
    return false;
  }

  /**
   * Return true if the specified time is before this time.
   */
  public boolean isBefore(Abstime x)
  {
    return compareTo(x) < 0;
  }

  /**
   * Return true if the specified time is after this time.
   */
  public boolean isAfter(Abstime x)
  {
    return compareTo(x) > 0;
  }

  /**
   * Abstime hash code is based on the
   * the absolute time in millis.
   */
  public int hashCode()
  {
   return (int)(millis ^ (millis >> 32));
  }

  /**
   * Is the date of the specified instance equal to the date of this instance?
   */
  public boolean dateEquals(Abstime that)
  {
    return (that.getYear() == getYear()) &&
           (that.getMonth() == getMonth()) &&
           (that.getDay() == getDay());
  }

  /**
   * Is the time of the specified instance equal to the date of this instance?
   */
  public boolean timeEquals(Abstime that)
  {
    return that.getTimeOfDayMillis() == getTimeOfDayMillis();
  }

////////////////////////////////////////////////////////////////
// Algebra
////////////////////////////////////////////////////////////////

  /**
   * Add a relative time to this time and return
   * the new instant in time.
  public Abstime add(long millis)
  {
    return make(this.millis+millis, timeZone);
  }
   */

  /**
   * Subtract a relative time from this time and
   * return the new instant in time.
  public Abstime subtract(long millis)
  {
    return make(this.millis-millis, timeZone);
  }
   */

  /**
   * Compute the time difference between this time and the specified time.  If
   * t2 is after this time, the result will be positive.  If t2 is before
   * this time, the result will be negative.
   *
   * @param t2 The time to compare against.
  public long delta(Abstime t2)
  {
    return t2.millis - millis;
  }
   */

  /**
   * Create a new instance on the same date as this instance
   * but with a different time.
  public Abstime timeOfDay(int hour, int min, int sec, int millis)
  {
    return new Abstime(getYear(), getMonth(), getDay(), hour, min, sec, millis, timeZone);
  }
   */

  /**
   * The same time on the next day.
  public Abstime nextDay()
  {
    int year  = getYear();
    int month = getMonth();
    int day   = getDay();

    if (day == getDaysInMonth(year,month))
    {
      day = 1;
      if (month == 12)
      {
        month = 1;
        year++;
      }
      else
      {
        month++;
      }
    }
    else
    {
      day++;
    }
    return new Abstime(year, month, day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * The same time on the previous day.
  public Abstime prevDay()
  {
    int year  = getYear();
    int month = getMonth();
    int day   = getDay();

    if (day == 1)
    {
      if (month == 1)
      {
        month = 12;
        year--;
      }
      else
      {
        month--;
      }
      day = getDaysInMonth(year,month);
    }
    else
    {
      day--;
    }
    return new Abstime(year, month, day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * The same day and time in the next month.  If
   * this day is greater than the last day in the
   * next month, then cap the day to the next month's
   * last day.  If this time's day is the last day
   * in this month, then we automatically set the
   * month to the next month's last day.
  public Abstime nextMonth()
  {
    int year  = getYear();
    int month = getMonth();
    int day   = getDay();

    if (month == 12)
    {
      // no need to worry about day capping
      // because both Dec and Jan have 31 days
      month = 1;
      year++;
    }
    else
    {
      if (day == getDaysInMonth(year, month))
      {
        month++;
        day = getDaysInMonth(year, month);
      }
      else
      {
        month++;
        if (day > getDaysInMonth(year, month))
          day = getDaysInMonth(year, month);
      }
    }

    return new Abstime(year, month, day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * The same time and day in previous month. If
   * this day is greater than the last day in the
   * prev month, then cap the day to the prev month's
   * last day.  If this time's day is the last day
   * in this month, then we automatically set the
   * month to the prev month's last day.
  public Abstime prevMonth()
  {
    int year  = getYear();
    int month = getMonth();
    int day   = getDay();

    if (month == 1)
    {
      // no need to worry about day capping
      // because both Dec and Jan have 31 days
      month = 12;
      year--;
    }
    else
    {
      if (day == getDaysInMonth(year, month))
      {
        month--;
        day = getDaysInMonth(year, month);
      }
      else
      {
        month--;
        if (day > getDaysInMonth(year, month))
          day = getDaysInMonth(year, month);
      }
    }

    return new Abstime(year, month, day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * Get the same time and day in next year.  If today
   * is a leap day, then return next year Feb 28.
  public Abstime nextYear()
  {
    int day = getDay();
    if (isLeapDay()) day = 28;
    return new Abstime(getYear()+1, getMonth(), day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * Get the same time and day in prev year.  If today
   * is a leap day, then return prev year Feb 28.
  public Abstime prevYear()
  {
    int day = getDay();
    if (isLeapDay()) day = 28;
    return new Abstime(getYear()-1, getMonth(), day, getHour(), getMinute(), getSecond(), getMillisecond(), timeZone);
  }
   */

  /**
   * Get the next day of the specified weekday. If
   * today is the specified weekday, then return one
   * week from now.
  public Abstime nextWeekday(int weekday)
  {
    Abstime t = nextDay();
    while(t.getWeekday() != weekday)
      t = t.nextDay();
    return t;
  }
   */

  /**
   * Get the prev day of the specified weekday. If
   * today is the specified weekday, then return one
   * week before now.
  public Abstime prevWeekday(int weekday)
  {
    Abstime t = prevDay();
    while(t.getWeekday() != weekday)
      t = t.prevDay();
    return t;
  }
   */

////////////////////////////////////////////////////////////////
// Leap Years
////////////////////////////////////////////////////////////////

  /**
   * Return if today is Feb 29.
   */
  public boolean isLeapDay()
  {
    return (getMonth() == 2) && (getDay() == 29);
  }

  /**
   * Return if the specified year (as a four digit
   * number) is a leap year.
   */
  public static boolean isLeapYear(int year)
  {
    if (year >= 1582)
    {
      // Gregorian
      return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }
    else
    {
      // Julian
      return (year % 4 == 0);
    }
  }

  /**
   * Given a year and month (1-12), return the number of days
   * in that month taking into consideration leap years.
   */
  public static int getDaysInMonth(int year, int month)
  {
    checkMonth(month);
    if (month == 2)
      return isLeapYear(year) ? 29 : 28;
    else
      return daysInMonth[month-1];
  }

  /**
   * Given a year, return the number of days in that
   * year taking into consideration leap years.
   */
  public static int getDaysInYear(int year)
  {
    return isLeapYear(year) ? 366 : 365;
  }

////////////////////////////////////////////////////////////////
// String Encoding
////////////////////////////////////////////////////////////////

  /**
   * Encode the value as a string in ISO 8601
   */
  public String encode()
  {
    StringBuffer s = new StringBuffer(32);

    s.append( getYear() ).append('-');

    int month = getMonth();
    if (month < 10) s.append('0');
    s.append( month ).append( '-' );

    int day = getDay();
    if (day < 10) s.append('0');
    s.append( day ).append( 'T' );

    int hour = getHour();
    if (hour < 10) s.append('0');
    s.append( hour ).append( ':' );

    int min = getMinute();
    if (min < 10) s.append('0');
    s.append( min ).append( ':' );

    int sec = getSecond();
    if (sec < 10) s.append('0');
    s.append( sec ).append( '.' );

    int millis = getMillisecond();
    if (millis < 10) s.append('0');
    if (millis < 100) s.append('0');
    s.append( millis );

    int offset = getTimeZoneOffset();
    if (offset == 0)
    {
      s.append('Z');
    }
    else
    {
      int hrOff = Math.abs(offset / (1000*60*60));
      int minOff = Math.abs((offset % (1000*60*60)) / (1000*60));

      if (offset < 0) s.append('-');
      else s.append('+');

      if (hrOff < 10) s.append('0');
      s.append(hrOff);

      s.append(':');
      if (minOff < 10) s.append('0');
      s.append(minOff);
    }

    return s.toString();
  }

  /**
   * Decode the value from a string.
   */
  private void decode(String val)
    throws RuntimeException
  {
    char[] c = val.toCharArray();
    try
    {
      int i = 0;

      int year = (int)(c[i++] - '0') * 1000 +
                 (int)(c[i++] - '0') * 100 +
                 (int)(c[i++] - '0') * 10 +
                 (int)(c[i++] - '0') * 1;

      if (c[i++] != '-') throw new Exception();

      int mon = (int)(c[i++] - '0') * 10 +
                (int)(c[i++] - '0') * 1;

      if (c[i++] != '-') throw new Exception();

      int day = (int)(c[i++] - '0') * 10 +
                (int)(c[i++] - '0') * 1;

      if (c[i++] != 'T') throw new Exception();

      int hour = (int)(c[i++] - '0') * 10 +
                 (int)(c[i++] - '0') * 1;

      if (c[i++] != ':') throw new Exception();

      int min = (int)(c[i++] - '0') * 10 +
                (int)(c[i++] - '0') * 1;

      if (c[i++] != ':') throw new Exception();

      int sec = (int)(c[i++] - '0') * 10 +
                (int)(c[i++] - '0') * 1;

      int ms = 0;
      if (c[i] == '.')
      {
        i++;
        ms = (c[i++] - '0') * 100;
        if ('0' <= c[i] && c[i] <= '9') ms += (c[i++] - '0') * 10;
        if ('0' <= c[i] && c[i] <= '9') ms += (c[i++] - '0') * 1;

        // skip any additional fractional digits
        while(i < c.length && '0' <= c[i]  && c[i] <= '9') i++;
      }

      // timezone offset sign
      int tzOff = 0;
      char sign = c[i++];
      if (sign != 'Z')
      {
        if (sign != '+' && sign != '-')
          throw new Exception();

        // timezone hours
        int hrOff = (int)(c[i++] - '0');
        if (i < c.length && c[i] != ':')
          hrOff = hrOff*10 + (int)(c[i++] - '0');

        // timezone minutes
        int minOff = 0;
        if (i < c.length)
        {
          if (c[i++] != ':') throw new Exception();
          minOff = 10*(int)(c[i++] - '0') + (int)(c[i++] - '0');
        }

        tzOff = hrOff*(60*60*1000) + minOff*(60*1000);
        if (sign == '-') tzOff *= -1;
      }

      Calendar cal = new GregorianCalendar(year, mon-1, day, hour, min, sec);
      cal.set(Calendar.MILLISECOND, ms);
      cal.setTimeZone(new SimpleTimeZone(tzOff, "Offset"));

      // save
      this.millis   = cal.getTime().getTime();
      this.timeZone = timeZone;
      this.bits0    = bits1 = 0;
    }
    catch(Exception e)
    {
      throw new RuntimeException("Invalid abstime: " + val);
    }
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  private static long toMillis(int year, int month, int day, int hour, int min, int sec, int millis, TimeZone timeZone)
  {
    checkMonth(month);
    Calendar c = new GregorianCalendar(timeZone);
    c.set(year, month-1, day, hour, min, sec);
    c.set( Calendar.MILLISECOND, millis );
    return c.getTime().getTime();
  }

  private static int checkMonth(int month)
  {
    if (month < 1 || month > 12) throw new IllegalArgumentException("Month must be 1 to 12");
    return month;
  }

////////////////////////////////////////////////////////////////
// Millis To Fields
////////////////////////////////////////////////////////////////

  /**
   * Map millis and timeZone to its component fields.
   *
   * Bits0:
   *  ------------------------------------------------
   *  Field    Num Bits  Range    Loc
   *  ------------------------------------------------
   *  Year       16      short    16-31
   *  Millis     16      short    0-15
   *
   * Bits1:
   *  ------------------------------------------------
   *  Field    Num Bits  Range    Loc
   *  ------------------------------------------------
   *  Daylight    1       0-1     29-29
   *  Month       4       1-12    25-28
   *  Day         5       1-31    20-24
   *  Hour        5       0-23    15-19
   *  Minutes     6       0-59    9-14
   *  Seconds     6       0-59    3-8
   *  Weekday     3       0-6     0-2
   * ------------------------------------------------
   */
  private void millisToFields()
  {
    // init a calendar with timeZone and millis
    Calendar calendar = new GregorianCalendar(timeZone);
    Date date = new Date(millis);
    calendar.setTime(date);

    // set year bits
    int x = calendar.get(Calendar.YEAR);
    bits0 |= ((x & 0xFFFF) << 16);

    // set millisecond bits
    x = calendar.get(Calendar.MILLISECOND);
    bits0 |= ((x & 0xFFFF) << 0);

    // set month bits
    x = calendar.get(Calendar.MONTH) + 1;
    bits1 |= ((x & 0x0F) << 25);

    // set day bits
    x = calendar.get(Calendar.DAY_OF_MONTH);
    bits1 |= ((x & 0x1F) << 20);

    // set hour bits
    x = calendar.get(Calendar.HOUR_OF_DAY);
    bits1 |= ((x & 0x1F) << 15);

    // set minute bits
    x = calendar.get(Calendar.MINUTE);
    bits1 |= ((x & 0x3F) << 9);

    // set seconds bits
    x = calendar.get(Calendar.SECOND);
    bits1 |= ((x & 0x3F) << 3);

    // set weekday
    x = calendar.get(Calendar.DAY_OF_WEEK) - 1;
    bits1 |= ((x & 0x07) << 0);

    // set daylight bit
    if (timeZone.inDaylightTime(date))
      bits1 |= (0x01 << 29);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  private static final int[] daysInMonth =
    { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

  static final TimeZone defaultTimeZone = TimeZone.getDefault();
  static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");

  private long millis;
  private int bits0, bits1;
  private TimeZone timeZone;

}
