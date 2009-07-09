//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.namespace.*;

/**
 * FindTestCases
 */
public class FindTestCases
  extends CompilerStep
{

  public FindTestCases(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    // skip if tests turned off
    if (!compiler.image.test) return;
    log.debug("  FindTestCases");
    
    // find all the tests
    compiler.testMethods = findTestMethods(flat.types, false);
    
    // check them
    for (int i=0; i<compiler.testMethods.length; ++i)
      checkTestCase(compiler.testMethods[i]);
  }

  private void checkTestCase(IrMethod m)
  {                         
    String qname = m.qname;
    Location loc = new Location(qname);
    boolean hasParams = m.params.length > 0;

    if (!m.isPublic())   err("Test method '" + qname + "' must be public", loc);
    if (!m.isStatic())   err("Test method '" + qname + "' must be static", loc);
    if (m.isAbstract())  err("Test method '" + qname + "' cannot be abstract", loc);
    if (m.isNative())    err("Test method '" + qname + "' cannot be native", loc);
    if (!m.ret.isVoid()) err("Test method '" + qname + "' return void", loc);
    if (hasParams)       err("Test method '" + qname + "' cannot have parameters", loc);
  }                  
  
  public static IrMethod[] findTestMethods(IrType[] types, boolean isJava)
  {
    ArrayList acc = new ArrayList(100);
    for (int i=0; i<types.length; ++i)
    {
      IrType t = types[i];
      if (!TypeUtil.isaTest(t)) continue;
      
      // walk all the methods
      for (int j=0; j<t.declared.length; ++j)
      {               
        IrSlot slot = (IrSlot)t.declared[j];
        if (!(slot instanceof IrMethod)) continue;
        IrMethod m = (IrMethod)slot;
        if (!m.name.startsWith("test")) continue; 
        if (isJava && m.facets.getb("javaSkip", false)) continue; 
        acc.add(m);
      }
    }
    return (IrMethod[])acc.toArray(new IrMethod[acc.size()]);
  }
  
}
