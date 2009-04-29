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

import sedonac.Compiler;

/**
 * OrderAstTypes orders the AST TypeDef list
 */
public class OrderAstTypes
  extends OrderTypes
{

  public OrderAstTypes(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    order(compiler.ast.types);
  }

}
