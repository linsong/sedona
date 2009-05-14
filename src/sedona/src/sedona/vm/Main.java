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
import sedona.kit.*;
import sedona.offline.*;

/**
 * Main for the Java based Sedona VM and runtime.
 */
public class Main
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
      File f = new File(arg);
      if (!f.exists()) { println("File not found: " + f); return; }
      Schema schema = OfflineApp.decodeApp(f).schema;
      
      // run the Sedona VM using the JVM!
      run(schema, "sedona.vm.sys.Sys", args);
    }
    catch (Throwable e)
    {
      e.printStackTrace();
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
// Run
////////////////////////////////////////////////////////////////

  public static void run(Schema schema, String mainClass, String[] args)
    throws Throwable
  {                        
    try
    {
      System.out.println("Java Sedona VM");
      
      // initialize the class loader
      SedonaClassLoader loader = new SedonaClassLoader(schema);
      
      Class cls = loader.loadClass(mainClass);
      Method main = findMethod(cls, "main");
      Object result = main.invoke(null, toSedonaArgs(args));
      
      System.out.println();
      System.out.println("Return Code: " + result);
      System.out.println("Test Results");
      System.out.println("  Success: " + VmUtil.assertSuccess);
      System.out.println("  Failure: " + VmUtil.assertFailure);
    }
    catch (InvocationTargetException e)
    {               
      throw e.getCause();
    }
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

