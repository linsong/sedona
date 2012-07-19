//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank        Creation
//    5 Mar 09  Elizabeth McKenney Added options for timestamp & prefix
//   16 Jun 09  Brian Frank        Refactor to use cleaner design
//

package sedona.util;

import java.io.*;
import java.util.Calendar;

/**
 * Log is responsible for outputting messages
 */
public class Log
{

  public Log(String name)
  {                
    this(name, System.out);
  }                    
                   
  public Log(String name, PrintStream out)
  {
    this.name = name;
    this.out  = out;
  }                    

  public final String name;         
  
  public PrintStream out;

  public int severity = INFO;               
  
  public static String severityToString(int severity)
  {
    switch (severity)
    {
      case ERROR: return "error";
      case WARN:  return "warn";
      case INFO:  return "info";
      case DEBUG: return "debug";
      default:    return "Unknown(" + severity + ")";
    }
  }
  
  public static final int ERROR   = 4;
  public static final int WARN    = 3;
  public static final int INFO    = 2;
  public static final int DEBUG   = 1;                   
  public static final int SILENT  = 0;    
  
  public boolean isError() { return severity >= ERROR; }
  public boolean isWarn()  { return severity >= WARN; }
  public boolean isInfo()  { return severity >= INFO; }
  public boolean isDebug() { return severity >= DEBUG; }
  
  public void error(String msg) { log(ERROR, msg, null); }
  public void error(String msg, Throwable ex) { log(ERROR, msg, ex); }

  public void warn(String msg) { log(WARN, msg, null); }
  public void warn(String msg, Throwable ex) { log(WARN, msg, ex); }

  public void info(String msg) { log(INFO, msg, null); }
  public void info(String msg, Throwable ex) { log(INFO, msg, ex); }
  
  public void debug(String msg) { log(DEBUG, msg, null); }
  public void debug(String msg, Throwable ex) { log(DEBUG, msg, ex); }
  
  public void log(int severity, String msg, Throwable ex)
  {                                      
    if (severity < this.severity) return;
    out.println("[" + Abstime.now() + "] [" + severityToString(severity) + "] [" + name + "] " + msg);
    if (ex != null) ex.printStackTrace(out);
  }

}
