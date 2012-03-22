//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 Nov 07  Brian Frank  Creation
//

package sedona.kit;

import java.io.File;

import sedona.util.TextUtil;
import sedona.util.Version;

/**
 * KitFile models a file containing a specific kit version.
 */
public class KitFile
  implements Comparable
{                   
  
  /**
   * Is the specified file a kit file?
   */
  public static boolean isKitFile(File f)
  {
    return !f.isDirectory() && f.getName().endsWith(".kit");
  }           
  
  /**
   * Construct for specified file.
   */
  public KitFile(File file)
  {            
    String s = file.getName(); 
    
    if (!s.endsWith(".kit"))
      throw new IllegalArgumentException("Not a .kit file: " + file);
    s = s.substring(0, s.length()-4);  
    
    String[] toks = TextUtil.split(s, '-');
    if (toks.length != 3)
      throw new IllegalArgumentException("Invalid kit filename: " + file);
    
    this.file     = file;
    this.name     = toks[0];
    this.checksum = (int)java.lang.Long.parseLong(toks[1], 16);
    this.version  = new Version(toks[2]);  
  }             
  
  /**
   * Does the kit file exist on the local disk.
   */
  public boolean exists()
  {
    return file.exists();
  }          
  
  /**
   * Compare based on string file name.
   */
  public int compareTo(Object obj)
  {
    return toString().compareTo(obj.toString());
  }            
  
  /**
   * Equals based on string file name.
   */
  public boolean equals(Object obj)
  {
    if (obj instanceof KitFile)
      return ((KitFile)obj).toString().equals(toString());
    return false;
  }
  
  public int hashCode()
  {
    return toString().hashCode();
  }

  /**
   * Return the file name.
   */
  public String toString()
  {
    return file.getName();
  }        
  
  /**
   * Return the best of two kits.  Highest version is best, or
   * if both are the same version, then use file timestamp.
   */
  public static KitFile best(KitFile a, KitFile b)
  {
    // if either is null, the other is better
    if (a == null) return b;
    if (b == null) return a;
            
    // if one has greater version than it is better
    int cmp = a.version.compareTo(b.version);
    if (cmp > 0) return a;
    if (cmp < 0) return b;
                    
    // if both the same version then use file modified time
    return a.file.lastModified() > b.file.lastModified() ? a : b;
  }
  
  public final File file;
  public final String name;
  public final int checksum;
  public final Version version;
}
