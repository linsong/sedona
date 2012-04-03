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
import sedona.util.sedonadev.Download;
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
   * Convenience to install a platform into the database when only the
   * platform manifest is available. Any platform matching the one specified
   * by the given platform manifest will be deleted prior to installation.
   *
   * @param platformManifest the platform manifest to install into the database.
   *
   * @throws PlatformDbException Thrown if the installation fails for any reason.
   */
  public void install(final PlatformManifest platformManifest)
  {
    try
    {
      PlatformManifest.validate(platformManifest);
      File installDir = initInstall(platformManifest);
      File manifestFile = new File(installDir, "platformManifest.xml");
      XWriter xout = new XWriter(manifestFile);
      platformManifest.encodeXml(xout);
      xout.flush();
      xout.close();
    }
    catch (Exception e)
    {
      throw new PlatformDbException(e);
    }
  }

  /**
   * Installs the given PAR file into the local platform database. If the PAR
   * is already present in the platform database, it is deleted before being
   * re-installed.
   *
   * @throws PlatformDbException Thrown if the installation fails for any reason.
   */
  public void install(ParFile par)
  {
    PlatformManifest manifest = par.getPlatformManifest();
    try
    {
      PlatformManifest.validate(manifest);
      File installDir = initInstall(manifest);
      Enumeration entries = par.entries();
      while (entries.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)entries.nextElement();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FileUtil.pipe(par.getInputStream(entry), bytes);
        writeEntry(installDir, entry, bytes);
      }
    }
    catch (Exception e) { throw new PlatformDbException(e); }
  }

  /**
   * Install a PAR file from a zip input stream. No files get installed unless
   * the input stream contains a valid platform manifest.  If the PAR already
   * exists in the database, then it is deleted and re-installed.
   * <p>
   * The {@code zin} will always be closed when this method completes.
   *
   * @param zin a ZipInputStream corresponding to a PAR file.
   * @throws PlatformDbException Thrown if the installation fails for any reason.
   */
  public void install(ZipInputStream zin)
  {
    LinkedHashMap todo = new LinkedHashMap();
    try
    {
      // Cache the entire zip file in memory - whoa baby!
      PlatformManifest manifest = null;
      ZipEntry entry = null;
      try
      {
        while ((entry = zin.getNextEntry()) != null)
        {
          final String name = entry.getName();
          if (name.startsWith("/"+manifestName) || name.startsWith(manifestName))
          {
            manifest = PlatformManifest.decodeXml(
              XParser.make(name, zin).parse(false));
            continue;
          }

          ByteArrayOutputStream content = new ByteArrayOutputStream();
          FileUtil.pipe(zin, content);
          todo.put(entry, content);
          zin.closeEntry();
        }
      }
      finally
      {
        zin.close();
      }

      PlatformManifest.validate(manifest);
      File installDir = initInstall(manifest);

      // write manifest
      XWriter out = new XWriter(new File(installDir, manifestName));
      manifest.encodeXml(out);
      out.close();

      // write others
      Iterator iter = todo.keySet().iterator();
      while (iter.hasNext())
      {
        entry = (ZipEntry)iter.next();
        writeEntry(installDir, entry, (ByteArrayOutputStream)todo.get(entry));
      }
    }
    catch (PlatformDbException e) { throw e; }
    catch (Exception e) { throw new PlatformDbException(e); }
    finally { todo = null; }
  }

  /**
   * Cleans and makes the target directory for the PAR file and returns the
   * installation directory.
   */
  private File initInstall(PlatformManifest manifest)
    throws IOException
  {
    File installDir = toParLocation(manifest.id);
    FileUtil.delete(installDir, null);
    FileUtil.mkdir(installDir, null);
    return installDir;
  }

  /**
   * Write the zip entry to disk rooted as {@code baseDir}
   */
  private void writeEntry(File baseDir, ZipEntry entry, ByteArrayOutputStream bytes)
    throws IOException
  {
    String name = entry.getName();

    // everything is relative
    if (name.startsWith("/")) name = name.substring(1);

    // make directories (could be empty and we will preserve these)
    if (name.endsWith("/") || entry.isDirectory())
    {
      FileUtil.mkdir(new File(baseDir, name), null);
      return;
    }

    // we cannot assume parent entries come before children entries,
    // so always make the directory for a file.
    final int idx = name.lastIndexOf('/');
    if (idx > 0)
    {
      String dirs = name.substring(0, idx);
      FileUtil.mkdir(new File(baseDir, dirs), null);
    }

    // write file
    FileOutputStream out = new FileOutputStream(new File(baseDir, name));
    bytes.writeTo(out);
    out.close();
  }

