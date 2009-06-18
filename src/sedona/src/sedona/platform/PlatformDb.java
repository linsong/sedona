//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Dec 07  Brian Frank  Creation
//   7 June 09 Matthew Giannini  Platform Management Changes
//

package sedona.platform;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import sedona.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * PlatformDb manages the directory of platform archives.
 */
public final class PlatformDb
{
  
//////////////////////////////////////////////////////////////////////////
// Constructors/Singleton
//////////////////////////////////////////////////////////////////////////
 
  private PlatformDb()
  {
    // Make sure database root directory exists
    if (!dbDir.exists()) dbDir.mkdirs();
  }
  
  private static class SingletonHolder 
  {
    private static final PlatformDb INSTANCE = new PlatformDb();
  }
  
  /**
   * Get the platform database instance.
   */
  public static PlatformDb db()
  {
    return SingletonHolder.INSTANCE;
  }
  
//////////////////////////////////////////////////////////////////////////
// Installation
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Installs the given PAR file into the local platform database. If the PAR
   * is already present in the platform database, it is deleted before being
   * re-installed.
   * 
   * @return true if the installation is successful, false otherwise.
   */
  public boolean install(ParFile par)
  {
    PlatformManifest manifest = par.getPlatformManifest();
    if (manifest == null) return false;
    
    try
    {
      File installDir = toParLocation(manifest.id);
      FileUtil.delete(installDir, null);
      FileUtil.mkdir(installDir, null);
      
      Enumeration entries = par.entries();
      while (entries.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)entries.nextElement();
        String name = entry.getName();
        
        // everything is relative
        if (name.startsWith("/")) name = name.substring(1);
        
        // make directories
        if (name.endsWith("/") || entry.isDirectory())
        {
          FileUtil.mkdir(new File(installDir, name), null);
          continue;
        }
        
        // we cannot assume parent entries come before children entries,
        // so always make the directory for a file
        final int idx = name.lastIndexOf('/');
        if (idx > 0)
        {
          // need to make directories
          String dirs = name.substring(0, idx);
          File dir = new File(installDir, dirs);
          FileUtil.mkdir(dir, null);
        }
        
        // extract file
        File file = new File(installDir, name);
        FileOutputStream out = new FileOutputStream(file);
        InputStream in = par.getInputStream(entry);
        for (int c = in.read(); c != -1; c = in.read())
          out.write(c);
        in.close();
        out.close();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
 
//////////////////////////////////////////////////////////////////////////
// List
//////////////////////////////////////////////////////////////////////////
  
  /**
   * List all the platformIds defined in the local database.
   */
  public String[] list()
  {
    ArrayList acc = new ArrayList();
    list(acc, dbDir, new ArrayList());
    String[] result = (String[])acc.toArray(new String[acc.size()]);
    Arrays.sort(result);
    return result;
  }
  
  private void list(ArrayList acc, File dir, ArrayList dirs)
  {
    File[] kids = dir.listFiles();
    for (int i=0; i<kids.length; ++i)
    {
      File kid = kids[i];
      String name = kid.getName();
      if (kid.isDirectory())
      {
        if (name.equals(".par"))
        {
          acc.add(TextUtil.join(dirs.toArray(), "-"));
          continue;
        }
        dirs.add(name);
        list(acc, kid, dirs);
        dirs.remove(dirs.size()-1);
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Load
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Lookup the platform manifest for the specified 
   * platformId, or return null if not found.   
   */
  public PlatformManifest load(final String platformId)
    throws XException
  {                         
    PlatformMatch platform = matchBest(platformId);
    if (platform == null) return null;
    
    XElem xml = parseManifest(platform.manifest);
    
    // This is a valid platform manifest for the given platformId if the 
    // platformId starts with the matched fileId
    if (!xml.get("platformId").startsWith(platform.matchId))
      throw new XException(platformId + " does not start with matched platform manifest id " + platform.matchId, xml);
    
    return PlatformManifest.decodeXml(xml);
  }
  
//////////////////////////////////////////////////////////////////////////
// SVM
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Convenience for {@code getVm(platform.id)}
   * 
   * @see #getVm(String)
   */
  public File getVm(PlatformManifest platform)
  {
    return getVm(platform.id);
  }
  
  /**
   * Checks the database and returns the best matching vm for the given
   * platform id.
   * 
   * @return the Sedona VM for the given platform id, or null if one could
   * not be found.
   */
  public File getVm(final String platformId)
  {
    PlatformMatch platform = matchBest(platformId);
    if (platform  == null) return null;

    File vmDir = new File(platform.manifest.getParentFile(), "svm");
    File[] files = vmDir.listFiles();
    
    // There should only be one vm in the platform's svm directory
    return (files.length > 0) ? files[0] : null;
  }
  
////////////////////////////////////////////////////////////////
// Utility
////////////////////////////////////////////////////////////////
  
  /**
   * @return a File representing the ".par" directory for the given platformId.
   */
  private File toParLocation(String platformId)
  {
    String[] toks = TextUtil.split(platformId, '-');
    File dir = dbDir;
    for (int i=0; i<toks.length; ++i)
      dir = new File(dir, toks[i]);
    return new File(dir, parDir);
  }
  
  private class PlatformMatch
  {
    public PlatformMatch(File manifest, String matchId)
    {
      this.manifest = manifest;
      this.matchId = matchId;
    }
    
    public final File manifest;
    public final String matchId;
  }
  
  /**
   * Try to find the xml manifest by walking up the directory tree looking for
   * the best match. The search is rooted at {@code sedona_home}/platforms/db}
   * <p>
   * For example, if platformId is {@code tridium-jace-win32-1.0.37} the
   * following directories will be searched in order for a platformManifest.xml
   * file.
   * <ol>
   * <li>tridium/jace/win32/1.0.37/.par
   * <li>tridium/jace/win32/.par
   * <li>tridium/jace/.par
   * <li>tridium/.par
   * </ol>
   * 
   * @param platformId
   *          the platform id to find the best match for
   * @return a PlatformMatch object if a manifest is found. Otherwise, null is
   *         returned.
   */
  private PlatformMatch matchBest(final String platformId)
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
      // Build up path to potential manifest file
      file = new File(toParLocation(fileId), manifestFile);
      if (file.exists()) break;
    }
        
    return file.exists() ? new PlatformMatch(file, fileId) : null;
  }
  
  private XElem parseManifest(File manifest)
  {
    try
    {
      return XParser.make(manifest).parse();
    }
    catch (Exception e)
    {
      throw new XException("Cannot parse XML file", new XLocation(manifest.toString()));
    }
  }

////////////////////////////////////////////////////////////////
// Main
////////////////////////////////////////////////////////////////
  
  public static void main(String[] args)
  {
    Facets options = parseOpts(args);
    try
    {
      if (options.getb("list"))
        showList(options);
      else if (options.getb("install"))
        doInstall(options);
      else
        err("No actions to take");
    }
    catch (Exception e)
    {
      e.printStackTrace();
      err(e.getMessage());
    }
  }
  
  private static void showList(Facets options)
  {
    String[] platforms = db().list();
    System.out.println("Platform Database: " + platforms.length + " platforms");
    for (int i=0; i<platforms.length; ++i)
      System.out.println(" " + platforms[i]);
    System.out.println();
  }
  
  private static void doInstall(Facets options) throws Exception
  {
    if (db().install(new ParFile(options.gets("par"))))
      System.out.println("Success!");
    else
      err("Could not install '" + options.gets("par") + "'");
  }
  
  private static Facets parseOpts(String[] args)
  {
    Facets options = new Facets();
    if (args.length == 0) 
      err("No options");
    
    for (int i=0; i<args.length; ++i)
    {
      final String arg = args[i];
      if (arg.equals("-h") || arg.equals("--help"))
        showHelp(0);
      else if (arg.equals("-l") || arg.equals("--list"))
        options.setb("list", true);
      else if (arg.equals("-i"))
      {
        if (i+1 >= args.length || args[i+1].charAt(0) == '-')
          err("Missing argument for -i option");
        options.setb("install", true);
        options.sets("par", args[++i]);
      }
      else
        err("Unrecognized option '" + arg + "'");
    }
    return options;
  }
  
  private static void err(String msg)
  {
    System.err.println("\nError: " + msg);
    showHelp(1);
  }
  
  private static void showHelp(final int exitCode)
  {
    System.out.println();
    System.out.println("usage:");
    System.out.println("  PlatformDb [OPTIONS]");
    System.out.println("options:");
    System.out.println("  -i platform.par     Install the given PAR file into the platform database");
    System.out.println("  -l, --list          List all platforms in the database");
    System.out.println("  -h, --help          Print this usage");
    System.out.println();
    System.exit(exitCode);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final File dbDir = new File(new File(Env.home, "platforms"), "db");

  private static final String sep = File.separator;
  private static final String manifestFile = "platformManifest.xml";
  private static final String parDir = ".par";

}
