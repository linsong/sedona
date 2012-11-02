//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import sedona.manifest.KitManifest;
import sedona.util.Version;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedona.xml.XParser;
import sedonac.ast.KitDef;
import sedonac.ir.IrFlat;
import sedonac.ir.IrKit;
import sedonac.ir.IrMethod;
import sedonac.namespace.Namespace;
import sedonac.platform.PlatformDef;
import sedonac.scode.SCodeImage;
import sedonac.steps.Assemble;
import sedonac.steps.AssignSlotIds;
import sedonac.steps.BuildManifest;
import sedonac.steps.CheckErrors;
import sedonac.steps.CheckHtmlLinks;
import sedonac.steps.CompileDir;
import sedonac.steps.ConstFolding;
import sedonac.steps.ConvertAppFile;
import sedonac.steps.FieldLayout;
import sedonac.steps.FilterTestClasses;
import sedonac.steps.FindTestCases;
import sedonac.steps.GenNativeTable;
import sedonac.steps.Generate;
import sedonac.steps.Inherit;
import sedonac.steps.InitImageCompile;
import sedonac.steps.InitKitCompile;
import sedonac.steps.InitStagePlatform;
import sedonac.steps.InlineConsts;
import sedonac.steps.InstanceInit;
import sedonac.steps.MountAstIntoNamespace;
import sedonac.steps.Normalize;
import sedonac.steps.NormalizeExpr;
import sedonac.steps.OptimizeIr;
import sedonac.steps.OrderAstTypes;
import sedonac.steps.OrderIrTypes;
import sedonac.steps.OrderStaticInits;
import sedonac.steps.Parse;
import sedonac.steps.ReadKits;
import sedonac.steps.ResolveDepends;
import sedonac.steps.ResolveExpr;
import sedonac.steps.ResolveFacets;
import sedonac.steps.ResolveIR;
import sedonac.steps.ResolveIncludes;
import sedonac.steps.ResolveNatives;
import sedonac.steps.ResolveTypes;
import sedonac.steps.StageNatives;
import sedonac.steps.StagePlatform;
import sedonac.steps.StaticAnalysis;
import sedonac.steps.TableOfContents;
import sedonac.steps.VTableLayout;
import sedonac.steps.WriteDoc;
import sedonac.steps.WriteImage;
import sedonac.steps.WriteKit;
import sedonac.translate.Translation;

/**
 * Main command line entry point for the Sedona compiler.
 */
public class Compiler
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Constructor
   */
  public Compiler()
  {
    log     = new CompilerLog();
    errors  = new ArrayList();
    warnings= new ArrayList();
    ns      = new Namespace();
  }

