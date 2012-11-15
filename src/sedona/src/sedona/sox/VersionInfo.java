//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Dec 07  Brian Frank  Creation
//

package sedona.sox;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;

/**
 * VersionInfo models all the version information about a sox
 * device obtained by the version and versionMore requests.
 */
public class VersionInfo
{ 
           
////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public VersionInfo(String platformId, int scodeFlags, KitVersion[] kits)
  { 
    this.platformId = platformId;           
    this.scodeFlags = scodeFlags;
    this.kits       = kits;
    this.props      = new Properties();
  }                                               
  
////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  public void dump()
  {
    System.out.println("VersionInfo");
    System.out.println("  platformId: " + platformId);
    System.out.println("  scodeFlags: 0x" + Integer.toHexString(scodeFlags));
    System.out.println("  kits [" + kits.length + "]:");
    for (int i=0; i<kits.length; ++i)
    {
      System.out.println("    " + kits[i]);
    }
    System.out.println("  props [" + props.size() + "]:");
    for (Iterator it = props.keySet().iterator(); it.hasNext(); )
    {
      String key = (String)it.next();
      System.out.println("    " + key + " = " + props.getProperty(key));
    }      
  }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final String platformId;   // platform id
  public final int scodeFlags;      // scode flags (see SCode.scodeXXX)
  public final KitVersion[] kits;   // kit versions (treat as readonly)
  public final Properties props;    // name/value pairs (treat as readonly)
}
