//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 May 07  Brian Frank  Creation
//

package sedona.manifest;

import sedona.Depend;
import sedona.Env;
import sedona.KitPart;
import sedona.kit.KitDb;
import sedona.kit.KitFile;
import sedona.util.Log;
import sedona.util.sedonadev.Download;
import sedona.xml.XParser;
import sedona.xml.XWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ManifestDb manages the cache of kit manifests on disk under
 * the {sedona_home}\manifests directory.  Each kit gets a directory
 * and each manifest is stored in a file called "kitName-checksum.xml".
 */
public class ManifestDb
{

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static final File dir = new File(Env.home, "manifests");
  public static final Log log = new Log("manifestdb");
  private static HashMap cache = new HashMap();

//////////////////////////////////////////////////////////////////////////
// Load
//////////////////////////////////////////////////////////////////////////

  /**
   * Load the manifest for the local kit zip file
   * or return null if kit is not found.
   */
  public static KitManifest loadForLocalKit(String name)
    throws Exception
  {
    // first check cache for special "kit-local" key
    KitManifest km = (KitManifest)cache.get(toLocalKey(name));
    if (km != null) return km;

    // attempt to load from kit using -1 wildcard checksum
    return loadFromLocalKit(new Info(name, -1));
  }

  /**
   * Lookup the manifest for the specified kit part using the algorithm:
   * <ol>
   * <li>If already loaded, return cached manifest
   * <li>If found, load from <code>{home}\manifests\kit\{kitName}-{checksum}.xml</code>
   * <li>If <code>\kits\{kitName}-xxx.kit</code> has matching checksum, then copy
   *     to manifest database and load that.
   * <li>If not locally available, attempt to download from a sedonadev.org website
   *     and save it to the local manifest database.
   * <li>Return {@code null}
   * </ol>
   * @see sedona.util.sedonadev.Download
   * @see sedona.util.sedonadev.Download#fetchManifest(KitPart)
   */
  public static KitManifest load(KitPart part)
    throws Exception
  {    synchronized (cache)
    {
      // build name/checksum key
      Info info = new Info(part.name, part.checksum);

      // check local cache
      KitManifest km = (KitManifest)cache.get(info.key);
      if (km != null) return km;

      // check local manifest database
      km = loadFromDb(info);
      if (km != null) return km;

      // check manifest in local kit file
      km = loadFromLocalKit(info);
      if (km != null) return km;
      
      // check sedonadev.org websites
      km = Download.fetchManifest(part);
      if (km != null)
      {
        save(km);
        return km;
      }

      // no dice
      return null;
    }
  }

  /**
   * Attempt to load manifest from local manifest database.
   */
  private static KitManifest loadFromDb(Info info)
    throws Exception
  {
    // check if db file exists
    if (!info.file.exists()) return null;

    log.debug("ManifestDb: Load [" + info.file + "]");

    // parse into memory
    KitManifest km = new KitManifest(info.name);
    km.decodeXml(XParser.make(info.file).parse());
    if (km.checksum != info.checksum)
      throw new Exception("Mismatched checksum: " + info.file);

    // store in cache and return
    cache.put(info.key, km);
    return km;
  }

  /**
   * Attempt to load manifest from kit zip file.
   */
  private static KitManifest loadFromLocalKit(Info info)
    throws Exception
  {
    // check if kit exists, or if checksum is wildcard
    // then find the highest version available
    KitFile kitFile = info.checksum == -1 ?
      KitDb.matchBest(info.name) :
      KitDb.matchBest(Depend.makeChecksum(info.name, info.checksum));
    if (kitFile == null) return null;

    // decode manifest found in zip
    ZipFile zip = new ZipFile(kitFile.file);
    KitManifest km = new KitManifest(info.name);
    try
    {
      ZipEntry entry = zip.getEntry("manifest.xml");
      if (entry == null) throw new Exception("Missing 'manifest.xml' manifest in " + info.file);
      km.decodeXml(XParser.make(kitFile.toString(), zip.getInputStream(entry)).parse());
    }
    finally
    {
      zip.close();
    }

    // if info has -1 wildcard checksum, then
    // recreate an info we know will match
    if (info.checksum == -1)
      info = new Info(info.name, km.checksum);

    // if not a matching checksum, then bail
    if (km.checksum != info.checksum)
    {
      log.debug("Local kit not match [" + kitFile + "]");
      return null;
    }

    // we have a matching checksum
    log.debug("Local kit is match [" + kitFile + "]");

    // store in cache under explicit checksum, store as local key,
    // save back to manifest db, and return
    cache.put(info.key, km);
    cache.put(toLocalKey(info.name), km);

    // save back if not already in local db
    if (!info.file.exists()) save(km);

    return km;
  }                        
  
  
  /**
   * List the name of all the kits installed into the local manifest 
   * database.  This method just scans the file system directories, 
   * it does not attempt to open the manifests themselves.
   * @return the list of installed kits by name, or an empty array
   *         if the sedona manifests home directory does not exist.
   */
  public static String[] listInstalledKits()
  {
    if (!dir.exists()) return new String[0];
    ArrayList acc = new ArrayList();
    File[] dirs = dir.listFiles();
    for (int i=0; i<dirs.length; ++i)
      if (dirs[i].isDirectory()) acc.add(dirs[i].getName());
    return (String[])acc.toArray(new String[acc.size()]);
  }                          
  
  /**
   * Given a kit name, return all the checksum manifests installed
   * in the local manifest database.  This method just scans the file 
   * system, it does not attempt to open the manifests themselves.
   * @return the list of installed <code>KitParts</code>, or an empty array
   *         if the sedona manifests home directory does not exist.
   */
  public static KitPart[] listInstalledParts(final String kit)
  {
    File[] files = new File(dir, kit).listFiles();
    if (files == null) return new KitPart[0];
    
    ArrayList acc = new ArrayList();
    for (int i=0; i<files.length; ++i)
    {
      final String n = files[i].getName();
      if (!(n.startsWith(kit) && n.endsWith(".xml"))) continue;
      try { acc.add(KitPart.parse(n.substring(0, n.length() - 4))); } catch (Exception e) {}
    }
    return (KitPart[])acc.toArray(new KitPart[acc.size()]);
  }
    
  /**
   * Given a kit name/checksum pair, get the file on the 
   * local file system which should store the manifest XML.
   */
  public static File toFile(KitPart kit)
  {
    return new File(dir, kit.name + File.separator + kit.toString() + ".xml");
  }

//////////////////////////////////////////////////////////////////////////
// Save
//////////////////////////////////////////////////////////////////////////

  /**
   * Save a kit back to the local manifest database.  This method
   * will not raise an exception but returns true on success and
   * false on failure.
   */
  public static boolean save(KitManifest km)
  {
    Info info = new Info(km);
    try
    {
      info.file.getParentFile().mkdirs();
      XWriter out = new XWriter(info.file);
      log.debug("Save [" + info.file + "]");
      try
      {
        km.encodeXml(out);
      }
      finally
      {
        out.close();
      }
      return true;
    }
    catch (Exception e)
    {
      log.error("ManifestDb: Cannot save manifest back to disk [" + info.file + "]", e);
      return false;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Info
//////////////////////////////////////////////////////////////////////////

  static String toLocalKey(String kitName)
  {
    return kitName + "-local";
  }

  static class Info
    extends KitPart
  {
    Info(String name, int checksum)
    {
      super(name, checksum);
      this.file = new File(dir, name + File.separator + key + ".xml");
    }

    Info(KitManifest km)
    {
      this(km.name, km.checksum);
    }

    final File file;     // file in manifest db
  }

}
