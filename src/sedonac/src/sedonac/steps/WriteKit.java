//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import sedona.KitPart;
import sedona.kit.KitDb;
import sedona.manifest.KitManifest;
import sedona.manifest.ManifestDb;
import sedona.util.FileUtil;
import sedona.xml.XWriter;
import sedonac.Compiler;
import sedonac.CompilerStep;
import sedonac.Location;
import sedonac.SourceFile;
import sedonac.ir.IrKit;
import sedonac.ir.IrType;
import sedonac.ir.IrWriter;


/**
 * WriteKit writes the IR data structure from memory into a zip file.
 */
public class WriteKit
  extends CompilerStep
{
//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public WriteKit(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// CompilerStep
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    writeKit();
    writeManifest();
  }
  
//////////////////////////////////////////////////////////////////////////
// Write Kit
//////////////////////////////////////////////////////////////////////////
 
  private void writeKit()
  {
    IrKit kit = compiler.ir;               
    KitManifest manifest = compiler.manifest;
    File file = KitDb.toFile(kit.name, manifest.checksum, manifest.version);
    File dir = file.getParentFile();
    
    if (compiler.outDir != null)
    {
      // write kit in <outDir>/kits/<kitname>/
      dir = new File(new File(compiler.outDir, "kits"), file.getParentFile().getName());
      file = new File(dir, file.getName());
    }
    
    log.info("  WriteKit [" + file + "]");

    try
    {
      dir.mkdirs();

      ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

      // manifest
      writeManifest(zout, kit);

      // IR files
      for (int i=0; i<kit.types.length; ++i)
        writeType(zout, kit.types[i]);

      // write source
      if (compiler.ast.includeSource)
        writeSource(zout);

      zout.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot write kit file", new Location(file));
    }
  }

  private void writeManifest(ZipOutputStream zout, IrKit kit)
    throws IOException
  {
    zout.putNextEntry(new ZipEntry("manifest.xml"));
    XWriter out = new XWriter(zout);
    compiler.manifest.encodeXml(out);
    out.flush();
    zout.closeEntry(); 
  }

  private void writeType(ZipOutputStream zout, IrType t)
    throws IOException
  {
    zout.putNextEntry(new ZipEntry(t.name + ".ir"));
    IrWriter out = new IrWriter(zout);
    out.writeType(t);
    out.flush();
    zout.closeEntry();
  }

  private void writeSource(ZipOutputStream zout)
    throws Exception
  {
    // write kit.xml (as SourceFile so we can re-use writeSource(zout, SourceFile)
    SourceFile kitXml = new SourceFile(); 
    kitXml.file = compiler.input;
    writeSource(zout, kitXml);
    
    // write source
    SourceFile[] files = compiler.sourceFiles;
    for (int i=0; i<files.length; ++i)
      writeSource(zout, files[i]);
  }
  
  private void writeSource(ZipOutputStream zout, SourceFile source)
    throws IOException
  {
    // get relative path to source file from kit file directory
    // InitKitCompile step gaurantees src is subdirectory of root
    final String rootPath = compiler.input.getParentFile().getCanonicalPath();
    final String srcPath  = source.file.getCanonicalPath();
    // do not include leading '/'
    final String relPath  = srcPath.substring(rootPath.length()+1);
    
    InputStream in = new BufferedInputStream(new FileInputStream(source.file));
    zout.putNextEntry(new ZipEntry("source/" + relPath));
    FileUtil.pipe(in, zout);
    in.close();
    zout.closeEntry();
  }
  
//////////////////////////////////////////////////////////////////////////
// Write Manifest
//////////////////////////////////////////////////////////////////////////
  
  private void writeManifest()
  {
    // As part of kit compilation, always write the kit manifest.
    // By default, we write it to the ManifestDb location, but if
    // an "-outDir" command-line option was used, we write it to
    // <outdir>/manifests/<kit>/<manifest>
    
    KitManifest manifest = compiler.manifest;
    KitPart part = new KitPart(manifest.name, manifest.checksum);
    File file = ManifestDb.toFile(part);
    
    if (compiler.outDir != null)
    {
      file = new File(new File(new File(compiler.outDir, "manifests"), part.name), file.getName());
      File dir = file.getParentFile();
    }
    
    log.info("  WriteManifest [" + file + "]");
    file.getParentFile().mkdirs();
    try
    {
      XWriter xml = new XWriter(file);
      manifest.encodeXml(xml);
      xml.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot write kit manifest", new Location(file));
    }
  }

}
