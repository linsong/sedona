//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import sedona.*;
import sedona.manifest.*;
import sedona.offline.*;
import sedona.util.*;

/**
 * Java based Sedona VM and runtime.
 */
public class Jsvm
{  

  public static void main(String[] args)
  {           
    try
    {                                                       
      // if no args or any arg contains -? or -help
      if (needHelp(args)) { usage(); return; }
      
      // -test runs test suite
      String arg = args[0];      
      if (arg.equals("-ver")) { Env.printVersion("Sedona Runtime for Java"); return; }
      if (arg.equals("-test")) { runTests(); return; }
      if (arg.equals("-testloop")) { runTestLoop(); return; }
      
      // otherwise first arg must be sab or sax file 
      // which we use to derive the schema from
      String[] sysMainArgs = new String[args.length-1];
      System.arraycopy(args, 1, sysMainArgs, 0, sysMainArgs.length);
      System.exit(new Jsvm(new File(arg), sysMainArgs).runJsvm());
    }
    catch (Throwable e)
    {
      e.printStackTrace();
      System.exit(-1);
    }
  }                       
  
  static boolean needHelp(String[] args)
  {                  
    if (args.length == 0) return true;
    for (int i=0; i<args.length; ++i)
    {
      String a = args[i];
      if (a.equals("-?") || a.equals("-help")) return true;
    }
    return false;
  }                              
  
////////////////////////////////////////////////////////////////
// Usage
////////////////////////////////////////////////////////////////
  
  public static void usage()
  {               
    println("usage:");
    println("  jsvm <app>  run sab or sax application");
    println("  jsvm -test  run test suite");
    println("  jsvm -ver   print version information");
  }

  public static void println(String s)
  {
    System.out.println(s);
  }
  
////////////////////////////////////////////////////////////////
// jsvm
////////////////////////////////////////////////////////////////
  
  /**
   * Convenience for {@code this(app, new String[0])}
   * @see #Jsvm(File, String[])
   */
  public Jsvm(File app) throws Exception
  {
    this(app, new String[0]);
  }
  
  /**
   * Creates a JSVM that runs the given application. If the {@code app} parameter
   * is a {@code .sax} file, it is first converted to a {@code .sab} file in the
   * same directory as the sax file.  The JSVM always uses the sab file to run
   * the application.
   * 
   * @param app the File containing the application to run. Can be a {@code .sax}
   * or {@code .sab} file.
   * @param appArgs the arguments to pass to the main method being run
   * by the JSVM.  The first argument is always the absolute path to the
   * sab file.
   * 
   * @throws Exception Thrown if any problem occurs while loading the schema from
   * the given app.
   */
  public Jsvm(File app, String[] appArgs) throws Exception
  {
    this.sab  = loadSchema(app);
    this.args = appArgs;
    this.mainClass = "sedona.vm.sys.Sys";
  }
  
  private File loadSchema(final File app) throws Exception
  {
    File sab = app;
    if (app.getName().endsWith(".sax"))
    {
      sab = new File(app.getParentFile(), FileUtil.getBase(app.getName()) + ".sab");
      System.out.println("Converting " + app.getName() + " -> " + sab);
      OfflineApp.decodeApp(app).encodeAppBinary(sab);
    }
    this.schema = OfflineApp.decodeApp(sab).schema;
    return sab;
  }
  
  /**
   * Get the Schema being used by this JSVM.
   * 
   * @return the Schema being used by the JSVM.
   * @see sedona.Schema
   */
  public final Schema getSchema() { return schema; }
  
  /**
   * Create a new instance of the SedonaClassLoader to use for this jsvm.
   * The default implementation returns
   * <p>
   * {@code new SedonaClassLoader(getSchema(), cx)}
   * 
   * @param cx the Context to create the class loader with
   * @return a new SedonaClassLoader instance
   * @throws Exception Thrown if the SedonaClassLoader fails to initialize
   */
  protected SedonaClassLoader newClassLoader(Context cx) throws Exception
  {
    return new SedonaClassLoader(getSchema(), cx);
  }
  