////////////////////////////////////////////////////////////////
// Pipelines
////////////////////////////////////////////////////////////////

  /**
   * Given an input String figure out which pipeline to run.
   */
  public void compile(File f)
  {
    // check that input file exists
    if (!f.exists())
      throw err("Input file does not exist" , new Location(input));
      
    // if directory, then look for kit.xml
    if (f.isDirectory())
    {
      File dir = f;
      f = new File(dir, "kit.xml");
      if (!f.exists()) f = new File(dir, "dir.xml");
      if (!f.exists() || f.isDirectory())
        throw err("Invalid input directory" , new Location(dir));
    }

    // save to field and attempt to normalize
    try
    {
      this.input = f.getCanonicalFile();
    }
    catch (IOException e)
    {
      this.input = f;
    }

    // if file ends with ".sab" then convert to XML .sax file
    if (f.getName().endsWith(".sab")) { appBinaryToXml(); return; }

    // parse xml
    try
    {
      this.xml = XParser.make(f).parse();
    }
    catch (XException e)
    {
      throw err(e);
    }
    catch (Exception e)
    {
      throw err("Cannot parse XML input file", new Location(f), e);
    }

    // check for compile pipelines
    String rootElem = xml.name();
    if (rootElem.equals("sedonaKit"))       { compileKit(); return; }
    if (rootElem.equals("sedonaCode"))      { compileImage(); return; }
    if (rootElem.equals("sedonaDir"))       { compileDir(); return; }
    if (rootElem.equals("sedonaTranslate")) { translate(); return; }
    if (rootElem.equals("sedonaPlatform"))  { stagePlatform(); return; }
    if (rootElem.equals("sedonaApp"))       { appXmlToBinary(); return; }
    if (rootElem.equals("toc"))             { compileDocs(); return; }

    throw err("Unknown XML input file type <" + rootElem + ">", new Location(xml));
  }

  /**
   * Run the pipeline to compile a directory of Sedona
   * source files into a kit file containing the IR.
   */
  public void compileKit()
  {
    new InitKitCompile(this).run();
    new ResolveDepends(this).run();
    new ResolveIncludes(this).run();
    new OrderIrTypes(this).run();
    new ResolveIR(this).run();
    new Parse(this).run();
    new MountAstIntoNamespace(this).run();
    new ResolveTypes(this).run();
    new OrderAstTypes(this).run();
    new Inherit(this).run();
    new InstanceInit(this).run();
    new Normalize(this).run();
    new ResolveExpr(this).run();
    new ConstFolding(this).run();
    new ResolveFacets(this).run();
    new CheckErrors(this).run();
    new NormalizeExpr(this).run();
    new ResolveNatives(this).run();
    new StaticAnalysis(this).run();
    new Assemble(this).run();
    new FieldLayout(this).run();
    new BuildManifest(this).run();
    new OptimizeIr(this).run();
    new WriteKit(this).run();
    new WriteDoc(this).run(); 
  }

  /**
   * Run the pipeline to compile a set of kits into a scode image.
   */
  public void compileImage()
  {
    new InitImageCompile(this).run();
    new ReadKits(this).run();
    new FilterTestClasses(this).run();
    new OrderIrTypes(this).run();
    new ResolveIR(this).run();
    new Inherit(this).run();
    new AssignSlotIds(this).run();
    new OrderStaticInits(this).run();
    new FieldLayout(this).run();
    new VTableLayout(this).run();
    new InlineConsts(this).run();
    new FindTestCases(this).run();
    new Generate(this).run();
    new WriteImage(this).run();
  }

  /**
   * Run the pipeline to compile a directory of compiler targets.
   */
  public void compileDir()
  {                                                             
    new CompileDir(this).run();
  }

  /**
   * Run the pipeline to stage the VM and native source 
   * code for a specific platform port.
   */
  public void stagePlatform()
  {
    new InitStagePlatform(this).run();
    new ReadKits(this).run();
    new StageNatives(this).run();
    new GenNativeTable(this).run();
    new StagePlatform(this).run();
  }

  /**
   * Run the pipeline to compile a set of kits into Java or C code.
   */
  public void translate()
  {                         
    throw new RuntimeException("translate not supported yet");
  }

  /**
   * Translate an application file from XML format to binary format.
   */
  public void appXmlToBinary()
  {
    new ConvertAppFile(this).run();
  }

  /**
   * Translate an application file from binary format to XML format.
   */
  public void appBinaryToXml()
  {
    new ConvertAppFile(this).run();
  }

  /**
   * Compile the HTML documentation.
   */
  public void compileDocs()
  {
    new TableOfContents(this).run();
    new CheckHtmlLinks(this).run();
  }

