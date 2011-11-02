//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 June 09  Matthew Giannini Creation
//
package sedona.platform;

import java.io.*;
import java.util.zip.*;

import sedona.xml.*;

/**
 * ParFile models a Platform ARchive.
 */
public class ParFile extends ZipFile
{
  
//////////////////////////////////////////////////////////////////////////
// Constructors
//////////////////////////////////////////////////////////////////////////

  public ParFile(File file)
    throws ZipException, IOException
  {
    super(file);
  }

  public ParFile(String name)
    throws IOException
  {
    super(name);
  }
  
//////////////////////////////////////////////////////////////////////////
// Manifest
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Get the Platform manifest, or {@code null} if none.
   * 
   * @return the PlatformManifest or {@code null} if none.
   */
  public PlatformManifest getPlatformManifest()
  {
    PlatformManifest m = null;
    try
    {
      ZipEntry entry = this.getEntry("platformManifest.xml");
      if (entry == null) return null;
      m = PlatformManifest.decodeXml(XParser.make(getName(), getInputStream(entry)).parse());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return m;
  }

}
