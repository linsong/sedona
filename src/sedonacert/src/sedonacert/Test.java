//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert;                        

import sedonac.test.*;

/**
 * Test is base class for a certification test.
 */
public abstract class Test extends Verifies
{          

  /**
   * Construct with simple name of test.
   */
  public Test(String name)  
  { 
    this.name = name; 
  }
 
  /**
   * Runner which provides context for this test's execution.
   */
  public Runner runner;  
  
  /**
   * Parent test bundle.
   */
  public Bundle bundle;            
  
  /**
   * Name of the test.
   */
  public final String name;  
  
  /**
   * Qualified name of the test.
   */
  public String qname;

  /**
   * State of the test: NOTRUN, PASS, FAIL.
   */
  public int status = NOTRUN;
  
  public static final int NOTRUN = 0;
  public static final int PASS   = 1;
  public static final int FAIL  = 2;  
  
  /**
   * If test failed, then this is the offending exception.
   */
  public Throwable failure;      
  
  /**
   * Number of success verifies.
   */
  public int verifies;
  
  /**
   * Run the test, throw an exception on failure or if no
   s* exception thrown we consider the test successful.
   */
  public abstract void run()
    throws Throwable;           
    
  /**
   * Throw test exception
   */  
  public void verify(boolean cond, String msg)
  {           
    if (!cond) throw new TestException(msg);
    verifies++;
  }
        
  
}