////////////////////////////////////////////////////////////////
// Errors
////////////////////////////////////////////////////////////////

  /**
   * Return list of accumulated errors.
   */
  public CompilerException[] errors()
  {
    return (CompilerException[])errors.toArray(new CompilerException[errors.size()]);
  }

  /**
   * If there are any acumulated errors, then throw
   * the first one to end the compiler pipeline.
   */
  public void quitIfErrors()
  {
    if (errors.size() > 0)
      throw (CompilerException)errors.get(0);
  }

  /**
   * Log all the accumulated errors to the log instance.
   * Return number of errors.
   */
  public int logErrors()
  {
    CompilerException[] errors = errors();
    for (int i=0; i<errors.length; ++i)
      log.error(errors[i]);
    return errors.length;
  }

  /**
   * Create and log a CompilerException.
   */
  public CompilerException err(String msg)
  {
    return err(new CompilerException(msg, null));
  }

  /**
   * Create and log a CompilerException.
   */
  public CompilerException err(String msg, Location loc)
  {
    return err(new CompilerException(msg, loc));
  }

  /**
   * Create and log a CompilerException.
   */
  public CompilerException err(String msg, String loc)
  {
    return err(new CompilerException(msg, new Location(loc)));
  }

  /**
   * Create and log a CompilerException.
   */
  public CompilerException err(String msg, Location loc, Throwable e)
  {
    return err(new CompilerException(msg, null, e));
  }

  /**
   * Create and log a CompilerException.
   */
  public CompilerException err(String msg, String loc, Throwable e)
  {
    return err(new CompilerException(msg, new Location(loc), e));
  }

  /**
   * Add an error to the errors list and return it.
   */
  public CompilerException err(CompilerException err)
  {
    errors.add(err);
    return err;
  }

  /**
   * Add an error to the errors list and return it.
   */
  public CompilerException err(XException err)
  {
    return err(new CompilerException(err));
  }
                       
////////////////////////////////////////////////////////////////
// Warnings
////////////////////////////////////////////////////////////////
  
  public void warn(String msg)
  {
    log.warn("[WARNING] " + msg);
    warnings.add(msg);
  }
  
  public void warn(String msg, Location loc)
  {
    if (loc != null) msg = loc + ": " + msg;
    warn(msg);
  }

////////////////////////////////////////////////////////////////
// New Copy
////////////////////////////////////////////////////////////////
  
  /**
   * Create a new fresh compiler instance which 
   * inherits all the environment configuration.
   */
  public Compiler spawn()
  {              
    Compiler c = new Compiler();
    c.log        = this.log;
    c.doc        = this.doc;
    c.dumpLayout = this.dumpLayout;
    c.errors     = this.errors;
    c.warnings   = this.warnings;
    c.outDir     = this.outDir;
    c.kitVersion = this.kitVersion;
    c.optimize   = this.optimize;
    c.www        = this.www;
    return c;
  }
                       
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  // all pipelines
  public CompilerLog log;          // env -v
  public boolean doc;              // env -doc
  public File input;               // env <input>
  public boolean dumpLayout;       // env -layout
  public Version kitVersion;       // env -kitVersion 
  public File outDir;              // env -outDir     
  public boolean optimize = true;  // env -noOptimize
  public boolean www = false;      // env -www
  public boolean nochk = false;    // env -noChecksum
  public boolean sim = false;      // env -stageSim
  public Namespace ns;             // ctor
  public XElem xml;                // compile(String)
  ArrayList errors;                // err()
  ArrayList warnings;              // warn() - ArrayList so spawn retains state

  // compile kit pipeline
  public KitDef ast;               // InitKitCompile
  public SourceFile[] sourceFiles; // InitKitCompile
  public boolean[] testOnly;       // InitKitCompile
  public IrKit ir;                 // Assemble
  public KitManifest manifest;     // BuildManifest

  // compile scode pipeline
  public IrKit[] kits;             // InitImageCompile/ReadKits
  public SCodeImage image;         // InitImageCompile/Generate
  public IrFlat flat;              // OrderIrTypes/ResolveIR
  public IrMethod[] testMethods;   // FindTestCases
  public int dataSize;             // LayoutFields

  // stage natives
  public PlatformDef platform;     // InitStageNatives
  
  // translate to C pipeline
  public Translation translation;  // InitTranslate
}
