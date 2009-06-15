//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Dec 07  Brian Frank  Creation
//

package sedona.platform;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * PlatformDb manages the directory of platform 
 * manifests and installables.
 */
public class PlatformDb
{

//////////////////////////////////////////////////////////////////////////
// List
//////////////////////////////////////////////////////////////////////////
  
  /**
   * List all the platformIds defined in the local database.
   */
  public static String[] list()
  {               
    ArrayList acc = new ArrayList();                     
    list(acc, dir);
    String[] result = (String[])acc.toArray(new String[acc.size()]);  
    Arrays.sort(result);
    return result;
  }                 
  
  private static void list(ArrayList acc, File dir)
  {                                        
    File[] kids = dir.listFiles();
    for (int i=0; i<kids.length; ++i)
    {
      File kid = kids[i];        
      String name = kid.getName();
      if (kid.isDirectory()) 
        list(acc, kid);
      else if (name.endsWith("xml"))
        acc.add(name.substring(0, name.length()-4));
    }
  }

//////////////////////////////////////////////////////////////////////////
// Load
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Lookup the platform manifest for the specified 
   * platformId, or return null if not found.   
   */
  public static Platform load(String platformId)
    throws XException
  {                         
    XElem xml = loadXml(platformId);
    if (xml == null) return null;         
    
    Platform p = new Platform(xml.get("id"));    
    p.load(xml);
    return p;
  }
  
  static XElem loadXml(String platformId)
    throws XException
  {                         
    // split the file up into its token parts
    String[] toks = TextUtil.split(platformId, '-');
    
    // try to find the xml manifest by walking up 
    // the directory tree looking for the best match
    File file = null; 
    String fileId = null;
    for (int i=toks.length; i>0; --i)
    {
      fileId = TextUtil.join(toks, 0, i, "-");
      file = new File(dir, TextUtil.join(toks, 0, i, sep) + sep + 
                        TextUtil.join(toks, 0, i, "-") + ".xml");
      if (file.exists()) break;
    }
        
    // if we still didn't find a match, give up
    if (!file.exists()) return null;
    
    // load the xml    
    XElem xml = null;
    try
    {
      xml = XParser.make(file).parse();
    }
    catch (Exception e)
    {
      throw new XException("Cannot parse XML file", new XLocation(file.toString()));
    }

    // verify matching id attributes
    if (!xml.get("id").equals(fileId))
      throw new XException("The id attribute must match filename: " + fileId, xml);
    
    return xml;
  }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final File dir = new File(Env.home, "platforms");

  private static final String sep = File.separator;
  

}