//////////////////////////////////////////////////////////////////////////
// Extraction
//////////////////////////////////////////////////////////////////////////

  /**
   * Extract all PAR file contents for the given platform id to the given
   * destination directory.  The destination directory will be created, if
   * necessary.
   *
   * @throws PlatformDbException Thrown if the extraction fails for any reason.
   */
  public void extract(final String platformId, File destDir)
  {
    File dbPar = toParLocation(platformId);
    if (!dbPar.exists())
      throw new PlatformDbException("'" + platformId + "' is not in the database");

    try { FileUtil.copyDir(dbPar, destDir, null); }
    catch (Exception e) { throw new PlatformDbException(e); }
  }

  /**
   * Extract all PAR file contents for the given platform id to the given
   * file. The file will be written as a zipped par file. All directories
   * will be created if they don't exist.
   *
   * @param platformId the platform id of the par file to extract
   * @param parFile the file to write the zipped par file to.
   *
   * @throws PlatformDbException Thrown if the extraction fails for any reason.
   */
  public void extractPar(final String platformId, File parFile)
  {
    File dbPar = toParLocation(platformId);
    if (!dbPar.exists())
      throw new PlatformDbException("'" + platformId + "' is not in the database");

    try
    {
      FileUtil.mkdir(parFile.getParentFile(), null);
      ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(parFile));
      try
      {
        zip(zip, dbPar, "");
      }
      finally
      {
        zip.close();
      }
    }
    catch (Exception e)
    {
      throw new PlatformDbException(e);
    }
  }
  
  private void zip(ZipOutputStream zip, File dir, String entryPath) throws Exception
  {
    File[] files = dir.listFiles();
    for (int i=0; i<files.length; ++i)
    {
      File f = files[i]; 
      if (f.isDirectory())
        zip(zip, f, entryPath.length() == 0 ? f.getName() + "/" : entryPath + f.getName() + "/");
      else
      {
        ZipEntry entry = new ZipEntry(entryPath + f.getName());
        zip.putNextEntry(entry);
        FileInputStream in = new FileInputStream(f);
        FileUtil.pipe(in, zip);
        in.close();
        zip.closeEntry();
      }
    }
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
   * Find the platform manifest that best matches the given platform id.
   *
   * @return the PlatformManifest for the best match, or null if no platforms
   * matched.
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

  /**
   * @return the PlatformManifest for the platform that exactly matches
   * the given platform id, or null if no match was found.
   */
  public PlatformManifest loadExact(final String platformId)
    throws XException
  {
    PlatformManifest manifest = load(platformId);
    return ((manifest != null) && platformId.equals(manifest.id)) ? manifest : null;
  }

//////////////////////////////////////////////////////////////////////////
// SVM
//////////////////////////////////////////////////////////////////////////

  /**
   * Represents an SVM stored in the database.
   * @see PlatformDb#getVm(String)
   */
  public static final class DbVm
  {
    DbVm(String platformId, File svm)
    {
      this.platformId = platformId;
      this.svm = svm;
    }

    /** @return the platform id corresponding to this SVM */
    public String id() { return platformId; }

    /** @return the name of the SVM file */
    public String name() { return svm.getName(); }

    /**
     * Copy the SVM from the database to the given destination directory.
     * The destination directory will be created if it doesn't exist.
     *
     * @param destDir the directory to copy the SVM to.
     * @return the File that was created in the {@code destDir} for the SVM.
     * @throws IOException Thrown if the copy fails.
     */
    public File copyTo(File destDir) throws IOException
    {
      File outSVM = new File(destDir, name());
      FileUtil.copyFile(svm, outSVM);
      return outSVM;
    }

    private final String platformId;
    private final File svm;
  }

  /**
   * Convenience for {@code getVm(platform.id)}
   *
   * @see #getVm(String)
   */
  public DbVm getVm(PlatformManifest platform)
  {
    return getVm(platform.id);
  }

  /**
   * Checks the database and returns the best matching vm for the given platform
   * id.
   *
   * @return a DbVm object representing the SVM that best matched the given
   *         platform id, or {@code null} if one could not be found.
   */
  public DbVm getVm(final String platformId)
  {
    PlatformMatch platform = matchBest(platformId);
    if (platform  == null) return null;

    File vmDir = new File(platform.manifest.getParentFile(), "svm");
    if (!vmDir.exists())
      return null;
    File[] files = vmDir.listFiles();

    // There should only be one vm in the platform's svm directory, AND 
    // it should not have any of these extensions: { .xml, .scode, .sax, .sab }.
    // So, assume the first file we find w/diff extension is the SVM executable.
    File svmFile = null;
    for (int d=0; d<files.length; d++)
    {
      String n = files[d].getName();
      if ( n.endsWith(".xml") || n.endsWith(".scode") || 
           n.endsWith(".sax") || n.endsWith(".sab") )
        continue;
      svmFile = files[d];
      break;
    }

    return (svmFile == null) ? null : new DbVm(platform.matchId, svmFile);
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
    return new File(dir, parDirName);
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
   * the best match. The search is rooted at {@code sedona_home/platforms/db}
   * <p>
   * If an exact match is not found, attempt to download and install a PAR file
   * with the given {@code platformId} from one of the sedonadev.org websites.
   * If we find an exact match from a website, return the exact match.
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
      file = new File(toParLocation(fileId), manifestName);
      if (file.exists()) break;
    }

    // TODO: if not exact match, try to download...
    if (!platformId.equals(fileId))
    {
      try
      {
        install(Download.fetchPar(platformId));
        file = new File(toParLocation(platformId), manifestName);
        fileId = platformId;
      }
      catch (Exception e) { }
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
        db().install(new ParFile(options.gets("par")));
      else if (options.getb("extract"))
        db().extract(options.gets("id"), new File(options.gets("out", options.gets("id"))));
      else
        err("No actions to take");
    }
    catch (Exception e) { e.printStackTrace(); err(e.getMessage()); }
  }

  private static void showList(Facets options)
  {
    String[] platforms = db().list();
    for (int i=0; i<platforms.length; ++i)
      System.out.println(platforms[i]);
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
      else if (arg.equals("-x") || arg.equals("--extract"))
      {
        if (i+1 >= args.length || args[i+1].charAt(0) == '-')
          err("Missing argument for -x option");
        options.setb("extract", true);
        options.sets("id", args[++i]);
      }
      else if (arg.equals("-o") || arg.equals("--out"))
      {
        if (i+1 >= args.length || args[i+1].charAt(0) == '-')
          err("Missing argument for -o option");
        options.sets("out", args[++i]);
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
    System.out.println("  -l, --list          List all platforms in the database");
    System.out.println("  -i platform.par     Install the given PAR file into the platform database");
    System.out.println("  -x, --extract <ID>  Extract all files for the platform with the given ID");
    System.out.println("                      to ./<ID>/. Use -o to change the output location.");
    System.out.println("  -o, --out LOC       Change the default output location to LOC");
    System.out.println("  -h, --help          Print this usage");
    System.out.println();
    System.exit(exitCode);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final File dbDir = new File(new File(Env.home, "platforms"), "db");

  private static final String manifestName = "platformManifest.xml";
  private static final String parDirName = ".par";

}