  /**
   * By default, the jsvm attempts to run the main method in class
   * {@code sedona.vm.sys.Sys}.  Use this method to change the class containing
   * the main method.  The class must have a method corresponding to the Sedona
   * signature
   * <p>
   * {@code static int main(Str[] args, int argsLen)}
   */
  public final void setMainClass(final String mainClass)
  {
    this.mainClass = mainClass;
  }
  
  /**
   * Convenience for {@code return runJsvm(new Context());}
   */
  public final int runJsvm() throws Throwable
  {
    return runJsvm(new Context());
  }
  
  /**
   * Runs the JSVM with the given context.
   *
   * @return the return code of the main class run by the jsvm.
   * 
   * @see #newClassLoader(Context)
   * @see #setMainClass(String)
   */
  public final int runJsvm(final Context cx) throws Throwable
  {
    try
    {
      SedonaClassLoader loader = newClassLoader(cx);
      bootstrap(schema, loader);
      Method sysMain = findMethod(loader.loadClass(mainClass), "main");
      return ((Integer)sysMain.invoke(null, sedonaArgs())).intValue();
    }
    catch (InvocationTargetException e)
    {
      throw e.getCause();
    }
  }

  protected Object[] sedonaArgs()
  {
    StrRef[] strs = new StrRef[args.length + 1]; // room for sab and other args
    strs[0] = VmUtil.strConst(sab.getAbsolutePath());
    for (int i=0; i<args.length; ++i) 
      strs[i+1] = VmUtil.strConst(args[i]);
    return new Object[] { strs, new Integer(strs.length) };
  }
  
  final protected File sab;
  protected String[] args;
  protected String mainClass;
  
  private Schema schema;
  
////////////////////////////////////////////////////////////////
// Utility
////////////////////////////////////////////////////////////////
  
  /**
   * Invokes {@code sedona.vm.<kit>.JsvmBootstrap.bootstrap()} on every
   * kit in the schema in kit dependency order.
   */
  static void bootstrap(Schema schema, ClassLoader loader) throws Exception
  {
    HashMap map = new HashMap();
    HashSet todo = new HashSet();
    for (int i=0; i<schema.kits.length; ++i)
    {
      Kit k = schema.kits[i];
      map.put(k.name, ManifestDb.load(new KitPart(k.name, k.checksum)));
      todo.add(k.name);
    }
    for (int i=0; i<schema.kits.length; ++i)
      doBootstrap(schema.kits[i].name, todo, map, loader);
  }
  
  static void doBootstrap(String kit, HashSet todo, HashMap map, ClassLoader loader) throws Exception
  {
    if (!todo.contains(kit)) return;
    
    Depend[] depends = ((KitManifest)map.get(kit)).depends;
    for (int i=0; i<depends.length; ++i)
      if (todo.contains(depends[i].name()))
        doBootstrap(depends[i].name(), todo, map, loader);
    
    findMethod(loader.loadClass("sedona.vm."+kit+".JsvmBootstrap"), "bootstrap")
      .invoke(null, (Object[])null);
    todo.remove(kit);
  }
  
  static Object[] toSedonaArgs(String[] args)
  {                                       
    StrRef[] strs = new StrRef[args.length];
    for (int i=0; i<args.length; ++i)
      strs[i] = VmUtil.strConst(args[i]);
    return new Object[] { strs, new Integer(args.length) };
  }                
  
  static Method findMethod(Class cls, String name)
    throws Exception
  {
    Method[] methods = cls.getMethods();
    for (int i=0; i<methods.length; ++i)
      if (methods[i].getName().equals(name))
        return methods[i];
    throw new IllegalStateException("Cannot find method in " + cls.getName() + "." + name);
  }

////////////////////////////////////////////////////////////////
// Tests
////////////////////////////////////////////////////////////////

