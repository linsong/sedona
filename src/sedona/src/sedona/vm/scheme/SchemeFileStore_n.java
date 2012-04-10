//
// Copyright (c) 2012 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   14 Mar 12  Elizabeth McKenney   Creation
//                     

package sedona.vm.scheme;

import java.lang.reflect.*;
import sedona.vm.*;
import sedona.vm.sys.*;

/**
 * scheme::SchemeFileStore native methods
 *
 */
public class SchemeFileStore_n   
{                  
  ////////////////////////////////////////////////////////////////
  // Natives
  ////////////////////////////////////////////////////////////////
          
  public static int doSchemeSize(StrRef scheme, StrRef name, Context cx) 
  { 
    StrRef realname = expandFilePath(scheme, name);
    return FileStore_n.doSize(realname, cx);
  }


  public static Object doSchemeOpen(StrRef scheme, StrRef name, StrRef mode, Context cx) 
  { 
    if (cx.isSandboxed()) return null;

    StrRef realname = expandFilePath(scheme, name);
    return FileStore_n.doOpen(realname, mode, cx);
  }

  
  ////////////////////////////////////////////////////////////////
  // Utils
  ////////////////////////////////////////////////////////////////
          
  static StrRef expandFilePath(StrRef schemestr, StrRef path)
  {
    String sch   = schemestr.toString();
    String fpath = path.toString();

    // Find scheme in map
    int sp;
    for (sp=0; sp<MAX_NUM_SCHEMES; sp++)
      if (sch.equals( schemes[sp] ))
        break;

    // If not found
    if (sp>=MAX_NUM_SCHEMES) return Empty_String;

    // Get kit name from filename
    int dash = fpath.indexOf('-');
    if (dash<0) return Empty_String;

    String kitname = fpath.substring(0, dash);

    // Construct & return full path to file
    StrRef fullpath = StrRef.make(schemePaths[sp] + kitname + path);
    return fullpath;
  }



  ////////////////////////////////////////////////////////////////
  // Scheme defns
  ////////////////////////////////////////////////////////////////
  final static int MAX_NUM_SCHEMES = 2;

  final static String[] schemes = { "m", "k", };
  final static String[] schemePaths = { "manifests/", "kits/", };

  final static byte[] nullterm = { 0 };
  final static StrRef Empty_String = new StrRef(nullterm);
}

