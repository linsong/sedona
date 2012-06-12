//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
package sedona.util.sedonadev;

import sedona.Env;
import sedona.KitPart;
import sedona.manifest.KitManifest;
import sedona.util.Log;
import sedona.util.TextUtil;
import sedona.xml.XParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipInputStream;

/**
 * Utility for downloading files from a website implementing the sedonadev.org
 * RESTful API.
 * <p/>
 * All operations in this class will check the {@code sedona.Env} to see if a
 * property called {@code sedonadev.org} is defined. If so, it assumes the
 * values for that property are a comma separated list of failover websites to
 * try when attempting to download the file. For example:
 * <p/>
 * <code>sedonadev.org=http://sedonadev.org, http://localhost</code>
 * <p/>
 * Note: No trailing '/' on the url! If the property is not defined,
 * <code>http://sedonadev.org</code> is the default.
 * <p/>
 * You must set sedona property {@code sedonadev.autodownload} to true
 * to enable auto-fetching from sedonadev.org. If not enabled (the default),
 * then all fetching operations will immediately return null.
 *
 * @author Matthew Giannini
 * @creation Jun 25, 2009
 * 
 */
public final class Download
{
  private Download()
  {  
  }
  
  /**
   * Get the kit manifest for the given kit part.
   *
   * @return the KitManifest for the given part, or {@code null} if one could
   *         not be found.
   */
  public static KitManifest fetchManifest(KitPart part) throws Exception
  {
    final String path = "/download/kit/" + part.key + ".xml";
    URLConnection conn = open(path);
    if (conn == null) return null;
    
    log.info("Downloading " + conn.getURL());
    InputStream bin = new BufferedInputStream(conn.getInputStream());
    KitManifest km = KitManifest.fromXml(XParser.make(part.key+".xml", bin).parse());
    return km;
  }
  
  /**
   * Get the PAR file with the given platform id.
   *
   * @return a ZipInputStream for the PAR file with the platform id, or {@code
   *         null} if one could not be found.
   */
  public static ZipInputStream fetchPar(final String platformId) throws Exception
  {
    final String path = "/download/platform/" + platformId + ".par";
    URLConnection conn = open(path);
    if (conn == null) return null;
    
    log.info("Downloading " + conn.getURL());
    return new ZipInputStream(new BufferedInputStream(conn.getInputStream()));
  }                       
   
  /**
   * Read a URL on the website as an in-memory string.  
   * Return null if the URL does exist on website.
   */
  public static String fetchString(final String path) throws Exception
  {
    URLConnection conn = open(path);
    Reader in = null;
    try
    {
      if (conn == null) return null;
      in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
      char[] buf = new char[1024];
      StringBuffer s = new StringBuffer();
      while (true)
      {
        int n = in.read(buf, 0, buf.length);    
        if (n < 0) break;
        s.append(buf, 0, n);
      }                                   
      return s.toString();
    }
    finally
    {
      if (in != null) in.close();
    }
  }
  
  /**
   * @return a URLConnection for the first sedonadev.org website that could be
   *         successfully opened, or {@code null} if none could be opened.
   */
  private static URLConnection open(final String path)
  {
    // must have this property set to attempt auto-download
    if (!Env.getProperty("sedonadev.autodownload", false))
      return null;

    for (int i=0; i<sites.length; ++i)
    {
      HttpURLConnection conn = null;
      try
      {
        conn = (HttpURLConnection)new URL(sites[i]+path).openConnection();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
          return conn;
        conn.disconnect();
      }
      catch (MalformedURLException e)
      {
        e.printStackTrace();
      }
      catch (IOException e)
      {
        // Try next site
      }
    }
    return null;
  }
  
  private static Log log = new Log("sedonadev.org");
  
  // Load list of sites from sedonadev.org property.
  private static String[] sites;
  static
  {
    String list = Env.getProperty("sedonadev.org", "http://sedonadev.org");
    sites = TextUtil.splitAndTrim(list, ',');
  }
  
}