  public static void runTests()
    throws Throwable
  {
    // just use fixed schema for now
    KitPart[] kits = new KitPart[] 
    { 
      KitPart.forLocalKit("sys"), 
      KitPart.forLocalKit("inet"), 
      KitPart.forLocalKit("sox"),
      KitPart.forLocalKit("web"),
      KitPart.forLocalKit("control"),
    };              

    // run tests    
    Schema schema = Schema.load(kits);
    SedonaClassLoader loader = new SedonaClassLoader(schema);   
    bootstrap(schema, loader);
    runTests(loader, true);          
    
  }

  public static void runTests(SedonaClassLoader loader, boolean log)
    throws Throwable
  {                     
    // walk all the kits                                     
    ArrayList failures = new ArrayList();
    for (int i=0; i<loader.schema.kits.length; ++i)
    {
      String[] tests = loader.reflector.kitTests(loader.schema.kits[i]);
      for (int j=0; j<tests.length; ++j)
        if (!runTest(loader, tests[j], log)) 
          failures.add(tests[j]);
    }                                 
    
    // print report
    if (log)
    {
      System.out.println();
      for (int i=0; i<failures.size(); ++i)
        System.out.println("FAILED: " + failures.get(i));
      System.out.println();      
      System.out.println("Assert Successes: " + VmUtil.assertSuccess);      
      System.out.println("Assert Failures:  " + VmUtil.assertFailure);      
      System.out.println();      
    }
  }                   
  
  static boolean runTest(SedonaClassLoader loader, String qname, boolean log)
  {                       
    if (log) System.out.print("-- Test " + qname + " ");
    if (log) System.out.flush();
    int startingFailures = VmUtil.assertFailure;
    int startingSuccesses = VmUtil.assertSuccess;
    try
    {                              
      // parse qname
      int dot = qname.indexOf('.');
      String type = qname.substring(0, dot);
      String slot = qname.substring(dot+1);
      
      // get method
      Class cls = loader.reflector.typeClass(type);
      Method m = cls.getMethod(slot, new Class[0]);
      
      // call
      m.invoke(null, new Object[0]);  
      
      int successes = VmUtil.assertSuccess - startingSuccesses;
      int failures  = VmUtil.assertFailure - startingFailures;
      if (log && failures == 0) System.out.println("(" + successes + ")");
      return failures == 0; 
    }
    catch (Throwable e)
    {
      if (e instanceof InvocationTargetException) e = e.getCause();
      System.out.println();
      System.out.println("FAILED: " + qname);
      e.printStackTrace();       
      return false;
    }
  }

  public static void runTestLoop()
    throws Throwable
  {   
    //                                   
    // To run loop tests:
    // sys changes:
    //   - ensure only testing sys kit
    //   - uncomment /* LOOPTEST */ code in sys
    //   - comment out @javaSkip tests to ensure same num of tests       
    //   - comment out anything with sleep such as SysTest.testTime()
    // svm.exe changes:
    //   - comment out printf in sys/native/sys_Test_doMain.c
    //   - recompile svm.exe with call to vmLoopTest in vmRun (after vmInit)
    // jsvm.exe changes:
    //   - run jsvm -testloop 
    //
    KitPart[] kits = new KitPart[]  {  KitPart.forLocalKit("sys") };
    Schema schema = Schema.load(kits);
    SedonaClassLoader loader = new SedonaClassLoader(schema);   
    
    // warm up
    long w1 = System.currentTimeMillis();
    for (int i=0; i<5000; ++i) runTests(loader, false);  
    long w2 = System.currentTimeMillis();
    VmUtil.assertSuccess = VmUtil.assertFailure = 0;        
    System.out.println("Warmup: 5000 loops in " + (w2-w1) + "ms");
    
    int count = 10000;
    long t1 = System.currentTimeMillis();
    for (int i=0; i<count; ++i) runTests(loader, false);          
    long t2 = System.currentTimeMillis();
    System.out.println("JVM: " + count + " loops in " + (t2-t1) + "ms");
    System.out.println();
    System.out.println("Assert Successes: " + VmUtil.assertSuccess);      
    System.out.println("Assert Failures:  " + VmUtil.assertFailure);      
  }
    
}

