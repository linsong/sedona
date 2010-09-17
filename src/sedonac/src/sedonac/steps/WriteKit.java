//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import sedona.kit.*;
import sedona.manifest.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.jasm.*;

/**
 * WriteKit writes the IR data structure from memory into a zip file.
 */
public class WriteKit
  extends CompilerStep
{

  public WriteKit(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    IrKit kit = compiler.ir;               
    KitManifest manifest = compiler.manifest;
    File file = KitDb.toFile(kit.name, manifest.checksum, manifest.version);
    File dir = file.getParentFile();
    
    if (compiler.outDir != null)
    {
      // if -outDir command line option, then treat that as the KitDb root directory
      dir = new File(compiler.outDir, file.getParentFile().getName());
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

      // Java bytecode files
      for (int i=0; i<compiler.java.length; ++i)
        writeJava(zout, compiler.java[i]);

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

  private void writeJava(ZipOutputStream zout, JavaClass cls)
    throws IOException
  {
    zout.putNextEntry(new ZipEntry(cls.path()));
    zout.write(cls.classfile.bytes, 0, cls.classfile.count);
    zout.closeEntry();
  }

  private void writeSource(ZipOutputStream zout)
    throws Exception
  {
    SourceFile[] files = compiler.sourceFiles;
    HashMap names = new HashMap();
    for (int i=0; i<files.length; ++i)
      writeSource(zout, files[i].file.getName(), new FileInputStream(files[i].file), names);
    
    IncludeDef[] includes = compiler.ast.includes;
    for (int i=0; i<includes.length; ++i)
      writeSource(zout, includes[i], names);
  }
  
  /**
   * Write source from existing kit.
   */
  private void writeSource(ZipOutputStream zout, IncludeDef include, HashMap names)
    throws Exception
  {
    ZipFile zip = new ZipFile(include.sourceKit.file.file);
    Iterator sources = include.typeToSource.values().iterator();
    HashSet alreadyWritten = new HashSet();
    while (sources.hasNext())
    {
      ZipEntry source = (ZipEntry)sources.next();
      
      // It is possible for multiple types to be defined in the same source
      // file, so make sure we only write the source once.
      if (alreadyWritten.add(source.getName()))
      {
        // pull off "source/"
        String name = source.getName().substring(source.getName().indexOf('/')+1);
        writeSource(zout, name, zip.getInputStream(source), names);
      }
    }

    zip.close();
  }

  private void writeSource(ZipOutputStream zout, String name, InputStream source, HashMap names)
    throws IOException
  {
    // generate unique name
    for (int i=0; i<100; ++i)
    {
      if (names.get(name) == null) break;
      name = FileUtil.getBase(name) + "_" + i + ".sedona";
    }
    names.put(name, name);

    InputStream in = new BufferedInputStream(source);
    zout.putNextEntry(new ZipEntry("source/" + name));
    FileUtil.pipe(in, zout);
    in.close();
    zout.closeEntry();
  }

}
