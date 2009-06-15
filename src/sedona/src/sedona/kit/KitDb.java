//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 Nov 07  Brian Frank  Creation
//

package sedona.kit;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;
import sedona.manifest.*;

/**
 * KitDb manages the directory of kit versions on the local disk.
 * Each kit gets a directory and each kit is stored in a file 
 * called "kitName-checksum-version.kit".
 */
public class KitDb
{

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static final File dir = new File(Env.home, "kits");

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  /**
   * Compute the directory used to store the specified kit.
   */
  public static File toDir(String kitName)
  {
    return new File(dir, kitName);
  }
      
  /**
   * Compute the filename for the kitName, checksum, version tuple.
   */
  public static File toFile(String kitName, int checksum, Version version)
  {                                                                
    return new File(toDir(kitName), kitName + "-" + 
      Integer.toHexString(checksum) + "-" + version + ".kit");
  }

  /**
   * Compute the filename from the manifest.
   */
  public static File toFile(KitManifest manifest)
  {
    return toFile(manifest.name, manifest.checksum, manifest.version);                                             
  }                   

  /**
   * Return a list of all kits installed.
   */    
  public static String[] kits() 
  {           
    File[] list = dir.listFiles();
    ArrayList acc = new ArrayList(list.length);  
    for (int i=0; i<list.length; ++i)
      if (list[i].isDirectory()) acc.add(list[i].getName());
    return (String[])acc.toArray(new String[acc.size()]);
  }

  /**
   * List all the kit files available for the specified kit name.
   * If no kit versions are installed return an empty array.
   */
  public static KitFile[] list(String kitName)
  {
    File[] files = toDir(kitName).listFiles(); 
    if (files == null || files.length == 0) return new KitFile[0];
    
    ArrayList acc = new ArrayList(files.length);  
    for (int i=0; i<files.length; ++i)
    {
      File f = files[i];
      if (KitFile.isKitFile(f)) acc.add(new KitFile(f));
    }
    
    KitFile[] kits =(KitFile[])acc.toArray(new KitFile[acc.size()]);
    Arrays.sort(kits);
          
    return kits;
  }

  /**
   * Given a kit dependency, return every kit file which meets
   * the dependency.  If no matches return an empty array.
   */
  public static KitFile[] matchAll(Depend depend)
  {                                  
    KitFile[] kits = list(depend.name());
    ArrayList acc = new ArrayList();  
    for (int i=0; i<kits.length; ++i)
      if (depend.match(kits[i].version, kits[i].checksum)) 
        acc.add(kits[i]);
    return (KitFile[])acc.toArray(new KitFile[acc.size()]);
  }

  /**
   * Return the highest version available of the specified kit.
   */
  public static KitFile matchBest(String kitName)
  {                                             
    return matchBest(Depend.parse(kitName + " 0+"));
  }

  /**
   * Given a kit dependency, return the best match available
   * which is the highest matching version.  If no matching
   * kit versions are available return null.
   */
  public static KitFile matchBest(Depend depend)
  {                                  
    KitFile[] kits = list(depend.name());
    KitFile best = null;
    for (int i=0; i<kits.length; ++i)
      if (depend.match(kits[i].version, kits[i].checksum))
        best = KitFile.best(best, kits[i]);
    return best;
  }

}
