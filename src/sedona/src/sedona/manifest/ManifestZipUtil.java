/*
 * Copyright (c) 2012 Tridium, Inc.
 * Licensed under the Academic Free License version 3.0
 */

package sedona.manifest;

import sedona.Buf;
import sedona.KitPart;
import sedona.util.FileUtil;
import sedona.xml.XParser;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @user Matthew Giannini
 * @creation 3/26/12 2:56 PM
 */
public class ManifestZipUtil
{
  public static KitManifest extract(final Buf zipped, final KitPart part)
  {
    return extract(zipped, new KitPart[] { part })[0];
  }
  
  public static KitManifest[] extract(final Buf zipped, final KitPart[] parts)
  {
    Map map = new LinkedHashMap();
    for (int i=0; i<parts.length; ++i)
      map.put(parts[i] + ".xml", null);
    
    zipped.seek(0);
    ZipInputStream zin = new ZipInputStream(zipped.getInputStream());
    ZipEntry entry;
    int found = 0;
    try
    {
      while (((entry = zin.getNextEntry()) != null) && (found != parts.length))
      {
        final String filename = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
        if (map.containsKey(filename))
        {
          ++found;
          Buf unzipped = new Buf();
          FileUtil.pipe(zin, unzipped.getOutputStream());
          unzipped.seek(0);
          map.put(filename, KitManifest.fromXml(
              XParser.make("manifest", unzipped.getInputStream()).parse()));
        }
        zin.closeEntry();
      }
    }
    catch (Exception e)
    {
      // Empty catch blocks are EVIL!!!!
    }
    finally
    {
      try { zin.close(); } catch (Exception e) { }
    }

    // If only looking for one manifest, and no zipped entries found,
    // assume 'zipped' arg is not a zip file, but a plain XML file;
    // open it as regular stream and create KitManifest instance
    if ( (parts.length==1) && (found==0) )
    {
      final String filename = parts[0] + ".xml";
      Buf plainXml = zipped;

      try
      {
        plainXml.seek(0);
        map.put(filename, KitManifest.fromXml(
              XParser.make("manifest", plainXml.getInputStream()).parse()));
      }
      catch (Exception e2)
      {
        System.out.println("  Error parsing file '" + filename + "': " + e2);
      }
    }

    KitManifest[] manifests = new KitManifest[parts.length];
    int i = 0;
    Iterator iter = map.keySet().iterator();
    while (iter.hasNext())
      manifests[i++] = (KitManifest)map.get(iter.next());
    return manifests;
  }
}
