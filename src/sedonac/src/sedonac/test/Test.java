//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.lang.reflect.*;
import sedona.*;
import sedona.util.*;

/**
 * Test
 */
public class Test extends Verifies
{

//////////////////////////////////////////////////////////////////////////
// Top Level
//////////////////////////////////////////////////////////////////////////

  public static int run(String testName)
  {
    try
    {
      long t1 = Env.ticks();
      int totalVerified = 0;
      int totalFailed = 0;

      String[] tests =
      {
        "sedonac.test.TokenizerTest",
        "sedonac.test.ParserTest",
        "sedonac.test.DefiniteAssignmentTest",
        "sedonac.test.DeadCodeTest",
        "sedonac.test.ManifestTest",
        "sedonac.test.KitDbTest",
        "sedonac.test.AppTest",        
        "sedonac.test.DocParserTest",
        "sedonac.test.DaspTest",
        "sedonac.test.SoxTest",
        "sedonac.test.SecurityTest",        
        "sedonac.test.PrimitiveDecodeTest",
        "sedonac.test.PstoreTest",
      };

      for (int i=0; i<tests.length; ++i)
      {
        if (testName != null && !TextUtil.getClassName(tests[i]).equals(testName)) continue;
        Class cls = Class.forName(tests[i]);
        Test test = (Test)cls.newInstance();
        test.run();
        totalVerified += test.verified;
        totalFailed   += test.failed;
      }
      
      long t2 = Env.ticks();
      System.out.println();
      if (totalFailed == 0)
        System.out.println("ALL TESTS PASSED " + totalVerified + " total verifies [" + (t2-t1) + "ms]");
      else
        System.out.println("SOME TESTS FAILED " + totalFailed + " total failures [" + (t2-t1) + "ms]");

      return totalFailed;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return 1;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
    throws Exception
  {
    Method[] methods = getClass().getMethods();
    for (int i=0; i<methods.length; ++i)
    {
      Method m = methods[i];     
      if (m.getDeclaringClass() == Test.class) continue;
      if (m.getName().startsWith("test")) run(m);
    }
  }

  public void run(Method m)
    throws Exception
  {
    System.out.print("-- " + getClass().getName() + "." + m.getName());
    System.out.flush();
    int startVerified = verified;
    try
    {
      m.invoke(this, new Object[] {});
      System.out.println(" [" + (verified-startVerified) + " verifies]");
    }
    catch (InvocationTargetException e)
    {
      failed++;
      System.out.println("\nFAILED");
      e.getTargetException().printStackTrace();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Verify
//////////////////////////////////////////////////////////////////////////

  public void verify(boolean cond, String msg)
  {
    if (cond)
      verified++;
    else
      throw new TestException(msg);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public File testDir()
  {
    File dir = new File(Env.home, "test");
    dir.mkdirs();
    verify(dir.exists());
    return dir;
  }
  
  /**
   * @return the name of svm executable for the current OS.
   */
  public String getSvmName() 
  {
    if (isWindows()) return "svm.exe";
    else if (isLinux()) return "svm";
    else throw new IllegalStateException("Unsupported OS: " + System.getProperty("os.name"));
  }
  
  /**
   * Are we running on a Windows OS?
   */
  public boolean isWindows()
  {
    return System.getProperty("os.name").toLowerCase().indexOf("win") > -1;
  }
  
  /**
   * Are we running on a Linux OS?
   */
  public boolean isLinux()
  {
    return System.getProperty("os.name").toLowerCase().indexOf("linux") > -1;
  }

  /**
   * Open a FileWriter instance.
   * Allow for temporary OS conditions that might cause FileWriter cstr to fail.
   */
  public FileWriter openFileWriter(File f)
  {
    FileWriter fw = null;
    final int maxAttempts = 10;            // max # attempts to open before giving up

    // Retry to avoid test failure due to transient OS issue (file in use, etc).
    // Give it maxAttempts chances to succeed before throwing exception. 
    for (int t=0; t<maxAttempts-1; t++)
    {
      try { fw = new FileWriter(f); } catch (IOException e) { }
      if (fw!=null) break;
      try { Thread.sleep(10); } catch (InterruptedException e) { }
    }

    // Try once more (if necessary), this time catch exception if any & fail
    if (fw==null) try
    {
      fw = new FileWriter(f);
    }
    catch (IOException e)
    {
      e.printStackTrace();
      fail("   Failed to open file " + f + " after " + maxAttempts + " attempts");
    }
    
    return fw;
  }


//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  int verified;
  int failed;

}
