//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert;

/**
 * Bundle is the base class for a group of Tests.
 */
public abstract class Bundle
{                               
  
  /**
   * Construct with runner.
   */
  public Bundle(Runner runner, String name, Test[] tests)
  {
    this.runner = runner;       
    this.tests  = tests;
    this.name   = name;
    for (int i=0; i<tests.length; ++i)
    {         
      Test test = tests[i];
      test.runner = runner;
      test.bundle = this;        
      test.qname  = name + "." + test.name;
    }
  }
 
  /**
   * Runner which provides context for this bundle's execution.
   */
  public final Runner runner;  

  /**
   * Children tests for this bundle.
   */
  public final Test[] tests;  

  /**
   * Name of this bundle.
   */
  public final String name;
   
}
