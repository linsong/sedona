//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Jun 09  Brian Frank  Creation
//

package sedonacert.prov;

import java.io.*;

import sedonacert.*;

/**
 * Bundle for provisioning tests.
 */
public class Prov extends Bundle
{         
  
  public Prov(Runner runner) 
  { 
    super(runner, "prov", new Test[]
    { 
      new ProvInit(),
      new ProvAddKit(),
      new ProvRestore(),
    });
    
    this.initSab    = new File(runner.testDir, "provInit.sab");
    this.initScode  = new File(runner.testDir, "provInit.scode");
    this.addSab     = new File(runner.testDir, "provAdd.sab");
    this.addScode   = new File(runner.testDir, "provAdd.scode"); 
  }
  
  final File initSab;
  final File initScode;
  final File addSab;
  final File addScode;
}
