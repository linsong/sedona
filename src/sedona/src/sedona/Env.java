//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedona;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import sedona.util.*;

/**
 * Env models the Sedona environment (its the "Sys" for the sedona
 * Java runtime and compiler)
 */
public class Env
{

  public static File home;
  public static final String hostname;
  public static final String version;
  public static final String copyright = "Copyright (c) 2007-2013 Tridium, Inc.";

////////////////////////////////////////////////////////////////
// Formatting
////////////////////////////////////////////////////////////////

  /**
   * Get the current time as a String.
   */
  public static String timestamp()
  {
    return new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date());
  }

  /**
   * Format a 32-bit floating point value to a String.
   * @see java.lang.Float#toString(float)
   */
  public static String floatFormat(float f)
  {
    return java.lang.Float.toString(f);
  }                         

  /**
   * Format a 64-bit floating point value to a String.
   * @see java.lang.Double#toString(double)
   */
  public static String doubleFormat(double f)
  {
    return java.lang.Double.toString(f);
  }

  /**
   * Print version information to standard output.
   */
  public static void printVersion(String programName)
  {
    System.out.println(programName + " " + version);
    System.out.println(copyright);
    System.out.println("sedona.version = " + version);
    System.out.println("sedona.home    = " + home);
    System.out.println("java.home      = " + System.getProperty("java.home"));
    System.out.println("java.version   = " + System.getProperty("java.version"));
  }

////////////////////////////////////////////////////////////////
// Properties
////////////////////////////////////////////////////////////////

  /**
   * Get the props configured via "/lib/sedona.properties".
   */
  public static Properties getProperties()
  {                       
    return props;
  }

  /**
   * Convenience for <code>getProperty(key, null)</code>.
   */
  public static String getProperty(String key)
  {
    return getProperty(key, null);
  }

  /**
   * Get a sedona property defined in "lib/sedona.properties".
   */
  public static String getProperty(String key, String val)
  {
    return props.getProperty(key, val);
  }

  /**
   * Get a boolean property.
   */
  public static boolean getProperty(String key, boolean def)
  {           
    String s = getProperty(key);
    if (s == null) return def;
    return s.equals("true");
  }
  
  /**
   * Get a int property.
   */
  public static int getProperty(String key, int def)
  {           
    String s = getProperty(key, null);
    if (s == null) return def;
    return Integer.parseInt(s);
  }

  /**
   * Get a long property.
   */
  public static long getProperty(String key, long def)
  {           
    String s = getProperty(key, null);
    if (s == null) return def;
    return java.lang.Long.parseLong(s);
  }

  static final Properties props = new Properties();

////////////////////////////////////////////////////////////////
// Ticks
////////////////////////////////////////////////////////////////

  /**
   * Get the current time as a number of milliseconds
   * independent of wall-time changes.
   */
  public static long ticks()
  {
    try
    {
      if (ticksMethod != null)
      {
        long x = ((java.lang.Long)ticksMethod.invoke(null, null)).longValue();
        if (ticksMethodInNanos) x /= 1000000L;
        return x;
      }

      // this will seriously mess up if the computer's  clock is modified
      return System.currentTimeMillis();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new IllegalStateException();
    }
  }
  static java.lang.reflect.Method ticksMethod;
  static boolean ticksMethodInNanos;

////////////////////////////////////////////////////////////////
// Initialization
////////////////////////////////////////////////////////////////
  
  /**
   * Check that we are running version 1.4 or later.
   * If not 1.4 then print error message and return false.
   */
  public static boolean checkJavaVersion()
  {
    try
    {
      String ver = System.getProperty("java.version");
      int ub = 0;
      while (ub < ver.length())
      {
        if (Character.isDigit(ver.charAt(ub)) || ver.charAt(ub) == '.') ++ub;
        else break;
      }
      ver = ver.substring(0, ub);

      Version have = new Version(ver);
      Version need = new Version("1.4");
      if (have.compareTo(need) >= 0) return true;

      System.out.println("Sedona requires Java " + need + " or greater");
      System.out.println("java.home    = " +System.getProperty("java.home"));
      System.out.println("java.version = " + have);
      return false;
    }
    catch(Exception e)
    {
      System.out.println("WARNING: Cannot check java version");
      System.out.println("  " + e);
      return true;
    }
  }

////////////////////////////////////////////////////////////////
// Initialization
////////////////////////////////////////////////////////////////

  static
  {
    // home
    File h;
    try
    {
      String homeProp = System.getProperty("sedona.home");
      if (homeProp == null) 
      {
        String niagaraHome = System.getProperty("baja.home");
        if (niagaraHome != null)
        {       
          homeProp = new File(niagaraHome).toString() + File.separator + "sedona";
        }
        else
        {
          throw new Exception("Must set system property 'sedona.home'");
        }
      }
      h = new File(homeProp).getCanonicalFile();
      
      if (!h.exists())
        System.out.println("ERROR: system property 'sedona.home' does not exist: " + h);
      else if (!h.isDirectory())
        System.out.println("ERROR: system property 'sedona.home' is not a directory: " + h);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      h = new File(".");
    }
    home = h;

    // hostname
    String host;
    try
    {
      host = InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e)
    {
      host = "Unknown";
    }
    hostname = host;

    // ticksMethod
    try
    {
      ticksMethod = Class.forName("javax.baja.sys.Clock").getMethod("ticks", new Class[0]);
    }
    catch (Exception e)
    {
      try { ticksMethod = Class.forName("java.lang.System").getMethod("nanoTime", new Class[0]); ticksMethodInNanos = true; } catch (Exception e2) {}
    }               
    
    // properties
    File propsFile = new File(home, "lib" + File.separator + "sedona.properties");
    try
    {            
      InputStream in = new BufferedInputStream(new FileInputStream(propsFile));
      try
      {
        props.load(in); 
        Iterator it = props.keySet().iterator();
        while (it.hasNext())
        {
          String key = (String)it.next();
          String val = props.getProperty(key);
          props.put(key, val.trim());
        }        
      }
      finally
      {
        in.close();
      }
    }
    catch (Exception e)
    {
      System.out.println("WARNING: Cannot load properties file: " + propsFile);
      if (!(e instanceof FileNotFoundException))
        System.out.println("  " + e);
    }          
    
    // version            
    String ver;
    try
    {                                         
      Properties props = new Properties();
      InputStream in = Env.class.getResourceAsStream("/version.txt");      
      props.load(in);
      in.close();
      ver = props.getProperty("version");
    } 
    catch (Exception e)
    {
      System.out.println("WARNING: Cannot load version.txt from jar");
      e.printStackTrace();
      //System.out.println("  " + e);
      ver = "Unknown";
    }
    version = ver;   
  }

}
