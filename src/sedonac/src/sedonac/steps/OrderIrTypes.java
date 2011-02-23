//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   28 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;

/**
 * OrderIrTypes orders the IR type list
 */
public class OrderIrTypes
  extends OrderTypes
{

  public OrderIrTypes(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    compiler.flat = new IrFlat(ns, compiler.kits);
    order(compiler.flat.types);
    quitIfErrors();
  }

}
